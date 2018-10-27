/**
 * Vision algorithms.
 *
 * @author Team Safier
 * @version 1.0
 */

#include <jni.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../algorithms/convex.h"
#include "../algorithms/geom.h"
#include "../algorithms/simplify.h"
#include "color.h"
#include "../general.h"
#include "../data/vector.h"

void setupVision() {
    // If something has to be setup beforehand
}

void cleanupVision() {
    // If something has to be setup beforehand
}

/*
 * Macros
 */
#define HSV_THRESHOLD 0.03
#define PRINT_SIDE_POLYGONS 0
#define SIMPLIFY_EPSILON 1.5

#pragma mark Private Methods

int get_line_intersection(int p0_x, int p0_y, int p1_x, int p1_y,
                          int p2_x, int p2_y, int p3_x, int p3_y,
                          float *i_x, float *i_y);
float get_dist_point_to_line(int x1, int y1, int x2, int y2, int x, int y);
float get_dist(int x1, int y1, int x2, int y2);

#pragma mark Implementation

// Cube side structure to be used as linked list
typedef struct cubeside_struct {
    hsv hsv; // HSV of side color
    unsigned int rgb; // Hash key
    int r,g,b;
    point_2d *first_point;
    point_2d *left_branch; // Points wrapping left
    point_2d *right_branch; // Points wrapping right
    int left_length;
    int right_length;
    struct cubeside_struct *next;
} cubeside;

// Cube structure to be used as linked list
typedef struct cube_struct {
    hsv hsv; // HSV of first side associated with this cube
    int r,g,b;
    line_2d *diagonals; // Location of (some of the) edges of this cube on a 2D image
    int diagonal_count;
    rectangle bounding_box; // Bounding box of cube in a 2D image
    struct cube_struct *next;
} cube;

/*
 * Called from Java. An array of *visible* cubes is detected.
 *  Each cube has a unique hue and saturation, but not a unique value.
 *
 * @return An array of cubes with *a* characteristic color (color of one of the sides, which one is undetermined),
 *  bounding box and possibly edges (each with 2 point coordinates, for start - and end point).
 * @note This algorithm applies the Ramer-Douglas-Peucker algorithm on the left and right chain of border points of each detected side.
 */
