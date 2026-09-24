package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.RangeQuerySequentialMetric;
import yggdrasil.storage.File;
import yggdrasil.storage.SequentialMetric;

public class App2ndRangeQuery {
    public static void main(String[] args) throws NumberFormatException, IOException {
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_SEQ_METRIC);
        SequentialMetric<PointCity> seq = new SequentialMetric<>(f1) {};
        
        PointCity pointCity = new PointCity();
        StringTokenizer tok;
        int i=0;
        while (txtFile.ready()){
            if(i % 1000 == 0){
                System.out.println(i);
            }
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            pointCity.setName(tok.nextToken());
            pointCity.setLatitude(Double.parseDouble(tok.nextToken()));
            pointCity.setLongitude(Double.parseDouble(tok.nextToken()));
            RangeQuerySequentialMetric<PointCity> range = 
                    new RangeQuerySequentialMetric<>(seq, pointCity, 0) {};
            Collection<PointCity> res = range.solve();
            if (!res.iterator().next().hasSameKey(pointCity)){
                System.out.println("error:"+i);
            }
            i++;
        }
        txtFile.close();
    
    }
}
