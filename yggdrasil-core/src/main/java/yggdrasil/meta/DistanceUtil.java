/*
Copyright (C) 2013     Enzo Seraphim

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
or visit <http://www.gnu.org/licenses/>
 */
package yggdrasil.meta;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 */
public class DistanceUtil {
    /**
     *
     * @param str1
     * @param str2
     * @return
     */
    public static double levenshtein(String str1, String str2) {
        String a = str1.toLowerCase();
        String b = str2.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
    
    
    /**
     *
     * @param p1
     * @param p2
     * @param rad
     * @return
     */
    public static double sphericalDistanceDegree(Point<?> p1, Point<?> p2, double rad) {
        double lat1, lat2, lon1, lon2;

        lat1 = p1.getOrigin(0) * Math.PI / 180;
        lat2 = p2.getOrigin(0) * Math.PI / 180;
        lon1 = p1.getOrigin(1) * Math.PI / 180;
        lon2 = p2.getOrigin(1) * Math.PI / 180;

        if (lat1 == lat2 && lon1 == lon2) {
            return 0;
        }
        return Math.acos(
                (Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2)) * Math.cos(Math.abs(lon2 - lon1))) * rad;
    }

    /**
     *
     * @param p1
     * @param p2
     * @param rad
     * @return
     */
    public static double sphericalDistanceRadian(Point<?> p1, Point<?> p2, double rad) {
        double lat1, lat2, lon1, lon2;

        lat1 = p1.getOrigin(0);
        lat2 = p2.getOrigin(0);
        lon1 = p1.getOrigin(1);
        lon2 = p2.getOrigin(1);

        if (lat1 == lat2 && lon1 == lon2) {
            return 0;
        }
        return Math.acos(
                (Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2)) * Math.cos(Math.abs(lon2 - lon1))) * rad;
    }

    /**
     *
     * @param point1
     * @param point2
     * @return
     */
    public static double euclidean(Point<?> point1, Point<?> point2) {
        int dims = point1.numberOfDimensions();
        double dist = 0;

        for (int i = 0; i < dims; i++) {
            dist += (point1.getOrigin(i) - point2.getOrigin(i)) * (point1.getOrigin(i) - point2.getOrigin(i));
        }

        return Math.sqrt(dist);
    }

    /**
     *
     * @param point1
     * @param point2
     * @return
     */
    public static double euclidean(double[] point1, double[] point2) {
        if (point1.length == point2.length) {
            double dist = 0;
            for (int i = 0; i < point1.length; i++) {
                dist += (point1[i] - point2[i]) * (point1[i] - point2[i]);
            }
            return Math.sqrt(dist);

        } else {
            return 0d;
        }
    }

    /**
     *
     * @param rectangle
     * @param point
     * @return
     */
    public static double euclidean(Point<?> point, Rectangle<?> rectangle) {
        int dims = point.numberOfDimensions();
        double dist = 0;

        for (int i = 0; i < dims; i++) {
            dist += (point.getOrigin(i) - rectangle.getOrigin(i))
                    * (point.getOrigin(i) - rectangle.getOrigin(i));
        }

        return Math.sqrt(dist);
    }

    /**
     *
     * @param rect1
     * @param rect2
     * @return
     */
    public static double euclidean(Rectangle<?> rect1, Rectangle<?> rect2) {
        int dims = rect1.numberOfDimensions();
        double dist = 0;

        for (int i = 0; i < dims; i++) {
            dist += (rect1.getOrigin(i) - rect2.getOrigin(i))
                    * (rect1.getOrigin(i) - rect2.getOrigin(i));
        }

        return Math.sqrt(dist);        
        /*        
        int dims = rect1.numberOfDimensions();
        double dist = 0;
        boolean flagEquals = true;
        for (int i = 0; i < dims; i++) {
            if (flagEquals) {
                if ((rect1.getOrigin(i) != rect2.getOrigin(i)) || (rect1.getExtension(i) != rect2.getExtension(i))) {
                    flagEquals = false;
                    dist += Math.pow(
                            Math.max(rect1.getOrigin(i) + rect1.getExtension(i), rect2.getOrigin(i) + rect2.getExtension(i))
                            - Math.min(rect1.getOrigin(i), rect2.getOrigin(i)), 2);
                }
            } else {
                dist += Math.pow(
                        Math.max(rect1.getOrigin(i) + rect1.getExtension(i), rect2.getOrigin(i) + rect2.getExtension(i))
                        - Math.min(rect1.getOrigin(i), rect2.getOrigin(i)), 2);
            }
        }
        if (flagEquals) {
            return 0;
        } else {
            return Math.sqrt(dist);
        }
        */
    }

    private static char[] amino = {
        'a', 'r', 'n', 'd', 'c', 'q', 'e', 'g', 'h', 'i', 'l', 'k', 'm', 'f', 'p', 's', 't', 'w', 'y', 'v'
    };// b x j o u z

    private static int aminoAcidToInt(char c) {
        for (int i = 0; i < amino.length; i++) {
            if (c == amino[i]) {
                return i;
            }
        }

        return -1;
    }

    /**
     *
     * @param str1
     * @param str2
     * @return
     */
    private static char[][] mpam = {
        {
            0, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 5, 4, 2
        },
        {
            2, 0, 2, 2, 4, 2, 2, 2, 2, 3, 3, 2, 2, 4, 2, 2, 2, 4, 4, 3
        },
        {
            2, 2, 0, 2, 4, 2, 2, 2, 2, 3, 3, 2, 2, 4, 2, 2, 2, 5, 4, 2
        },
        {
            2, 2, 2, 0, 4, 2, 2, 2, 2, 3, 3, 2, 3, 4, 2, 2, 2, 6, 4, 2
        },
        {
            3, 4, 4, 4, 0, 4, 4, 3, 4, 3, 4, 4, 4, 4, 3, 3, 3, 7, 3, 3
        },
        {
            2, 2, 2, 2, 4, 0, 2, 2, 2, 3, 3, 2, 2, 4, 2, 2, 2, 5, 4, 3
        },
        {
            2, 2, 2, 2, 4, 2, 0, 2, 2, 3, 3, 2, 3, 4, 2, 2, 2, 6, 4, 2
        },
        {
            2, 2, 2, 2, 3, 2, 2, 0, 2, 2, 3, 2, 2, 4, 2, 2, 2, 6, 4, 2
        },
        {
            2, 2, 2, 2, 4, 2, 2, 2, 0, 3, 3, 2, 3, 3, 2, 2, 2, 5, 3, 3
        },
        {
            2, 3, 3, 3, 3, 3, 3, 2, 3, 0, 1, 3, 2, 2, 2, 2, 2, 5, 3, 2
        },
        {
            2, 3, 3, 3, 4, 3, 3, 3, 3, 1, 0, 3, 1, 2, 3, 3, 2, 4, 2, 1
        },
        {
            2, 2, 2, 2, 4, 2, 2, 2, 2, 3, 3, 0, 2, 4, 2, 2, 2, 4, 4, 3
        },
        {
            2, 2, 2, 3, 4, 2, 3, 2, 3, 2, 1, 2, 0, 2, 2, 2, 2, 4, 3, 2
        },
        {
            3, 4, 4, 4, 4, 4, 4, 4, 3, 2, 2, 4, 2, 0, 4, 3, 3, 3, 1, 2
        },
        {
            2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 3, 2, 2, 4, 0, 2, 2, 5, 4, 2
        },
        {
            2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 3, 2, 2, 3, 2, 0, 2, 5, 4, 2
        },
        {
            2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 0, 5, 3, 2
        },
        {
            5, 4, 5, 6, 7, 5, 6, 6, 5, 5, 4, 4, 4, 3, 5, 5, 5, 0, 4, 5
        },
        {
            4, 4, 4, 4, 3, 4, 4, 4, 3, 3, 2, 4, 3, 1, 4, 4, 3, 4, 0, 3
        },
        {
            2, 3, 2, 2, 3, 3, 2, 2, 3, 2, 1, 3, 2, 2, 2, 2, 2, 5, 3, 0
        }
    };

    public static double protein(String str1, String str2) {
        byte[] bstr1 = str1.toLowerCase().getBytes();
        byte[] bstr2 = str2.toLowerCase().getBytes();

        int sizeI = bstr1.length + 1;
        int sizeJ = bstr2.length + 1;
        int align[][] = new int[sizeI][sizeJ];
        int i, j, cost, ins, upd, del, transp, idxI, idxJ;
        align[0][0] = 0;
        for (i = 1; i < sizeI; i++) {
            align[i][0] = i * 7;
        }//end for
        for (j = 1; j < sizeJ; j++) {
            align[0][j] = j * 7;
        }//end for

        // Without transp
        for (j = 1; j < sizeJ; j++) {
            // Cost			
            idxI = DistanceUtil.aminoAcidToInt((char) bstr1[0]);
            idxJ = DistanceUtil.aminoAcidToInt((char) bstr2[j - 1]);
            cost = mpam[idxI][idxJ];

            ins = align[0][j] + cost + 1;
            del = align[1][j - 1] + cost + 1;
            upd = align[0][j - 1] + cost;

            if ((ins < del) && (ins < upd)) {
                align[1][j] = ins;
            } else if (del < upd) {
                align[1][j] = del;
            } else {
                align[1][j] = upd;
            }
        }

        // With transp
        for (i = 2; i < sizeI; i++) {
            // Cost			
            idxI = DistanceUtil.aminoAcidToInt((char) bstr1[i - 1]);
            idxJ = DistanceUtil.aminoAcidToInt((char) bstr2[0]);
            cost = mpam[idxI][idxJ];

            ins = align[i - 1][1] + cost + 1;
            del = align[i][0] + cost + 1;
            upd = align[i - 1][0] + cost;

            if ((ins < del) && (ins < upd)) {
                align[i][1] = ins;
            } else if (del < upd) {
                align[i][1] = del;
            } else {
                align[i][1] = upd;
            }

            for (j = 2; j < sizeJ; j++) {
                // Cost				
                idxI = DistanceUtil.aminoAcidToInt((char) bstr1[i - 1]);
                idxJ = DistanceUtil.aminoAcidToInt((char) bstr2[j - 1]);
                cost = mpam[idxI][idxJ];

                ins = align[i - 1][j] + cost + 1;
                del = align[i][j - 1] + cost + 1;
                upd = align[i - 1][j - 1] + cost;
                transp = align[i - 2][j - 2] + cost;

                if ((ins < del) && (ins < upd) && (ins < transp)) {
                    align[i][j] = ins;
                } else if ((del < upd) && (del < transp)) {
                    align[i][j] = del;
                } else if (upd < transp) {
                    align[i][j] = upd;
                } else {
                    align[i][j] = transp;
                }
            }
        }
        return ((double) align[sizeI - 1][sizeJ - 1]);
//        return ((double) align[sizeI - 1][sizeJ - 1] / greaterLen);
    }

    /**
     *
     * @param matrix1
     * @param matrix2
     * @return
     */
    public static double veryLeastDistance(float[][] matrix1, float[][] matrix2) {
        //the very least function
        double min = Double.MAX_VALUE;
        double delta;
        boolean isEqual = true;
        boolean minIsEqual = false;

        for (int i = 0; i < matrix1.length; i++) // para cada descritor na matrix1
        {
            for (int j = 0; j < matrix2.length; j++) // para cada descritor na matrix2
            {
                delta = 0;
                for (int k = 0; k < matrix1[i].length; k++) // para cada feature
                {
                    isEqual = (isEqual && matrix1[i][k] == matrix2[j][k]);

//					delta += Math.abs(matrix1[i][k] - matrix2[j][k]);
                    delta += (matrix1[i][k] - matrix2[j][k]) * (matrix1[i][k] - matrix2[j][k]);

                    if (delta > min) {
                        break;
                    }
                }
                if (delta < min) {
                    min = delta;
                    minIsEqual = isEqual;
                }
            }
        }

        if (minIsEqual) {
            return 0;
        } else {
//			return min;
            return Math.sqrt(min);
        }
    }

    /**
     *
     * @param matrix1
     * @param matrix2
     * @return
     */
    public static double veryLeastDistance(double[][] matrix1,
            double[][] matrix2) {
        // the very least function
        double min = Double.MAX_VALUE;
        double delta;
        boolean isEqual = true;
        boolean minIsEqual = false;

        for (int i = 0; i < matrix1.length; i++) // para cada descritor na
        // matrix1
        {
            for (int j = 0; j < matrix2.length; j++) // para cada descritor na
            // matrix2
            {
                delta = 0;
                for (int k = 0; k < matrix1[i].length; k++) // para cada feature
                {
                    isEqual = (isEqual && matrix1[i][k] == matrix2[j][k]);

                    // delta += Math.abs(matrix1[i][k] - matrix2[j][k]);
                    delta += (matrix1[i][k] - matrix2[j][k])
                            * (matrix1[i][k] - matrix2[j][k]);

                    if (delta > min) {
                        break;
                    }
                }
                if (delta < min) {
                    min = delta;
                    minIsEqual = isEqual;
                }
            }
        }

        if (minIsEqual) {
            return 0;
        } else {
            // return min;
            return Math.sqrt(min);
        }
    }

    // CALCULA A DISTANCIA ENTRE AS AREAS DE DOIS HISTOGRAMAS METRICOS
    public static double metricHistogram(Point<?> p1, Point<?> p2) {
        double area1 = 0, area2 = 0;
        //for (int i=0;i<p1.numberOfDimensions();i+=2){
        //System.out.println("N="+p1.numberOfDimensions());
        for (int i = 0; i < p1.numberOfDimensions() - 3; i += 2) {
            area1 += ((p1.getOrigin(i + 1) + p1.getOrigin(i + 3)) * (Math.abs(p1.getOrigin(i) - p1.getOrigin(i + 2)))) / 2;
            area2 += ((p2.getOrigin(i + 1) + p2.getOrigin(i + 3)) * (Math.abs(p2.getOrigin(i) - p2.getOrigin(i + 2)))) / 2;
        }
        return Math.abs(area2 - area1);
    }

    private DistanceUtil() {
    }
}
