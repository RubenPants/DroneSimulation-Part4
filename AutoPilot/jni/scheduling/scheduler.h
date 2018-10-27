/**
 * Scheduling algorithms.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include "scheduler_tools.h"
#include "trajectory.h"

#ifndef SCHEDULER_H
#define SCHEDULER_H

#define HOVER_POINT_ALTITUDE 40.0

#endif /* SCHEDULER_H */

/**
 * Set-up functions.
 */
void setupPlanning();
void defineAirportParameters(float length, float width);
void defineAirport(float centerX, float centerZ, float centerToRunway0X, float centerToRunway0Z);
void cleanupPlanning();

/**
 * Scheduling functions.
 */
trajectory *getTrajectory(int source, char sourceGate, int destination, char destinationGate);

/**
 * Misc functions.
 */
int getNbIntersections();
int resetNbIntersections();
void reportIntersection(trajectory_segment *segment1, trajectory_segment *segment2, point_2Df *p);