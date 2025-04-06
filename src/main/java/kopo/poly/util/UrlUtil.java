package kopo.poly.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

@Slf4j
public class UrlUtil {
    private String readAll(Reader rd){
        log.info("readAll Start");

        StringBuilder sb = null;

        try{
            sb = new StringBuilder();
            int cp = 0;

            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        } catch (IOException e) {
            log.info("readAll Execption : {}", e.toString());
        }
        log.info("readAll End");

        return sb.toString();
    }

    public String urlReadforString(String url) throws IOException {

        log.info("urlReadforString Start");
        log.info("urlReadforString url : {}", url);

        BufferedReader rd = null;
        InputStream is = null;
        String res="";

        try {
            is = new URL(url).openStream();

            rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            res = readAll(rd);
        } catch (Exception e){
            log.info("urlReadforString Exception : {}", e.toString());
        } finally {
            {
                is.close();
                is = null;
                rd = null;
            }

            log.info("urlReadforString End");
            return res;
        }
    }
}
