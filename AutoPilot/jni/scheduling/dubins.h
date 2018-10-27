/**
 * Dubins path calculation.
 *
 * @author Team Saffier
 * @version 1.0
 * @note Adapted from https://github.com/AndrewWalker/Dubins-Curves
 */

#include "scheduler_tools.h"

#ifndef DUBINS_H
#define DUBINS_H

typedef enum {
    LSL = 0,
    LSR = 1,
    RSL = 2,
    RSR = 3/*,
    RLR = 4,
    LRL = 5*/
} DubinsPathType;

typedef struct {
    double startTheta, endTheta; // Start/End configuration headings/angles
    double param[3]; // Length of segments (C, S & C in CSC)
    DubinsPathType type; // Type of the path
} DubinsPath;

#define EDUBOK        (0)   /* No error */
#define EDUBCOCONFIGS (1)   /* Colocated configurations */
#define EDUBPARAM     (2)   /* Path parameterisitation error */
#define EDUBBADRHO    (3)   /* the rho value is invalid */
#define EDUBNOPATH    (4)   /* no connection between configurations with this word */

/**
 * Generate a path from an initial configuration to
 * a target configuration, with a specified maximum turning
 * radii
 *
 * A configuration is (x, y, theta), where theta is in radians, with zero
 * along the line x = 0, and counter-clockwise is positive
 *
 * @param path  - the resultant path
 * @param q0    - a configuration specified as an array of x, y, theta
 * @param q1    - a configuration specified as an array of x, y, theta
 * @return      - non-zero on error
 */
int dubins_shortest_path(DubinsPath *path, double q0[3], double q1[3]);

/**
 * Generate a path with a specified word from an initial configuration to
 * a target configuration, with a specified turning radius 
 *
 * @param path     - the resultant path
 * @param q0       - a configuration specified as an array of x, y, theta
 * @param q1       - a configuration specified as an array of x, y, theta
 * @param pathType - the specific path type to use
 * @return         - non-zero on error
 */
int dubins_path(DubinsPath *path, double q0[3], double q1[3], DubinsPathType pathType);

/**
 * Calculate the length of an initialised path
 *
 * @param path - the path to find the length of
 */
double dubins_path_length(DubinsPath *path);

/**
 * Extract an integer that represents which path type was used
 *
 * @param path    - an initialised path
 * @return        - one of LSL, LSR, RSL, RSR, RLR or LRL 
 */
DubinsPathType dubins_path_type(DubinsPath *path);

#endif /* DUBINS_H */