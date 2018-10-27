/**
 * Trajectories.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include "scheduler_tools.h"
#include "dubins.h"
#include "intersection.h"

#ifndef TRAJECTORY_H
#define TRAJECTORY_H

typedef struct trajectory_collision_struct trajectory_collision;

typedef struct trajectory_struct {
    uint8_t source, sourceGate, destination, destinationGate;
    point_3Df takeoffPoint; // Takeoff point (start configuration of Dubins path)
    point_3Df landingPoint1, landingPoint2, landingPoint3; // Landing points
    point_2Df centerFirst, centerSecond; // Centers of circles for CSC
    point_2Df keyPointFirst, keyPointSecond; // Tangents of circles for CSC
    DubinsPath *path; // Dubins path
    trajectory_collision *collisions[MAX_AIRPORTS*2]; // Index gives source airport for colliding trajectory
} trajectory;

typedef struct trajectory_segment_struct { // Represents a segment of a trajectory
    segment segment;
    trajectory *trajectory;
    short type; // C/C-S-C/C = 0/1-2-3/4 (C/C = upper and lower arc)
} trajectory_segment;

typedef struct trajectory_collision_struct { // Represents a collision with a trajectory
    point_2Df point;
    float param; // Where in the path that the collision would occur
    trajectory *trajectory;
    trajectory_collision *next; // Linked list
} trajectory_collision;

typedef struct active_trajectory_struct {
    double altitude; // Altitude of trajectory
    trajectory *trajectory; // Reference to trajectory
} active_trajectory;

#endif /* TRAJECTORY_H */