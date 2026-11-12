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

import yggdrasil.query.RangeQueryMTreeLocation;
import yggdrasil.storage.File;
import yggdrasil.storage.MTree;

public class App16thPointQueryLocation {

    public static final String FILE_TREINO_POINT_MTREE = App1stInsert.DIR_APP+"/treino_point_mtree.txt";
    public static void main(String[] args) throws NumberFormatException, IOException {
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        BufferedWriter txtTreino = new BufferedWriter(  
                new OutputStreamWriter(new FileOutputStream(FILE_TREINO_POINT_MTREE)));

        File f1 = new File(App1stInsert.FILE_MTREE);
        MTree<PointCity> mtree = new MTree<PointCity>(f1){};

        PointCity pointCity = new PointCity();
        double range = 0.0;
        StringTokenizer tok;
        int i = 0;

        while (txtFile.ready()) {
            if (i % 1000 == 0) {
                System.out.println(i);
            }
            tok = new StringTokenizer(txtFile.readLine(), "\t");
            pointCity.setName(tok.nextToken());
            pointCity.setLatitude(Double.parseDouble(tok.nextToken()));
            pointCity.setLongitude(Double.parseDouble(tok.nextToken()));


            //usamos a rangeQueryMtree, porem usamos range 0.0 para testar se funcionaria,
            //ao inves e criar uma nova consulta.

            RangeQueryMTreeLocation<PointCity> rangeQuery =
                    new RangeQueryMTreeLocation<PointCity>(mtree, pointCity, range){};
            Collection<PointCity> res = rangeQuery.solve();

            PointCity objRes = res.iterator().next();
            if (objRes.distanceTo(pointCity)==0.0){
                StringBuilder linha= new StringBuilder();
                // for(int j=0;j<50;j++){
                //     if(j<objRes.getName().length()){
                //         linha.append((int)objRes.getName().charAt(j));
                //         linha.append('\t');
                //     }else{
                //         linha.append('0');
                //         linha.append('\t');
                //     }
                // }
                //comentado pois nao sei se vamos usar o nome da cidade aqui tbm, ou melhor latitude e longitude;
                
                linha.append(objRes.getLatitude()).append('\t');
                linha.append(objRes.getLongitude()).append('\t');
                
                linha.append(objRes.getLeafPageId());
                txtTreino.write(linha.toString());
                txtTreino.newLine();
            }else{
                System.out.println("error:"+i);
            }
            i++;

            for (PointCity found : res) {
                if (pointCity.distanceTo(found) > range || found.getLeafPageId() == 0) {
                    System.out.println("error:" + i);
                    break;
                }
            }
        }
        txtTreino.close();
        txtFile.close();
    }
}