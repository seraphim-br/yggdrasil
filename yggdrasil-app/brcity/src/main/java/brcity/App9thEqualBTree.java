package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.EqualBTree;
import yggdrasil.storage.BTree;
import yggdrasil.storage.File;

public class App9thEqualBTree {
    public static void main(String[] args) throws NumberFormatException, IOException {
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_BTREE);
        BTree<SortCity> seq = new BTree<>(f1) {};
        
        SortCity sortCity = new SortCity();
        StringTokenizer tok;
        int i=0;
        while (txtFile.ready()){
            if(i % 1000 == 0){
                System.out.println(i);
            }
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            sortCity.setName(tok.nextToken());
            sortCity.setLatitude(Double.parseDouble(tok.nextToken()));
            sortCity.setLongitude(Double.parseDouble(tok.nextToken()));
            EqualBTree<SortCity> equal = 
                    new EqualBTree<>(seq, sortCity) {};
            Collection<SortCity> res = equal.solve();
            if (res.iterator().next().compareTo(sortCity)!=0){
                System.out.println("error:"+i);
            }
            i++;
        }
        txtFile.close();
    
    }
}
