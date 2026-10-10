package kr.co.jobhub.auth.service;
import org.springframework.stereotype.Service; import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.security.SecureRandom;
@Service public class TotpService {
 private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
 public String secret(){byte[] b=new byte[20];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();int buffer=0,bits=0;for(byte v:b){buffer=(buffer<<8)|(v&255);bits+=8;while(bits>=5){s.append(ALPHABET.charAt((buffer>>(bits-=5))&31));}}return s.toString();}
 public boolean verify(String secret,String code){if(secret==null||code==null||!code.matches("\\d{6}"))return false;long step=System.currentTimeMillis()/30000;for(long d=-1;d<=1;d++)if(code.equals(code(secret,step+d)))return true;return false;}
 private String code(String secret,long counter){try{byte[] key=decode(secret);byte[] msg=new byte[8];for(int i=7;i>=0;i--){msg[i]=(byte)counter;counter>>>=8;}Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));byte[] h=mac.doFinal(msg);int o=h[h.length-1]&15;int n=((h[o]&127)<<24)|((h[o+1]&255)<<16)|((h[o+2]&255)<<8)|(h[o+3]&255);return String.format("%06d",n%1_000_000);}catch(Exception e){return "";}}
 private byte[] decode(String s){int buffer=0,bits=0,index=0;byte[] out=new byte[s.length()*5/8];for(char c:s.toCharArray()){int v=ALPHABET.indexOf(c);if(v<0)continue;buffer=(buffer<<5)|v;bits+=5;if(bits>=8)out[index++]=(byte)(buffer>>(bits-=8));}return out;}
}
