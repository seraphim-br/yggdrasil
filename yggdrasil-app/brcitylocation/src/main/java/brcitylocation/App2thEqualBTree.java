package brcitylocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.EqualBTreeLocation;
import yggdrasil.storage.BTree;
import yggdrasil.storage.File;

public class App2thEqualBTree {

    public static final String FILE_TREINO_EQUAL_BTREE = App1stInsert.DIR_APP+"/treino_equal_btree.txt";

    public static void main(String[] args) throws NumberFormatException, IOException {

        BufferedWriter txtTreino = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_TREINO_EQUAL_BTREE)));

        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_BTREE);
        BTree<SortCity> btree = new BTree<>(f1) {};
        
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
            EqualBTreeLocation<SortCity> equal = 
                    new EqualBTreeLocation<>(btree, sortCity) {};
            Collection<SortCity> res = equal.solve();
            SortCity objRes = res.iterator().next();
            if (objRes.compareTo(sortCity)==0){
                StringBuilder linha= new StringBuilder();
                for(int j=0;j<50;j++){
                    if(j<objRes.getName().length()){
                        linha.append((int)objRes.getName().charAt(j));
                        linha.append('\t');
                    }else{
                        linha.append('0');
                        linha.append('\t');
                    }
                }
                linha.append(objRes.getLeafPageId());
                txtTreino.write(linha.toString());
                txtTreino.newLine();
            }else{
                System.out.println("error:"+i);
            }
            i++;
        }
        txtFile.close();
        txtTreino.close();
    
    }
}
