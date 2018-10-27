
/**
 * Intersection algorithms.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "intersection_algorithms.h"
#include "vector.h"

#define BALABAN_DEBUG 0

void (*reportFunction)(segment*, segment*, point_2Df*) = printIntersection;
void setReportFunction(void (*function)(segment*, segment*, point_2Df*)) {
    reportFunction = function;
}

// Algorithms
short getIntersections(segment *s1, segment *s2, point_2Df *p1, point_2Df *p2);
void reportIntersectionsBruteForce(segment *segments, int segment_count);
void reportIntersectionsBentleyOttmann(segment *segments, int segment_count);
void reportIntersectionsBalaban(segment *segments, int segment_count);

// Utility functions
void sortSegments(segment *segments, int segment_count);
float getYForX(segment *segment, float x);

// Balaban functions
void balabanIntersectingPairs(segment *segments, int segment_count);
void balabanTreeSearch(vector *L_v, vector *I_v, vector *Q_ftv, int b, int e, vector *R_v);
void balabanSplit(vector *L_v, vector *Q_ftv, vector *Or_v, vector *L_lsv, int b, int e);
void balabanSplitOld(vector *L, vector *Q, vector *Li, float bx, float ex);
void balabanSearchInStrip(vector *L, vector *R, int b, int e);
void balabanBuildSQBbe(vector *Or_v, vector *Q_ftv, vector *Q_v, int SB_v[], int b, int e);
void balabanReportIntersections(vector *staircase, vector *L_lsv);
void balabanMerge(vector *v1, vector *v2, vector *destination, float x);
char balabanIsIntersecting(segment *s1, segment *s2, float bx, float ex);
int balabanGetLocation(vector *staircase, float bx, float ex, segment *segment);
void findIntersectionsSegmentStaircase(vector *staircase, float bx, float ex, int i, segment *s);
void findIntersectionsUnsorted(vector *staircase, vector *segments, float bx, float ex);
void findIntersectionsSorted(vector *staircase, vector *segments, float bx, float ex, float x);

#pragma mark Intersection Algorithms

// Print the given intersection
void printIntersection(segment *s1, segment *s2, point_2Df *p) {
    return;
    printf("New intersection point : [%f %f]\n", p->x, p->y);
}

// Calculate intersections in O(N^2)
void reportIntersectionsBruteForce(segment *segments, int segment_count) {
    short nb;
    point_2Df p1, p2;
    for (int i=0 ; i<segment_count ; i++)
        for (int j=i+1 ; j<segment_count ; j++) {
            nb = getIntersections(&segments[i], &segments[j], &p1, &p2);
            if (nb > 0) reportFunction(&segments[i], &segments[j], &p1);
            if (nb > 1) reportFunction(&segments[i], &segments[j], &p2);
        }
}

// Calculate intersections in O((N+K)*log(N))
void reportIntersectionsBentleyOttmann(segment *segments, int segment_count) {
    // TODO (transitions in priority queue probably, then use RB-tree for order etc.)
    // Code written elsewhere, just copy paste when needed
}

// Calculate intersections in O(N*log(N)+K)
void reportIntersectionsBalaban(segment *segments, int segment_count) {
    balabanIntersectingPairs(segments, segment_count);
}

// Calculate intersections of given segments
void reportIntersections(segment *segments, int segment_count) {
#if INTERSECTION_ALGORITHM == 0
    reportIntersectionsBruteForce(segments, segment_count);
#elif INTERSECTION_ALGORITHM == 1
    reportIntersectionsBalaban(segments, segment_count);
#else
    reportIntersectionsBentleyOttmann(segments, segment_count);
#endif
}

#pragma mark Utilities

// Get intersections of given trajectory segments
short getIntersections(segment *s1, segment *s2, point_2Df *p1, point_2Df *p2) {
    if (s1->is_line) {
        if (s2->is_line) return get_line_line_intersection(*s1, *s2, p1);
        else return get_line_circle_intersection(*s1, *s2, p1, p2);
    }
    else {
        if (s2->is_line) return get_line_circle_intersection(*s2, *s1, p1, p2);
        else return get_circle_circle_intersection(*s1, *s2, p1, p2);
    }
}

#pragma mark Balaban

end_point **transitions; // Endpoints
point_2Df *balabanP1, *balabanP2; // Containers for intersections

// Entry point
void balabanIntersectingPairs(segment *segments, int segment_count) {
    
    if (segment_count < 1)
        return;
    
    balabanP1 = malloc(sizeof(point_2Df));
    balabanP2 = malloc(sizeof(point_2Df));
    
    vector L_v, I_v, Q_ftv, R_v;
    vector_init(&L_v);
    vector_init(&I_v);
    vector_init(&R_v);
    vector_init(&Q_ftv);
    
    // Keep list of endpoints
    transitions = malloc(sizeof(end_point*)*(segment_count*2));
    int j=0;
    for (int i=0 ; i<segment_count ; i++) {
        
        segments[i].sb = 0;
        
        // Left
        transitions[j] = malloc(sizeof(end_point));
        transitions[j]->left = 1;
        transitions[j]->x = segments[i].x1;
        transitions[j]->y = segments[i].y1;
        // transitions[j]->idx = j;
        transitions[j]->s = &segments[i];
        
        // Right
        transitions[j+1] = malloc(sizeof(end_point));
        transitions[j+1]->left = 0;
        transitions[j+1]->x = segments[i].x2;
        transitions[j+1]->y = segments[i].y2;
        // transitions[j]->idx = j;
        transitions[j+1]->s = &segments[i];
        
        vector_add(&I_v, &segments[i]);
        
        j += 2;
        
    }
    
    // Sort list of endpoints
    quicksort(transitions, 0, segment_count*2-1); // Could use qsort(*) ...
    
    // Tree search
    vector_delete_element(&I_v, transitions[0]->s);
    vector_delete_element(&I_v, transitions[segment_count*2-1]->s);
    vector_add(&L_v, transitions[0]->s);
    balabanTreeSearch(&L_v, &I_v, &Q_ftv, 0, segment_count*2-1, &R_v);
    
    // Release memory
    for (int i=0 ; i<segment_count*2 ; i++)
        free(transitions[i]);
    vector_free(&L_v);
    vector_free(&I_v);
    vector_free(&Q_ftv);
    vector_free(&R_v);
    free(balabanP1);
    free(balabanP2);
    
}

#define PRINT_VECTOR(vector) for (int i=0 ; i<vector_count(vector) ; i++) printf("%.0f %.0f\n", ((segment*)vector_get(vector, i))->x1, ((segment*)vector_get(vector,i))->x2);

// Tree search (optimal version)
void balabanTreeSearch(vector *L_v, vector *I_v, vector *Q_ftv, int b, int e, vector *R_v) {
    
#if BALABAN_DEBUG
    printf("--- TREE SEARCH FOR %i %i\n", b, e);
    PRINT_VECTOR(L_v);
    printf("-----\n");
#endif
    
    // Step 1 **
    if (e-b == 1)
        return balabanSearchInStrip(L_v, R_v, b, e);
    float bx = transitions[b]->x, ex = transitions[e]->x;
    
    // Step 2 *
    vector Or_v;
    vector L_lsv;
    vector Q_v;
    vector_init(&Or_v);
    vector_init(&L_lsv);
    vector_init(&Q_v);
    balabanSplit(L_v, Q_ftv, &Or_v, &L_lsv, b, e);
    
#if BALABAN_DEBUG
    printf("--- LLSV %.0f -- %.0f\n", bx, ex);
    PRINT_VECTOR(&L_lsv);
    printf(">-----\n");
    PRINT_VECTOR(&Or_v);
    printf("-----\n");
#endif
    
    // Step 3 *
#if BALABAN_DEBUG
    printf("%i\n", (vector_count(Q_ftv)+1));
#endif
    int *SB_v = (int *)malloc(sizeof(int)*(vector_count(Q_ftv)+vector_count(&Or_v)+1));
    balabanBuildSQBbe(&Or_v, Q_ftv, &Q_v, SB_v, b, e);
    free(SB_v);
    
    // Step 4
    for (int i=0 ; i<vector_count(&L_lsv) ; i++) {
        // segment s = vector_get(&L_lsv, i)
        // Find location based on s.bs
        // Save that location in s.bs
    }
    
    // Step 5
    
    
    // Step 6 **
    int c = (b + e)/2;
    float cx = transitions[c]->x;
    
    // Step 7 **
    vector I_lsv;
    vector I_rsv;
    vector_init(&I_lsv);
    vector_init(&I_rsv);
    for (int i=0 ; i<vector_count(I_v) ; i++) {
        segment *segment = vector_get(I_v, i);
        if (segment->x1 > bx && segment->x2 < cx)
            vector_add(&I_lsv, segment);
        if (segment->x1 > cx && segment->x2 < ex)
            vector_add(&I_rsv, segment);
    }
    
    // Step 8 **
    vector R_lsv;
    vector_init(&R_lsv);
    balabanTreeSearch(&L_lsv, &I_lsv, &Q_v, b, c, &R_lsv);
    
    // Step 9 **
    vector *L_rsv = &R_lsv;
    segment *sc = transitions[c]->s;
    if (transitions[c]->left)
        vector_add(L_rsv, sc);
    else
        vector_delete_element(&R_lsv, sc);
    
#if BALABAN_DEBUG
    printf("--- LRSV %.0f -- %.0f\n", bx, ex);
    PRINT_VECTOR(L_rsv);
    printf("-----\n");
#endif
    
    // Step 10 **
    vector R_rsv;
    vector_init(&R_rsv);
    balabanTreeSearch(L_rsv, &I_rsv, &Q_v, c, e, &R_rsv);
    
    // Step 11
    for (int i=0 ; i<vector_count(&R_rsv) ; i++) {
        
    }
    
    // Step 12
    
    
    // Step 13
    
    
    // Step 14
    
    
    // Step 15 **
    balabanMerge(&Q_v, &R_rsv, R_v, ex);
    
    // Free vectors
    vector_free(&Q_v);
    vector_free(&Or_v);
    vector_free(&L_lsv);
    vector_free(&R_lsv);
    vector_free(&R_rsv);
    vector_free(&I_lsv);
    vector_free(&I_rsv);
    
}

// Merge given arrays
//  assumes that v1 and v2 are sorted at x
//  returns vector with all elements of v1 and v2, sorted at x
void balabanMerge(vector *v1, vector *v2, vector *destination, float x) {
    int nL = vector_count(v1), nR = vector_count(v2);
    int i=0, j=0, k=0;
    segment *leftj, *rightk;
    while (j < nL && k < nR) {
        leftj = vector_get(v1, j);
        rightk = vector_get(v2, k);
        if (getYForX(leftj, x) < getYForX(rightk, x)) {
            vector_add(destination, leftj);
            j++;
        }
        else {
            vector_add(destination, rightk);
            k++;
        }
        i++;
    }
    while (j < nL) {
        vector_add(destination, vector_get(v1, j));
        j++;
        i++;
    }
    while (k < nR) {
        vector_add(destination, vector_get(v2, k));
        k++;
        i++;
    }
}

// Split in stair and set (for optimal algorithm)
void balabanSplit(vector *L_v, vector *Q_ftv, vector *Or_v, vector *L_lsv, int b, int e) {
    int i=0, n=vector_count(Q_ftv);
    float bx = transitions[b]->x, ex = transitions[e]->x;
    for (int j=0 ; j<vector_count(L_v) /*k*/ ; j++) {
        segment *sj = vector_get(L_v,j);
        while (i<n && getYForX(vector_get(Q_ftv,i), bx) < getYForX(sj, bx))
            i++;
        if (sj->x1 <= bx && sj->x2 >= ex
            && !balabanIsIntersecting(vector_get(Q_ftv, i-2), sj, bx, ex)
            && !balabanIsIntersecting(vector_get(Q_ftv, i+1), sj, bx, ex)
            && !balabanIsIntersecting(vector_get(Or_v, vector_count(Or_v)-1), sj, bx, ex))
            vector_add(Or_v, sj);
        else
            vector_add(L_lsv, sj);
    }
}

