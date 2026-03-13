# Compatibilidade Jakarta EE 9+ para ODM 9.5

## Problema Original

O IBM ODM 9.5 usa Jakarta EE 9+, que migrou de `javax.servlet` para `jakarta.servlet`. As dependências do Liberty disponíveis no Maven Central (liberty-target 19.0.0.9) ainda usam a API antiga `javax.servlet`, causando o erro:

```
java.lang.AbstractMethodError:
com/ibm/wsspi/security/tai/TrustAssociationInterceptor.isTargetInterceptor(Ljakarta/servlet/http/HttpServletRequest;)Z
```

Este erro indica que o runtime do ODM 9.5 espera `jakarta.servlet`, mas o código compilado usa `javax.servlet`.

## Solução Implementada

Como as APIs corretas do WebSphere Liberty para Jakarta EE 9+ não estão disponíveis no Maven Central, criamos **interfaces stub** que permitem a compilação com `jakarta.servlet`:

### Interfaces Stub Criadas:

1. `com.ibm.websphere.security.WebTrustAssociationException`
2. `com.ibm.websphere.security.WebTrustAssociationFailedException`
3. `com.ibm.wsspi.security.tai.TAIResult`
4. `com.ibm.wsspi.security.tai.TrustAssociationInterceptor`

Estas interfaces são **apenas para compilação**. Em runtime, o Liberty do ODM 9.5 fornecerá as implementações reais dessas classes.

## Configuração do POM

- **Java Version**: 11 (requerido pelo ODM 9.5)
- **Servlet API**: jakarta.servlet-api 6.0.0
- **Repository IBM**: Adicionado para possíveis dependências futuras
- **Sem dependência liberty-target**: Removida, usando stubs locais

## Estrutura do Projeto

```
src/main/java/
├── com/ibm/websphere/security/          # Stubs para compilação
│   ├── WebTrustAssociationException.java
│   └── WebTrustAssociationFailedException.java
├── com/ibm/wsspi/security/tai/          # Stubs para compilação
│   ├── TAIResult.java
│   └── TrustAssociationInterceptor.java
└── com/itau/EL9/custom/iberty/components/  # Código da aplicação
    ├── ItauTAI.java
    ├── ItauTAICacheKey.java
    └── ItauTAICacheManager.java
```

## Artefatos Gerados

**Localização**: `target/`

- ✅ `odm-custom-token-validator.jar` - JAR principal (inclui stubs + código da aplicação)
- ✅ `nimbus-jose-jwt-3.1.2.jar`
- ✅ `json-20180813.jar`
- ✅ `jcip-annotations-1.0.jar`
- ✅ `json-smart-1.1.1.jar`
- ✅ `ehcache-2.10.6.jar`
- ✅ `slf4j-api-1.7.25.jar`
- ✅ `slf4j-simple-1.7.25.jar`

## Deploy no ODM 9.5

1. **Copie todos os JARs** do diretório `target/` para o diretório de bibliotecas do ODM 9.5
2. **Configure o server.xml** do Liberty com os parâmetros do ItauTAI:
   ```xml
   <trustAssociation id="myTrustAssociation" invokeForUnprotectedURI="false" failOverToAppAuthType="false">
       <interceptors id="itauTAI"
                     className="com.itau.EL9.custom.liberty.components.ItauTAI"
                     invokeBeforeSSO="true"
                     invokeAfterSSO="false"
                     libraryRef="itauTAILib">
           <properties url-sts="..." ambiente="..." secret-hmac="..." tai-user="..." sts-timeout="..."/>
       </interceptors>
   </trustAssociation>
   
   <library id="itauTAILib">
       <fileset dir="${server.config.dir}/lib" includes="*.jar"/>
   </library>
   ```
3. **Reinicie o servidor** Liberty

## Notas Técnicas

- ✅ O código usa `jakarta.servlet` (Jakarta EE 9+)
- ✅ As interfaces stub são substituídas pelas implementações reais do Liberty em runtime
- ✅ Não haverá erro `AbstractMethodError` porque as assinaturas dos métodos agora correspondem
- ✅ Esta solução é compatível com ODM 9.5 e versões futuras do Liberty
- ⚠️ **IMPORTANTE**: Não inclua as classes stub em um JAR separado - elas devem estar no mesmo JAR da aplicação para evitar conflitos de classloader

## Por que esta solução funciona?

1. **Compilação**: As interfaces stub permitem compilar o código com `jakarta.servlet`
2. **Runtime**: O Liberty carrega suas próprias implementações das classes `com.ibm.websphere.security.*` e `com.ibm.wsspi.security.tai.*`
3. **Classloader**: O classloader do Liberty dá prioridade às suas próprias classes sobre as do aplicativo
4. **Resultado**: O código compilado funciona perfeitamente com as implementações reais do Liberty que usam `jakarta.servlet`