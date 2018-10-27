/**
 * Intersection determination.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include "scheduler_tools.h"

#ifndef INTERSECTION_H
#define INTERSECTION_H

#ifndef RHO
#define RHO 400.0 // Radius of circles (relatively easy to go for variable radii)
#endif

typedef struct segment_struct {
    float x1;
    float y1;
    float x2;
    float y2;
    char is_line; // 1 : line (obviously!) - 0 : half-circle
    char is_top; // 1 : top of circle - 0 : bottom of circle
    int sb; // Lower bound (used in Balaban algorithm)
} segment;

#endif /* INTERSECTION_H */

short get_line_line_intersection(segment s1, segment s2, point_2Df *intersection);
short get_line_circle_intersection(segment line, segment circle, point_2Df *firstIntersection, point_2Df *secondIntersection);
short get_circle_circle_intersection(segment circle1, segment circle2, point_2Df *firstIntersection, point_2Df *secondIntersection);