// Old split in stair and set
void balabanSplitOld(vector *L, vector *Q, vector *Li, float bx, float ex) {
    segment *lastFromQ = NULL;
    for (int j=0 ; j<vector_count(L) ; j++) {
        segment *sj = vector_get(L, j);
        if (sj->x1 <= bx && sj->x2 >= ex
            && !balabanIsIntersecting(sj, lastFromQ, bx, ex)) {
            vector_add(Q, sj);
            lastFromQ = sj;
        }
        else
            vector_add(Li, sj);
    }
}

// Search in strip <b,e>
void balabanSearchInStrip(vector *L, vector *R, int b, int e) {
    
    vector Q, Li;
    vector_init(&Q);
    vector_init(&Li);
    
    float bx = transitions[b]->x, ex = transitions[e]->x;
    
    balabanSplitOld(L, &Q, &Li, bx, ex);
    if (vector_count(&Li) == 0) {
        vector_copy(R, &Q);
        vector_free(&Li);
        // vector_free(&Q); // Is okay normally releases data but now R holds that data
        return;
    }
    
    findIntersectionsSorted(&Q, &Li, bx, ex, bx);
    
    vector Ri;
    vector_init(&Ri);
    balabanSearchInStrip(&Li, &Ri, b, e);
    balabanMerge(&Q, &Ri, R, transitions[e]->x); // Result sorted by e
    
    vector_free(&Ri);
    vector_free(&Li);
    vector_free(&Q);
    
}