JNIEXPORT jobjectArray JNICALL locateUnitCubes(JNIEnv *env, jclass class, jbyteArray bytes, jint width, jint height, jboolean useHistory, jboolean hasGround) {
    
    // Get input from Java
    jsize length = (*env)->GetArrayLength(env, bytes);
    jbyte *pixels = (*env)->GetByteArrayElements(env, bytes, 0);
    
    // Initialize hashmap for sides
    //  Currently an array of 256 linked lists is kept for each red color component
    //  When a non-white pixel is found its coordinates are added to a vector to a 'cubeside' structure
    //  The structure to update is that with the same rgb value as the pixel
    //  It is found in the hashmap, by first fetching the linked list corresponding to the red color component of the pixel
    //  This linked list is looped through until a matching cubeside is found ; otherwise, a new one is created
    // For faster access red-black trees could be used, to guarantee logarithmic time
    // Since collisions should be quite (very!) rare this is currently not necessary
    point_2d *point, *otherpoint; // Used when iterating
    cubeside *side; // Used when iterating
    cubeside *sides_map[256] = {NULL}; // Array of pointers to linked lists of cube sides, one pointer for each red color component
    unsigned int nbOfSides = 0; // Sides/colors detected so far
    float huediff, satdiff; // For comparing colors
    
    // Initialization
    // Depending on endianness the rgb values should be shifted right or left (little-endian = to the left)
    //  (If necessary a check could be made)
    int i = 0, row = 0, column = 0, width3 = width*3, cmax = 0, red = 0, green = 0, blue = 0;
    char whites[] = {-1,-1,-1,-1};
    unsigned int rgb, white = (*(unsigned int *)(whites) << 8);
    
    // Loop through all pixels in the input image and keep track of border points
    while (i < length) {
        
        // Get RGB values as unsigned int (= *4* bytes) shifted 8 bits to the right (to mask the last, irrelevant byte)
        rgb = *(unsigned int *)(pixels + i) << 8;
        
        // Check if current pixel is a border point
        if (rgb ^ white) { // Not white (XOR implies 0 if rgb is identical to white)
            
            // Get the value because ground texture pixels should be ignored
            //  Only calculate value or performance may suffer from the full HSV calculation
            //  Could use the convenience method in color.h but that's still gonna take a function call
            // Note that this whole method could be improved upon a tad by fully separating the logic for cases
            //  where there is a ground vs. cases where there isn't one.
            red = (unsigned char)pixels[i];
            green = (unsigned char)pixels[i+1];
            blue = (unsigned char)pixels[i+2];
            cmax = (red > green) ? red : green;
            if (blue > cmax)
                cmax = blue;
            if (cmax < 128) {
                
                if ((column == 0) | (rgb ^ (*(unsigned int *)(pixels + i - 3) << 8))) { // Left point
                    
                    point = malloc(sizeof(point_2d));
                    point->x = column, point->y = row;
                    
                    side = sides_map[(unsigned char)pixels[i]];
                    while (side != NULL && side->rgb ^ rgb)
                        side = side->next;
                    if (side == NULL) { // First time seeing this side, create it and add the first point to it
                        
                        // Ground texture is already filtered out now
                        hsv hsv = rgb_to_hsv((unsigned char)pixels[i], (unsigned char)pixels[i+1], (unsigned char)pixels[i+2]);
                        // printf("||| ok");
                        point->next = NULL; // Denote end of linked list
                        side = malloc(sizeof(cubeside));
                        side->rgb = rgb;
                        side->r = red, side->g = green, side->b = blue;
                        side->hsv = hsv;
                        side->first_point = point, side->left_branch = point, side->right_branch = point;
                        side->next = sides_map[red];
                        side->left_length = 1, side->right_length = 1;
                        sides_map[red] = side;
                        // printf("\n");
                        
                    }
                    else if (side->left_branch->y != row)  { // Side already detected, add point to the left branch
                        point->next = side->left_branch;
                        side->left_branch = point;
                        side->left_length++;
                    }
                    
                }
                else if ((column == width-1) | (rgb ^ (*(unsigned int *)(pixels + i + 3) << 8))) { // Right point
                    
                    point = malloc(sizeof(point_2d));
                    point->x = column, point->y = row;
                    
                    side = sides_map[red];
                    while (side != NULL && side->rgb ^ rgb)
                        side = side->next;
                    if (side != NULL) { // Cannot happen, as first point is always seen as left point first
                        if (side->right_branch->y == row && side->right_length > 1) {
                            point->next = side->right_branch->next;
                            free(side->right_branch);
                        }
                        else
                            point->next = side->right_branch;
                        side->right_branch = point;
                        side->right_length++;
                    }
                    
                }
                
            }
            
        }
        
        // Next pixel
        i += 3;
        if (++column == width) {
            column = 0;
            row++;
            // printf("new row");
        }
        
    }
    
    // Create linked list of cube sides (and apply Ramer-Douglas-Peucker at the same time)
    cubeside *sides = NULL, *lastside = NULL;
    for (int i=0 ; i<256 ; i++) {
        if (sides_map[i] != NULL) {
            
            side = sides_map[i];
            while (side != NULL) {
                
                // Make sure that last detected point is start point of left AND right branch
                if (side->left_branch != side->right_branch) {
                    point = malloc(sizeof(point_2d));
                    if (side->left_branch->y > side->right_branch->y) {
                        point->x = side->left_branch->x, point->y = side->left_branch->y;
                        point->next = side->right_branch;
                        side->right_branch = point;
                        side->right_length++;
                    }
                    else {
                        point->x = side->right_branch->x, point->y = side->right_branch->y;
                        point->next = side->left_branch;
                        side->left_branch = point;
                        side->left_length++;
                    }
                }
                
                // Simplify left and right branch using Ramer-Douglas-Peucker
                simplify(side->left_branch, side->first_point, SIMPLIFY_EPSILON /*side->left_length * 0.02*/);
                simplify(side->right_branch, side->first_point, SIMPLIFY_EPSILON /*side->right_length * 0.02*/);
                
                // Register current side and go to next one
                if (sides == NULL)
                    sides = side;
                else
                    lastside->next = side;
                lastside = side;
                side = side->next;
                
            }
        }
    }
    if (lastside != NULL)
        lastside->next = NULL;
    
    // Print polygons?
#if PRINT_SIDE_POLYGONS
    cubeside *print_sides = sides;
    while (print_sides != NULL) {
        point = print_sides->left_branch;
        while (point != NULL) {
            printf("%d %d\n", point->x, point->y);
            point = point->next;
        }
        printf(" (to right) \n");
        point = print_sides->right_branch;
        while (point != NULL) {
            printf("%d %d\n", point->x, point->y);
            point = point->next;
        }
        print_sides = print_sides->next;
        printf("----\n");
    }
#endif
    
    // Initialize linked list
    cube *cubes = NULL, *lastcube; // Linked list of cubes that have been found
    int cube_count = 0; // Number of cubes
    int nb_diagonal_points = 0;
    point_2d *diagonal_points[5];
    
    // Loop through all detected sides, filter out real diagonals
    lastside = sides;
    line_2d *diagonal, *otherdiagonal;
    point_2d *point2, *otherpoint2;
    while (lastside != NULL) {
        
        // Find matching cube for side, or create one
        lastcube = cubes;
        while (lastcube != NULL) {
            huediff = fabs(lastcube->hsv.h - lastside->hsv.h), satdiff = fabs(lastcube->hsv.s - lastside->hsv.s);
            if ((huediff < HSV_THRESHOLD || 1.0 - huediff < HSV_THRESHOLD)
                && (satdiff < HSV_THRESHOLD || 1.0 - satdiff < HSV_THRESHOLD))
                break;
            else
                lastcube = lastcube->next;
        }
        if (lastcube == NULL) { // Add new cube to linked list of cubes
            lastcube = malloc(sizeof(cube));
            lastcube->hsv = lastside->hsv;
            lastcube->r = lastside->r, lastcube->g = lastside->g, lastcube->b = lastside->b;
            lastcube->diagonals = NULL;
            lastcube->diagonal_count = 0;
            lastcube->bounding_box.min_x = lastcube->bounding_box.max_x = lastside->first_point->x;
            lastcube->bounding_box.min_y = lastcube->bounding_box.max_y = lastside->first_point->y;
            lastcube->next = cubes;
            cubes = lastcube;
            cube_count++;
        }
        
        // Get the first point of the left branch (same as first point of right branch)
        point = lastside->left_branch;
        
        // Update bounding box with this point
        if (point->x < lastcube->bounding_box.min_x)
            lastcube->bounding_box.min_x = point->x;
        else if (point->x > lastcube->bounding_box.max_x)
            lastcube->bounding_box.max_x = point->x;
        if (point->y < lastcube->bounding_box.min_y)
            lastcube->bounding_box.min_y = point->y;
        else if (point->y > lastcube->bounding_box.max_y)
            lastcube->bounding_box.max_y = point->y;
        
        // Array of diagonal points (add last point to this array already)
        nb_diagonal_points = 1; // Last point of either branch is added
        diagonal_points[0] = point;
        
        // Loop through edges on left/right branch
        int right_shift = 0;
        for (i=0 ; i<2 ;i++) {
            
            // Left/right branch
            if (i == 1)
                point = lastside->right_branch;
            
            // Get 4 points of branches. If there are more than 4 points in total, only the bounding box is calculated.
            while (point->next != NULL && !(i==1 && point->next == lastside->first_point)) {
                
                point = point->next;
                
                // Update bounding box
                if (point->x < lastcube->bounding_box.min_x)
                    lastcube->bounding_box.min_x = point->x;
                else if (point->x > lastcube->bounding_box.max_x)
                    lastcube->bounding_box.max_x = point->x;
                if (point->y < lastcube->bounding_box.min_y)
                    lastcube->bounding_box.min_y = point->y;
                else if (point->y > lastcube->bounding_box.max_y)
                    lastcube->bounding_box.max_y = point->y;
                
                // Add point to list of diagonal points
                if (nb_diagonal_points < 5) {
                    diagonal_points[(i==0 ? nb_diagonal_points : 3-right_shift++)] = point; // Left branch added rightwards, right branch added leftwards
                    nb_diagonal_points++;
                }
                
            }
            
        }
        
        // If exactly 4 points are found, we got two diagonals and should check for their validity by checking for intersections with frame and other cubes
        // printf("POINTS NB = %i\n", nb_diagonal_points);
        if (nb_diagonal_points == 4) {
            
            for (int i=0 ; i<2 ; i++) {
                
                // Get diagonal endpoints
                point = diagonal_points[i];
                otherpoint = diagonal_points[i+2];
                // printf("%i %i -> %i %i \n", point->x, point->y, otherpoint->x, otherpoint->y);
                
                // Make sure endpoints aren't part of the frame
                if ((point->x != 0 && point->x != width-1 && point->y != 0 && point->y != height-1)
                    && (otherpoint->x != 0 && otherpoint->x != width-1 && otherpoint->y != 0 && otherpoint->y != height-1)) {
                    
                    // Compare edge with every other side's edges
                    side = sides;
                    int ok = 1;
                    while (side != NULL && ok) {
                        
                        // Make sure cube side is other one, and that it's not part of the same cube
                        if (side != lastside) {
                            
                            huediff = fabs(side->hsv.h - lastside->hsv.h), satdiff = fabs(side->hsv.s - lastside->hsv.s);
                            
                            if (!((huediff < HSV_THRESHOLD || 1.0 - huediff < HSV_THRESHOLD)
                                  && (satdiff < HSV_THRESHOLD || 1.0 - satdiff < HSV_THRESHOLD))) {
                                
                                // Check edges of the side with current diagonal (both left & right branch)
                                for (int j=0 ; j<2 ;j++) {
                                    
                                    // Left/right branch
                                    if (j == 0)
                                        point2 = side->left_branch;
                                    else
                                        point2 = side->right_branch;
                                    
                                    // Check each edge
                                    otherpoint2 = point2->next;
                                    while (otherpoint2 != NULL && ok) {
                                        
                                        // Check for intersection
                                        if (!get_line_intersection(point->x, point->y, otherpoint->x, otherpoint->y,
                                                                   point2->x, point2->y, otherpoint2->x, otherpoint2->y,
                                                                   NULL, NULL)) {
                                            
                                            
                                            
                                            // Make sure edge isn't too close to current diagonal
                                            if (get_dist_point_to_line(point->x, point->y, otherpoint->x, otherpoint->y, point2->x, point2->y) < 2
                                                || get_dist_point_to_line(point->x, point->y, otherpoint->x, otherpoint->y, otherpoint2->x, otherpoint2->y) < 2
                                                || get_dist_point_to_line(point2->x, point2->y, otherpoint2->x, otherpoint2->y, point->x, point->y) < 2
                                                || get_dist_point_to_line(point2->x, point2->y, otherpoint2->x, otherpoint2->y, otherpoint->x, otherpoint->y) < 2) {
                                                ok = 0;
                                            }
                                            
                                        }
                                        else {
                                            ok = 0;
                                        }
                                        
                                        // Next edge
                                        point2 = otherpoint2;
                                        otherpoint2 = otherpoint2->next;
                                        
                                    }
                                    
                                }
                                
                            }
                            
                        }
                        
                        side = side->next;
                        
                    }
                    
                    // Add diagonal if all checks have passed
                    if (ok) {
                        diagonal = malloc(sizeof(line_2d));
                        diagonal->x1 = point->x, diagonal->y1 = point->y, diagonal->x2 = otherpoint->x, diagonal->y2 = otherpoint->y;
                        if (lastcube->diagonals == NULL) {
                            diagonal->next = NULL;
                            lastcube->diagonals = diagonal;
                            lastcube->diagonal_count++;
                        }
                        else {
                            diagonal->next = lastcube->diagonals;
                            lastcube->diagonals = diagonal;
                            lastcube->diagonal_count++;
                        }
                    }
                    
                }
                
            }
            
        }
        
        // Next side
        side = lastside;
        lastside = lastside->next;
        
    }
    
    // Release sides memory
    side = sides;
    while (side != NULL) {
        lastside = side;
        side = side->next;
        free(lastside);
        free(lastside->first_point);
    }
    
    // Loop through cubes and transmute them to a 2D array to be returned to Java
    // Based on https://stackoverflow.com/questions/43865462/return-multidimensional-array-in-jni
    jclass int_class = (*env)->FindClass(env, "[I");
    jintArray array_2d = (*env)->NewIntArray(env, cube_count);
    jobjectArray outer = (*env)->NewObjectArray(env, cube_count, int_class, array_2d);
    i = 0; // Index of current cube
    lastcube = cubes;
    line_2d *olddiagonal;
    while (lastcube != NULL) {
        
        // Read rgb, bounding box and diagonals into array
        if (lastcube->diagonal_count % 2 != 0)
            lastcube->diagonal_count = 0;
        int size = lastcube->diagonal_count*4 + 7; // 7 because of rgb (3 integers) + bounding box (4 integers)
        jint array[size];
        array[0] = lastcube->r, array[1] = lastcube->g, array[2] = lastcube->b;
        array[3] = lastcube->bounding_box.min_x, array[4] = lastcube->bounding_box.min_y, array[5] = lastcube->bounding_box.max_x, array[6] = lastcube->bounding_box.max_y;
        diagonal = lastcube->diagonals;
        for (int j=0 ; j<lastcube->diagonal_count ; j++) { // j is index of current diagonal
            array[7 + j*4] = diagonal->x1;
            array[8 + j*4] = diagonal->y1;
            array[9 + j*4] = diagonal->x2;
            array[10 + j*4] = diagonal->y2;
            olddiagonal = diagonal;
            diagonal = diagonal->next;
            free(olddiagonal);
        }
        
        // Put integer array into jintArray and put that one in the outer array
        jintArray inner = (*env)->NewIntArray(env, size);
        (*env)->SetIntArrayRegion(env, inner, 0, size, array);
        (*env)->SetObjectArrayElement(env, outer, i++, inner);
        (*env)->DeleteLocalRef(env, inner);
        
        // Release memory for cube and go to the next one
        cubes = lastcube;
        lastcube = lastcube->next;
        free(cubes);
        
    }
    
    (*env)->ReleaseByteArrayElements(env, bytes, pixels, 0);
    
    return outer;
    
}

