package com.itau.EL9.custom.liberty.components;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Properties;
 
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
 
 
 
import org.json.JSONArray;
import org.json.JSONObject;
 
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
public class ItauTAI implements TrustAssociationInterceptor {
 
    static String urlSts;
    static String ambiente;
    static String secretHmac;
    static String taiUser;
    static int stsTimeout;
    static Logger log = LoggerFactory.getLogger(ItauTAI.class.getName());
 
    public ItauTAI() {
        super();
    }
 
    /*
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#
     * isTargetInterceptor (javax.servlet.http.HttpServletRequest)
     *
     * Metodo que determina quando eu vou antenticar e quando vou deixar passar
     * direto
     */
    public boolean isTargetInterceptor(HttpServletRequest req) throws WebTrustAssociationException {
        // se a rota chamada eh a rota de API
        return true;
    }
 
    /*
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#
     * negotiateValidateandEstablishTrust
     *
     * Esse metodo eh chamado somente se o metodo isTargetInterceptor retornar
     * true. Ele faz a autenticacao em si e libera o acesso a recurso
     * manipulando o indicador de retorno Caso contrario, ele nega o acesso
     */
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse resp)
            throws WebTrustAssociationFailedException {
        try {// Se a rota chamada e outra que nao seja decision service, proibir
 
            if (req.getRequestURI().endsWith("DecisionService") || req.getRequestURI().endsWith("DecisionService/")) {
                return TAIResult.create(HttpServletResponse.SC_OK, taiUser);
            }
 
            // Se a rota eh pra pegar o swagger da API, permitir
            if (req.getRequestURI().contains("DecisionService/rest") && req.getRequestURI().contains("/OPENAPI")) {
                return TAIResult.create(HttpServletResponse.SC_OK, taiUser);
            }
 
            // Se a rota eh pra pegar o contrato da API, permitir
            if (req.getRequestURI().contains("DecisionService/rest") && req.getRequestURI().contains("/json")) {
                return TAIResult.create(HttpServletResponse.SC_OK, taiUser);
            }
 
            // Permitir arquivos estaticos css
            if (req.getRequestURI().contains("DecisionService/css")) {
                return TAIResult.create(HttpServletResponse.SC_OK, taiUser);
            }
 
            // Permitir arquivos estaticos images
            if (req.getRequestURI().contains("DecisionService/images")) {
                return TAIResult.create(HttpServletResponse.SC_OK, taiUser);
            }
 
            String accessToken = req.getHeader("Authorization");
 
            // apos as validacoes basicas iniciais, processar o token
            boolean tokenValido = ValidarToken(accessToken);
 
            if (tokenValido)
                return TAIResult.create(HttpServletResponse.SC_OK, taiUser);
            else {
                if (accessToken != null)
                    if (accessToken.length() > 20)
                        log.info("#INFO - Token rejeitado: " + accessToken.substring(0, 20));
 
                return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Exception e) {
            log.error("#ERROR - ERRO ao validar o token: " + e.getMessage());
            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        }
    }
 
    public boolean ValidarToken(String accessToken) throws WebTrustAssociationFailedException {
        // apos as validacoes basicas iniciais, processar o token
        try {
            // Obrigar a existencia de uma autorizacao e que seja Bearer Token
            if (accessToken == null)
                throw new WebTrustAssociationFailedException("Nenhuma autorizacao encontrada");
 
            // if (!accessToken.contains("Bearer ")){
            // throw new WebTrustAssociationFailedException("Autenticacao
            // precisa ser do tipo Bearer, mas foi recebido " + accessToken);
            // }
 
            String token = accessToken.replace("Bearer ", ""); // Retirando a
                                                                // palavra
                                                                // bearer
            SignedJWT jwt = SignedJWT.parse(token);
 
            // Ver se token esta expirado
            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            Date now = new Date();
            if (now.after(exp)) {
                log.info(String.format("#INFO - Token Expirado - Timestamp Token %s Timestamp servidor %s ", exp, now));
                return false;
            }
 
            JWSAlgorithm tokenAlg = jwt.getHeader().getAlgorithm();
            // se o algoritmo do token e hmac
            if (tokenAlg == JWSAlgorithm.HS256 || tokenAlg == JWSAlgorithm.HS384 || tokenAlg == JWSAlgorithm.HS512) {
 
                JWSVerifier verifier = new MACVerifier(secretHmac);
                boolean result = jwt.verify(verifier);
 
                if (!result)
                    log.info("#INFO - Token Invalido - por algoritmo HMAC");
 
                return result;
            }
            // se o algoritmo do token e rsa
            else if (tokenAlg == JWSAlgorithm.RS256 || tokenAlg == JWSAlgorithm.RS384 || tokenAlg == JWSAlgorithm.RS512
                    || tokenAlg == JWSAlgorithm.PS256 || tokenAlg == JWSAlgorithm.PS384
                    || tokenAlg == JWSAlgorithm.PS512) {
                String clientId = jwt.getJWTClaimsSet().getSubject();
                String keyId = jwt.getHeader().getKeyID();
 
                String chave = RecuperarChaveToken(token, clientId, keyId);
 
                JSONObject json = new JSONObject(chave);
                JSONArray keys = json.getJSONArray("keys");
                String jwk = keys.getJSONObject(0).toString();
                RSAKey rsaJWK = RSAKey.parse(jwk);
                RSAPublicKey publicKey = (RSAPublicKey) rsaJWK.toRSAPublicKey();
 
                JWSVerifier verifier = new RSASSAVerifier(publicKey);
                boolean result = jwt.verify(verifier);
 
                if (!result) {
                    // remover a chave do cache pra evitar loop infinito de
                    // validacao
                    log.info(String.format("#INFO - Validacao RSA para o keyId %s falhou, removendo do cache", keyId));
                    ItauTAICacheManager.removeKey(keyId);
                }
 
                return result;
 
            } else
                throw new WebTrustAssociationFailedException("Algoritmo do token nao suportado: " + tokenAlg.getName());
 
            // Object scope = jwt.getJWTClaimsSet().getClaim("scope");
 
            // if (scope != null)
            // System.out.println("Escopo do token validado: " +
            // scope.toString());
 
        } catch (Exception e) {
            log.error("#ERROR - Erro no validateToken: " + e.getMessage());
            throw new WebTrustAssociationFailedException("Falha na validacao do token: " + e.getMessage());
        }
    }
 
    private String RecuperarChaveToken(String accesstoken, String client_id, String keyid)
            throws IOException, WebTrustAssociationFailedException {
 
        String chave = ItauTAICacheManager.getPrivateKeyFromCache(keyid);
 
        if (chave == null) {
            log.info("#INFO - Requisitando chave ao STS");
            long start = System.currentTimeMillis();
 
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            String api = String.format("https://%s/seguranca/v1/rsa/%s/%s", urlSts, client_id, keyid);
 
            URL url = new URL(api);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
 
            String encodedToken = URLDecoder.decode(accesstoken, "iso-8859-1");
 
            conn.setRequestProperty("Authorization", "Bearer " + encodedToken);
            conn.setRequestProperty("Content-Type", "application/json; charset=iso-8859-1");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(stsTimeout); // em milisegundos
 
            conn.setSSLSocketFactory(sslsocketfactory);
 
            chave = getResult(conn);
            long finish = System.currentTimeMillis();
 
            log.info(String.format("#INFO - Tempo de conexao STS: %d milisegundos", (finish - start)));
 
            ItauTAICacheManager.addPrivateKeyOnCache(keyid, chave);
        } // se ja pegou do cache, simbora
 
        return chave;
    }
 
    private String getResult(HttpsURLConnection conn) throws IOException, WebTrustAssociationFailedException {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
 
            inputStream = conn.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
 
            String s;
            StringBuilder json = new StringBuilder();
            while ((s = bufferedReader.readLine()) != null) {
                json.append(s);
            }
 
            return URLDecoder.decode(json.toString(), "iso-8859-1");
 
        } catch (java.net.SocketTimeoutException e) {
            log.error("#ERROR - Erro na conexao com o STS: " + e.getMessage());
            throw new WebTrustAssociationFailedException("Timeout na conexao com o STS");
        } finally {
 
            if (inputStream != null)
                inputStream.close();
            if (inputStreamReader != null)
                inputStreamReader.close();
            if (bufferedReader != null)
                bufferedReader.close();
        }
    }
 
    /*
     * @see
     * com.ibm.wsspi.security.tai.TrustAssociationInterceptor#initialize(java.
     * util.Properties)
     *
     * Esse metodo eh chamado apenas na inicializacao do componente. Recebe
     * argumentos que estao no server.xml se precisar. Coloquei pra receber a
     * URL do sts pra poder variar por ambiente e o ambiente de execucao
     */
    public int initialize(Properties argumentos) throws WebTrustAssociationFailedException {
 
        if (!argumentos.containsKey("url-sts"))
            throw new WebTrustAssociationFailedException("argumento 'url-sts' nao encontrado");
 
        if (!argumentos.containsKey("ambiente"))
            throw new WebTrustAssociationFailedException("argumento 'ambiente' nao encontrado");
 
        if (!argumentos.containsKey("secret-hmac"))
            throw new WebTrustAssociationFailedException("argumento 'secret-hmac' nao encontrado");
 
        if (!argumentos.containsKey("tai-user"))
            throw new WebTrustAssociationFailedException("argumento 'tai-user' nao encontrado");
 
        if (!argumentos.containsKey("sts-timeout"))
            throw new WebTrustAssociationFailedException("Argumento 'sts-timeout' nao encontrado");
 
        urlSts = argumentos.getProperty("url-sts");
        ambiente = argumentos.getProperty("ambiente");
        secretHmac = argumentos.getProperty("secret-hmac");
        taiUser = argumentos.getProperty("tai-user");
        stsTimeout = Integer.parseInt(argumentos.getProperty("sts-timeout"));
 
        log.info(String.format("#INFO - Autenticador versao %s inicializado. Ambiente %s - URL do STS %s", getVersion(),
                ambiente, urlSts));
 
        ItauTAICacheManager.initializeCache();
 
        return 0;
    }
 
    /*
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#getVersion()
     * Retorna a versao do componente
     */
    public String getVersion() {
        return "1.0.3";
    }
 
    /*
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#getType()
     * Retorna o nome do componente
     */
    public String getType() {
        return this.getClass().getName();
    }
 
    /*
     * @see com.ibm.wsspi.security.tai.TrustAssociationInterceptor#cleanup()
     * Esse metodo eh chamado quando o Liberty eh parado ou reiniciado, entao se
     * precisar fazer alguma limpeza coloque o codigo aqui
     */
    public void cleanup() {
        log.info(String.format("#INFO - Autenticador versao %s clean up", getVersion()));
        ItauTAICacheManager.disposeCache();
    }
}
 