// Build SB_v array for calculating Loc(D,s) in O(1)
void balabanBuildSQBbe(vector *Or_v, vector *Q_ftv, vector *Q_v, int *SB_v, int b, int e) {
    SB_v[0] = 0;
    int i=0, l=0, n=vector_count(Q_ftv);
    float bx = transitions[b]->x, ex = transitions[e]->x;
    for (int j=0 ; j<vector_count(Or_v) ; j++) {
        segment *sj = vector_get(Or_v,j);
        while (i<n && getYForX(vector_get(Q_ftv, i), bx) < getYForX(sj, bx)) {
            segment *sj1 = vector_get(Or_v, j-1);
            segment *si = vector_get(Q_ftv, i);
            if (i%4 == 0
                && !balabanIsIntersecting(si, sj, bx, ex)
                && !balabanIsIntersecting(si, sj1, bx, ex)) {
                vector_add(Q_v, si);
                l++;
                SB_v[l] = i;
            }
            i++;
        }
        vector_add(Q_v, sj);
        l++;
        if (balabanIsIntersecting(sj, vector_get(Q_ftv, i-1), bx, ex))
            SB_v[l] = i-2;
        else
            SB_v[l] = i-1;
    }
}

// Report intersections for staircase and complementing set
void balabanReportIntersections(vector *staircase, vector *L_lsv) {
    
    
    
}

