package brcitylocation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import yggdrasil.query.RangeQueryMTreeLocation;
import yggdrasil.storage.File;
import yggdrasil.storage.MTree;

public class App15thRangeQueryMTreeLocation {
    public static void main(String[] args) throws NumberFormatException, IOException {
        BufferedReader txtFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(App1stInsert.FILE_BASE)));

        File f1 = new File(App1stInsert.FILE_MTREE);
        MTree<PointCity> mtree = new MTree<PointCity>(f1){};

        PointCity pointCity = new PointCity();
        double range = 0.5;
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

            RangeQueryMTreeLocation<PointCity> rangeQuery =
                    new RangeQueryMTreeLocation<PointCity>(mtree, pointCity, range){};
            Collection<PointCity> res = rangeQuery.solve();

            for (PointCity found : res) {
                if (pointCity.distanceTo(found) > range || found.getLeafPageId() == 0) {
                    System.out.println("error:" + i);
                    break;
                }
            }
            i++;
        }
        txtFile.close();
    }
}