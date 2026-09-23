package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.EqualSequential;
import yggdrasil.storage.File;
import yggdrasil.storage.Sequential;

public class App4thEqual {
    public static void main(String[] args) throws NumberFormatException, IOException {
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_SEQ_ENTITY);
        Sequential<EntityCity> seq = new Sequential<>(f1) {};
        
        EntityCity entityCity = new EntityCity();
        StringTokenizer tok;
        int i=0;
        while (txtFile.ready()){
            if(i % 1000 == 0){
                System.out.println(i);
            }
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            entityCity.setName(tok.nextToken());
            entityCity.setLatitude(Double.parseDouble(tok.nextToken()));
            entityCity.setLongitude(Double.parseDouble(tok.nextToken()));
            EqualSequential<EntityCity> equal = 
                    new EqualSequential<>(seq, entityCity) {};
            Collection<EntityCity> res = equal.solve();
            if (!res.iterator().next().isEqual(entityCity)){
                System.out.println("error:"+i);
            }
            i++;
        }
        txtFile.close();
    
    }
}