// Find location of stair where s has a location between i-1 and i-th stairs of staircase(work, (b, e))
int balabanGetLocation(vector *staircase, float bx, float ex, segment *segment) {
    float x = (segment->x1 > bx ? segment->x1 : bx);
    int l=0, h=vector_count(staircase), c;
    while (h != l) {
        c = (l+h)/2;
        if (getYForX(segment, x) < getYForX(vector_get(staircase, c), x))
            h = c;
        else
            l = c+1;
    }
    return h;
}

// Find all intersections of segment seg with staircase(work, (b, e))
//  knowing that seg has points between i-1 and i-th stairs
// = O(k+1), k = #intersections
void findIntersectionsSegmentStaircase(vector *staircase, float bx, float ex, int i, segment *s) {
    
    int is, h = i;
    segment *segment1;
    
    while (h < vector_count(staircase)) {
        segment1 = vector_get(staircase, h);
        is = getIntersections(segment1, s, balabanP1, balabanP2);
        if (is > 0 && balabanP1->x >= bx && balabanP1->x <= ex) reportFunction(segment1, s, balabanP1);
        if (is > 1 && balabanP2->x >= bx && balabanP2->x <= ex) reportFunction(segment1, s, balabanP2);
        if (is == 0)
            break;
        h++;
    }
    
    int l=i-1;
    while (l >= 0) {
        segment1 = vector_get(staircase, l);
        is = getIntersections(segment1, s, balabanP1, balabanP2);
        if (is > 0 && balabanP1->x >= bx && balabanP1->x <= ex) reportFunction(segment1, s, balabanP1);
        if (is > 1 && balabanP2->x >= bx && balabanP2->x <= ex) reportFunction(segment1, s, balabanP2);
        if (is == 0)
            break;
        l--;
    }
    
}

