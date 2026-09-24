package brcity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import yggdrasil.storage.BTree;
import yggdrasil.storage.File;
import yggdrasil.storage.Sequential;
import yggdrasil.storage.SequentialMetric;
import yggdrasil.storage.SequentialSort;


public class App1stInsert {

    public static final String DIR_ROOT = System.getProperty("user.dir");
    public static final String FILE_BASE = DIR_ROOT+"/yggdrasil-data/brasil/brasil.txt";
    public static final String DIR_APP = DIR_ROOT+"/yggdrasil-app/brcityrange";
    public static final String FILE_SEQ_ENTITY = DIR_APP+"/EntityCity.seq";
    public static final String FILE_SEQ_METRIC = DIR_APP+"/PointCity.seq";
    public static final String FILE_SEQ_SORT = DIR_APP+"/SortCity.seq";
    public static final String FILE_BTREE = DIR_APP+"/SortCity.btree";

    public static void main(String[] args) throws NumberFormatException, IOException, URISyntaxException {


        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(FILE_BASE)));

        File f1 = new File(FILE_SEQ_ENTITY, 4096);
        Sequential<EntityCity> seqEntity = new Sequential<EntityCity>(f1) {};

        File f2 = new File(FILE_SEQ_METRIC, 4096);
        SequentialMetric<PointCity> seqMetric = new SequentialMetric<PointCity>(f2) {};

        File f3 = new File(FILE_SEQ_SORT, 4096);
        SequentialSort<SortCity> seqSort = new SequentialSort<SortCity>(f3) {};

        File f4 = new File(FILE_BTREE, 4096);
        BTree<SortCity> btree = new BTree<SortCity>(f4) {};

                
        PointCity pointCity = new PointCity();
        SortCity sortCity = new SortCity();
        EntityCity entityCity = new EntityCity();
        StringTokenizer tok;
        int i=0;
        while (txtFile.ready()){
            if(i % 1000 == 0){
                System.out.println(i);
            }
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            //seq entity
            entityCity.setName(tok.nextToken());
            entityCity.setLatitude(Double.parseDouble(tok.nextToken()));
            entityCity.setLongitude(Double.parseDouble(tok.nextToken()));
            seqEntity.add(entityCity);
            //seq metric
            pointCity.setLatitude(entityCity.getLatitude());
            pointCity.setLongitude(entityCity.getLongitude());
            seqMetric.add(pointCity);
            //seq sort
            sortCity.setName(entityCity.getName());
            seqSort.add(sortCity);
            //btree
            btree.add(sortCity);
            i++;
        }
        txtFile.close();

    }
}