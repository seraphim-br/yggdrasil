package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.MoreThanBTree;
import yggdrasil.storage.BTree;
import yggdrasil.storage.File;

public class App11thMoreThanBTree {
    public static void main(String[] args) throws NumberFormatException, IOException{
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_BTREE);
        BTree<SortCity> btree = new BTree<SortCity>(f1){};

        SortCity sortCity = new SortCity();
        StringTokenizer tok;
        int i = 0;
        while(txtFile.ready()){
            if(i % 1000 == 0){
                System.out.println(i);
            }
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            sortCity.setName(tok.nextToken());
            Double.parseDouble(tok.nextToken());
            Double.parseDouble(tok.nextToken());

            MoreThanBTree<SortCity> moreThan = 
                    new MoreThanBTree<SortCity>(btree, sortCity){};
            Collection<SortCity> res = moreThan.solve();

            for(SortCity found : res){
                if(found.compareTo(sortCity) <= 0){
                    System.out.println("error:" + i);
                    break;
                }
            }
            i++;
        }
        txtFile.close();
    }
}