// Find intersections in given staircase (unsorted)
void findIntersectionsUnsorted(vector *staircase, vector *segments, float bx, float ex) {
    int location;
#if BALABAN_DEBUG
    printf("FIUS # : %i %i\n", vector_count(segments), vector_count(staircase));
#endif
    for (int i=0 ; i<vector_count(segments) ; i++) {
        segment *segment = vector_get(segments, i);
        location = balabanGetLocation(staircase, bx, ex, segment);
#if BALABAN_DEBUG
        printf("FIUS : %f %f %i\n", bx, ex, location);
#endif
        findIntersectionsSegmentStaircase(staircase, bx, ex, location, segment);
    }
}

// Find intersections in given staircase (sorted)
void findIntersectionsSorted(vector *staircase, vector *segments, float bx, float ex, float x) {
    
    if (vector_count(segments) == 0)
        return;
    
    int j=0;
    segment *last = vector_get(segments, 0);
    for (int i=0 ; i<vector_count(staircase) ; i++) {
        segment *segment = vector_get(staircase, i);
        while (getYForX(segment, x) >= getYForX(last, x)) {
            findIntersectionsSegmentStaircase(staircase, bx, ex, i, last);
            last = vector_get(segments, ++j);
            if (last == NULL)
                return;
        }
    }
    
    while (last != NULL) {
        findIntersectionsSegmentStaircase(staircase, bx, ex, vector_count(staircase), last);
        last = vector_get(segments, ++j);
    }
    
}

// Check if segments intersect within strip
char balabanIsIntersecting(segment *s1, segment *s2, float bx, float ex) {
    if (s1 != NULL && s2 != NULL) {
        int is = getIntersections(s1, s2, balabanP1, balabanP2);
        if ((is > 0 && balabanP1->x >= bx && balabanP1->x <= ex)
            || (is > 1 && balabanP2->x >= bx && balabanP2->x <= ex))
            return 1;
    }
    return 0;
}

#pragma mark Bentley-Ottmann

// http://geomalgorithms.com/a09-_intersect-3.html

#pragma mark Utility Methods

// Get y value for x value for segment
//  assumes (segment.x1 < segment.x2)
float getYForX(segment *segment, float x) {
    if (segment->is_line) {
        if (x >= segment->x1 && x <= segment->x2)
            return segment->y1 +
                (segment->y2-segment->y1)
                / (segment->x2-segment->x1)
                * (x - segment->x1); // rico * dx
    }
    else if (x >= segment->x1-RHO && x <= segment->x2+RHO) {
        float dy = sqrtf(RHO*RHO - (segment->x1-x)*(segment->x1-x));
        return segment->y1 + (segment->is_top ? dy : -dy);
    }
    return 0; // x outside segment interval
}

// QuickSort for transition array
void quicksort(end_point *transitions[], int low, int high) {
    
    // Preprocessing checks
    if (transitions == NULL)
        return;
    if (low >= high)
        return;
    
    // Pivot
    int middle = low + (high - low) / 2;
    end_point *pivot = transitions[middle];
    
    // Make left < pivot and right > pivot
    int i = low, j = high;
    while (i <= j) {
        while (transitions[i]->x < pivot->x) i++;
        while (transitions[j]->x > pivot->x) j--;
        if (i <= j) { // Swap pointers
            end_point *temp = transitions[i];
            transitions[i] = transitions[j];
            transitions[j] = temp;
            i++;
            j--;
        }
    }
    
    // Recursively sort two sub parts
    if (low < j) quicksort(transitions, low, j);
    if (high > i) quicksort(transitions, i, high);
    
}