/**
 * Check if two line segments intersect.
 *  The intersection can be put in the given pointers to floats (set to NULL if not wanted).
 */
int get_line_intersection(int p0_x, int p0_y, int p1_x, int p1_y,
                          int p2_x, int p2_y, int p3_x, int p3_y,
                          float *i_x, float *i_y) {
    
    float s02_x, s02_y, s10_x, s10_y, s32_x, s32_y, s_numer, t_numer, denom, t;
    s10_x = p1_x - p0_x;
    s10_y = p1_y - p0_y;
    s32_x = p3_x - p2_x;
    s32_y = p3_y - p2_y;
    
    denom = s10_x * s32_y - s32_x * s10_y;
    if (denom == 0)
        return 0; // Collinear
    int denomPositive = denom > 0;
    
    s02_x = p0_x - p2_x;
    s02_y = p0_y - p2_y;
    s_numer = s10_x * s02_y - s10_y * s02_x;
    if ((s_numer < 0) == denomPositive)
        return 0; // No collision
    
    t_numer = s32_x * s02_y - s32_y * s02_x;
    if ((t_numer < 0) == denomPositive)
        return 0; // No collision
    
    if (((s_numer > denom) == denomPositive) || ((t_numer > denom) == denomPositive))
        return 0; // No collision
    
    // Collision detected
    t = t_numer / denom;
    if (i_x != NULL)
        *i_x = p0_x + (t * s10_x);
    if (i_y != NULL)
        *i_y = p0_y + (t * s10_y);
    
    return 1;
    
}

/**
 * Get the distance between the given line segment and the given point.
 */
float get_dist_point_to_line(int x1, int y1, int x2, int y2, int x, int y) {
    
    float dist = get_dist(x1, y1, x2, y2);
    if (dist == 0)
        return get_dist(x, y, x1, y1);
    float t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / dist;
    t = fmax(0, fmin(1, t));
    
    return sqrt(get_dist(x, y, (x1 + t * (x2 - x1)), (y1 + t * (y2 - y1))));
    
}

/**
 * Helper function for point-line-segment distance calculation.
 */
float get_dist(int x1, int y1, int x2, int y2) {
    return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
}