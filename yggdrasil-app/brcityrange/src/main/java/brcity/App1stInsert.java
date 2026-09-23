package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import yggdrasil.storage.BTree;
import yggdrasil.storage.File;
import yggdrasil.storage.SequentialMetric;


public class App1stInsert {

    public static final String DIR_ROOT = System.getProperty("user.dir");
    public static final String FILE_BASE = DIR_ROOT+"/yggdrasil-data/brasil/brasil.txt";
    public static final String DIR_APP = DIR_ROOT+"/yggdrasil-app/brcityrange";
    public static final String FILE_SEQ = DIR_APP+"/PointCity.seq";
    public static final String FILE_BTREE = DIR_APP+"/OrderCity.btree";

    public static void main(String[] args) throws NumberFormatException, IOException, URISyntaxException {


        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(FILE_BASE)));

        File f1 = new File(FILE_SEQ, 4096);
        SequentialMetric<PointCity> seq = new SequentialMetric<PointCity>(f1) {};

        File f2 = new File(FILE_BTREE, 4096);
        BTree<OrderCity> btree = new BTree<OrderCity>(f2) {};

                
        PointCity pointCity = new PointCity();
        OrderCity orderCity = new OrderCity();
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
            seq.add(pointCity);
            orderCity.setName(pointCity.getName());
            btree.add(orderCity);
            i++;
        }
        txtFile.close();

    }
}