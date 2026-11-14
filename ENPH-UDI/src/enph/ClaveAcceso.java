package enph;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class ClaveAcceso {
    private static final int RONDAS=600000;
    static final int MINIMO=8;
    static void validar(char[] clave) {
        if(clave.length<MINIMO || clave.length>128)
            throw new IllegalArgumentException("La contraseña debe tener entre "+MINIMO+" y 128 caracteres.");
    }
    static String nueva(char[] clave) {
        validar(clave);
        byte[] sal=new byte[16];new SecureRandom().nextBytes(sal);
        return "P1$"+Base64.getEncoder().encodeToString(sal)+"$"
            +Base64.getEncoder().encodeToString(calcular(clave,sal));
    }
    static boolean coincide(char[] clave,String registro) {
        if(clave.length>128 || registro==null) return false;
        try {
            String[] partes=registro.split("\\$",-1);
            if(partes.length!=3 || !"P1".equals(partes[0]))return false;
            return MessageDigest.isEqual(calcular(clave,Base64.getDecoder().decode(partes[1])),
                   Base64.getDecoder().decode(partes[2]));
        } catch(IllegalArgumentException e){return false;}
    }
    private static byte[] calcular(char[] clave,byte[] sal) {
        PBEKeySpec spec=new PBEKeySpec(clave,sal,RONDAS,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(GeneralSecurityException e){throw new IllegalStateException(e);}
        finally{spec.clearPassword();}
    }
}
