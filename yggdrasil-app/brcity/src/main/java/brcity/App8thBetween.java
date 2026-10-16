package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.BetweenSequentialOrder;
import yggdrasil.storage.File;
import yggdrasil.storage.SequentialSort;

public class App8thBetween {
    public static void main(String[] args) throws NumberFormatException, IOException {
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_SEQ_SORT);
        SequentialSort<SortCity> seq = new SequentialSort<>(f1) {};
        
        SortCity prevCity = null;
        StringTokenizer tok;
        int i = 0;
        
        while (txtFile.ready()) {
            if (i % 1000 == 0) {
                System.out.println(i);
            }
            
            SortCity sortCity = new SortCity();
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            sortCity.setName(tok.nextToken());
            sortCity.setLatitude(Double.parseDouble(tok.nextToken()));
            sortCity.setLongitude(Double.parseDouble(tok.nextToken()));
            
            if (prevCity == null) {
                prevCity = sortCity;
            }

            SortCity lower = (prevCity.compareTo(sortCity) <= 0) ? prevCity : sortCity;
            SortCity upper = (prevCity.compareTo(sortCity) <= 0) ? sortCity : prevCity;

            BetweenSequentialOrder<SortCity> between = 
                    new BetweenSequentialOrder<>(seq, lower, upper) {};
            
            Collection<SortCity> res = between.solve();
            
            for (SortCity c : res) {
                if (c.compareTo(lower) < 0 || c.compareTo(upper) > 0) {
                    System.out.println("error:" + i);
                    break;
                }
            }
            
            prevCity = sortCity;
            i++;
        }
        txtFile.close();
    }
}