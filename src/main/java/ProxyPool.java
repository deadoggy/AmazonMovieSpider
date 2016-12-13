import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import static com.sun.org.apache.xalan.internal.lib.ExsltStrings.split;

/**
 * Created by deado on 2016/12/13.
 */
public class ProxyPool {
    static private String API = "http://api.goubanjia.com/api/get.shtml?order=fbbc179c0259954335b0fc52f22ac08b&num=100&carrier=0&protocol=0&an1=1&an2=2&an3=3&sp1=1&sort=2&system=1&distinct=0&rettype=1&seprator=%0D%0A";
    public String[] getProxy(){
        try{
            //get html
            HttpClient client = new DefaultHttpClient();
            HttpGet getHttp = new HttpGet(this.API);

            String  htmlContext = EntityUtils.toString(client.execute(getHttp).getEntity());
            return htmlContext.split("\r\n");

        }catch(Exception e) {
            return null;
        }
    }



}
