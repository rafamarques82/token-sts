package com.itau.EL9.custom.liberty.components;
 
import java.io.Serializable;
 
public class ItauTAICacheKey implements Serializable {
 
    /**
     *
     */
    private static final long serialVersionUID = 1L;
 
    // ctor
    public ItauTAICacheKey(String keyid) {
        super();
        this.keyId = keyid;
    }
 
    // fields
    String keyId;
 
    public String getKeyId() {
        return keyId;
    }
 
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
 
    @Override
    public boolean equals(Object o) {
 
        // Se for a mesma instancia basta retornar true
        if (o == this) {
            return true;
        }
 
        /*
         * ver se eh do mesmo tipo de objeto, se nao for eh falso
         */
        if (!(o instanceof ItauTAICacheKey)) {
            return false;
        }
 
        // Faz o cast e compara as chaves
        ItauTAICacheKey c = (ItauTAICacheKey) o;
        return (this.keyId.compareTo(c.getKeyId()) == 0);
    }
 
    @Override
    public int hashCode() {
        return keyId.hashCode();
    }
 
}
 
