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

import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Enzo Seraphim <seraphim@unifei.edu.br>
 * @author Luiz Olmes Carvalho <olmes@icmc.usp.br>
 * @author Thatyana de Faria Piola Seraphim <thatyana@unifei.edu.br>
 *
 * @param <K>
 */
public class EuclideanGeometry<K extends Rectangle<K>> {

    private Class<K> keyClass = null;
    private long verification = 0;
    private final double precisionError = 0.00000001; //incrementa nona casa

    /**
     *
     * @param keyClass
     */
    public EuclideanGeometry(Class<K> keyClass) {
        this.keyClass = keyClass;
    }

    private K newGenericType()  {
        try {
            return keyClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param rect1
     * @param rect2
     * @return
     */
    public K union(K rect1, K rect2) {
        int dims = rect1.numberOfDimensions();
        double[] minPoint = new double[dims];
        double[] maxPoint = new double[dims];
        double coord1;
        double coord2;
        K mbrUnion = this.newGenericType();
        verification++;

        for (int i = 0; i < dims; i++) {
            coord1 = rect1.getOrigin(i);
            coord2 = rect2.getOrigin(i);
            minPoint[i] = Math.min(coord1, coord2);
            maxPoint[i] = Math.max(coord1 + rect1.getExtension(i), coord2 + rect2.getExtension(i));
            mbrUnion.setOrigin(i, minPoint[i]);
            mbrUnion.setExtension(i, maxPoint[i] - minPoint[i] + this.precisionError);
        }
        return mbrUnion;
    }

    /**
     *
     * @param rect
     * @return
     */
    public double occupancy(K rect) {
        double ocup = 1;
        int dims = rect.numberOfDimensions();

        for (int i = 0; i < dims; i++) {
            ocup *= rect.getExtension(i);
        }

        return ocup;
    }

    /**
     *
     * @param rectOverlap
     * @param rectOverlaped
     * @return
     */
    public boolean isOverlap(K rectOverlap, K rectOverlaped) {
        int dims = rectOverlap.numberOfDimensions();
        verification++;
//    return this.x < r.x + r.width && 
//           this.x + width > r.x && 
//           this.y < r.y + r.height && 
//           this.y + height > r.y;
        for (int i = 0; i < dims; i++) {
            if (rectOverlap.getOrigin(i) > 
                    rectOverlaped.getOrigin(i) + rectOverlaped.getExtension(i) ) {
                return false;
            }
            if (rectOverlap.getOrigin(i) + rectOverlap.getExtension(i) <
                    rectOverlaped.getOrigin(i)) {
                return false;
            }
        }

        return true;
    }

    public K cicleMBR(Point<?> point, double range){
        K rect = this.newGenericType();
        int dims = rect.numberOfDimensions();
        verification++;
        //set origin
        for (int i = 0; i < dims; i++) {
            rect.setOrigin(i, point.getOrigin(i) - range);
            rect.setExtension(i, 2 * range);
        }        
        return rect;
    }
        
    public double distancePointToLine2D(K point, K startPoint, K endPoint){
        double x1, y1, x2, y2, px, py, dotprod, projlenSq;
        verification++;
        x1 = startPoint.getOrigin(0); 
        y1 = startPoint.getOrigin(1);
        x2 = endPoint.getOrigin(0);
        y2 = endPoint.getOrigin(1);
        px = point.getOrigin(0);
        py =point.getOrigin(1);
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        dotprod = px * x2 + py * y2;
        if (dotprod <= 0.0) {
            // px,py is on the side of x1,y1 away from x2,y2
            // distance to segment is length of px,py vector
            // "length of its (clipped) projection" is now 0.0
            projlenSq = 0.0;
        } else {
            // switch to backwards vectors relative to x2,y2
            // x2,y2 are already the negative of x1,y1=>x2,y2
            // to get px,py to be the negative of px,py=>x2,y2
            // the dot product of two negated vectors is the same
            // as the dot product of the two normal vectors
            px = x2 - px;
            py = y2 - py;
            dotprod = px * x2 + py * y2;
            if (dotprod <= 0.0) {
                // px,py is on the side of x2,y2 away from x1,y1
                // distance to segment is length of (backwards) px,py vector
                // "length of its (clipped) projection" is now 0.0
                projlenSq = 0.0;
            } else {
                // px,py is between x1,y1 and x2,y2
                // dotprod is the length of the px,py vector
                // projected on the x2,y2=>x1,y1 vector times the
                // length of the x2,y2=>x1,y1 vector
                projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
            }
        }
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        // (which is zero if the projection falls outside the range
        //  of the line segment).
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return Math.sqrt(lenSq);
    }    

    public boolean intersectionCircletoToSegment2D(Point<?> point, double radius, Point<?> startPoint, Point<?> endPoint) {
        /* 
        * This program finds if an intersection between a line segment and circle exists.
        * It uses the following formulation:
        * Line seg: x = x1 + lambda * dx
        * Line seg: y = y1 + lambda * dy
        * 
        * Plug in for circle equation
        * (x1 + lambda * x - cx)^2 + (y1 + lamda * dy - cy)^2 = r^2
        * where (cx,cy) is the center of the circle with radius r.
        *
        * Rewrite the equation
        * (dx^2 + dy^2)lambda^2 - 2(dx (x1 - cx) + dy(y1 - cy))lambda + (x1 - cx)^2 + (y1 - cy)^2 - r^2 = 0
        * use the quadratic formula to determine if a solution exists (see function)
        */
        
        double circlex = point.getOrigin(0);
        double circley = point.getOrigin(1);
        double x1 = startPoint.getOrigin(0);
        double y1 = startPoint.getOrigin(1);
        double x2 = endPoint.getOrigin(0);
        double y2 = endPoint.getOrigin(1);
        
        
        //Calculate change in x and y for the segment
        double deltax = x2 - x1;
        double deltay = y2 - y1;

        //Set up our quadratic formula
        double a = deltax * deltax + deltay * deltay;
        double b = 2 * (deltax * (x1-circlex) + deltay * (y1 - circley));
        double c = (x1 - circlex) * (x1 - circlex) + (y1 - circley) * (y1 - circley) - radius * radius;

        //Check if there is a negative in the discriminant
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) 
                return false;

        //Try both +- in the quadratic formula
        double quad1 = (-b + Math.sqrt(discriminant))/(2 * a);
        double quad2 = (-b - Math.sqrt(discriminant))/(2 * a);
        verification++;
        //If the result is between 0 and 1, there is an intersection
        if (quad1 >= 0.0 && quad1 <= 1.0) 
                return true;
        else if (quad2 >= 0.0 && quad2 <= 1.0) 
                return true;
        return false;
    }
    
    /**
     *
     * @return
     */
    public long getVerifications() {
        return verification;
    }

    /**
     *
     */
    public void resetVerifications() {
        verification = 0;
    }
}
