/**
 * Intersection determination.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "intersection.h"

#pragma mark Intersections

// https://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect?rq=1
short get_line_line_intersection(segment s1, segment s2, point_2Df *intersection) {
    
    
    float s1_x = s1.x2 - s1.x1, s1_y = s1.y2 - s1.y1;
    float s2_x = s2.x2 - s2.x1, s2_y = s2.y2 - s2.y1;
    
    float s = (-s1_y * (s1.x1 - s2.x1) + s1_x * (s1.y1 - s2.y1)) / (-s2_x * s1_y + s1_x * s2_y);
    float t = ( s2_x * (s1.y1 - s2.y1) - s2_y * (s1.x1 - s2.x1)) / (-s2_x * s1_y + s1_x * s2_y);
    
    if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
        if (intersection != NULL) {
            intersection->x = s1.x1 + (t * s1_x);
            intersection->y = s1.y1 + (t * s1_y);
        }
        return 1;
    }
    
    return 0; // No collision
    
}

// Based on https://stackoverflow.com/questions/1073336/circle-line-segment-collision-detection-algorithm
//  (but adapted to segments as code as for lines)
// Assumes radius of RHO
short get_line_circle_intersection(segment line, segment circle, point_2Df *firstIntersection, point_2Df *secondIntersection) {
    
    float dx = line.x2-line.x1, dy = line.y2-line.y1;
    float l = sqrtf(dx*dx+dy*dy); // Length of line
    float dxl = dx/l, dyl = dy/l;
    float t = dxl*(circle.x1-line.x1) + dyl*(circle.y1-line.y1); // Projection of circle center on line
    
    float ex = t*dxl + line.x1, ey = t*dyl + line.y1; // Coordinates of e on line and closest to circle center
    float lec = sqrtf((ex-circle.x1)*(ex-circle.x1) + (ey-circle.y1)*(ey-circle.y1)); // Distance e to circle center
    
    if (lec < RHO) { // Intersection
        float dt = sqrtf(RHO*RHO - lec*lec); // Distance to to circle intersection point
        int nbi = 0;
        if (t-dt >=0 && t-dt <= l) {
            if (firstIntersection != NULL) {
                firstIntersection->x = (t-dt) * dxl + line.x1;
                firstIntersection->y = (t-dt) * dyl + line.y1;
            }
            nbi = 1;
        }
        if (t+dt >=0 && t+dt <= l) {
            if (nbi == 0) {
                if (firstIntersection != NULL) {
                    firstIntersection->x = (t+dt) * dxl + line.x1;
                    firstIntersection->y = (t+dt) * dyl + line.y1;
                }
                nbi = 1;
            }
            else {
                if (firstIntersection != NULL && secondIntersection != NULL) {
                    secondIntersection->x = (t+dt) * dxl + line.x1;
                    secondIntersection->y = (t+dt) * dyl + line.y1;
                }
                nbi = 2;
            }
        }
        return nbi;
    }
    else if (lec == RHO && t >=0 && t <= l) { // Tangent
        if (firstIntersection != NULL) {
            firstIntersection->x = ex;
            firstIntersection->y = ex;
        }
        return 1;
    }
    
    return 0;
    
}

// Also see https://stackoverflow.com/questions/3349125/circle-circle-intersection-points
// Assumes radius of RHO
short get_circle_circle_intersection(segment circle1, segment circle2, point_2Df *firstIntersection, point_2Df *secondIntersection) {
    
    float distance = sqrtf((circle1.x1-circle2.x1)*(circle1.x1-circle2.x1)
                           + (circle1.y1-circle2.y1)*(circle1.y1-circle2.y1));
    
    if (distance <= RHO*2) {
        
        if (distance == 0)
            return 0; // Circles are equal
        
        float d = distance/2;
        float z = sqrtf(RHO*RHO - d*d);
        
        float xp = circle1.x1 + d * (circle2.x1 - circle1.x1) / distance;
        float yp = circle1.y1 + d * (circle2.y1 - circle1.y1) / distance;
        
        if (firstIntersection != NULL) {
            firstIntersection->x = xp + z* (circle2.y1 - circle1.y1) / distance;
            firstIntersection->y = yp - z* (circle2.x1 - circle1.x1) / distance;
        }
        
        if (distance != RHO*2) {
            if (firstIntersection != NULL && secondIntersection != NULL) {
                secondIntersection->x = xp - z* (circle2.y1 - circle1.y1) / distance;
                secondIntersection->y = yp + z* (circle2.x1 - circle1.x1) / distance;
            }
            return 2;
        }
        else
            return 1;
        
    }
    
    return 0;
    
}