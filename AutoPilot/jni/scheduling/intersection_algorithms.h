/**
 * Intersection algorithms.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include "intersection.h"

#define INTERSECTION_ALGORITHM 0 // 0 = Brute force, 1 = Balaban, 2 = Bentley-Ottmann

#ifndef INTERSECTION_ALGORITHMS_H
#define INTERSECTION_ALGORITHMS_H

// End point (transition), can be used for sweep line or balaban
typedef struct end_point_struct {
    char left;
    float x,y;
    // int idx;
    segment *s;
} end_point;

#endif /* INTERSECTION_ALGORITHMS_H */

void printIntersection(segment *s1, segment *s2, point_2Df *p);
void reportIntersections(segment *segments, int segment_count);
void setReportFunction(void (*function)(segment*, segment*, point_2Df*));

float getYForX(segment *segment, float x);
void quicksort(end_point *transitions[], int low, int high);