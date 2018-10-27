/**
 * Scheduling algorithms.
 *
 * @author Team Saffier
 * @version 1.0
 */

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "scheduler.h"
#include "trajectory.h"
#include "intersection_algorithms.h"

void calculateTrajectories(int airports);
void calculateTrajectory(int source, char sourceGate, int destination, char destinationGate);
point_3Df takeoffPointForAirport(int airport, int gate, float distance);
point_3Df landingPointForAirport(int airport, int gate, float distance);

point_3Df centerFirst, centerSecond;
point_3Df keypointFirst, keypointSecond;
void loadKeyPoints(trajectory *trajectory);

#pragma mark - Airports

    // Airports
    float airportLength, airportWidth;
    int nbOfAirports = 0;
    int airportsCapacity = MAX_AIRPORTS;
    airport *airports = NULL; // Airports are defined by

    // Define airport parameters
    void defineAirportParameters(float length, float width) {
        airportLength = length;
        airportWidth = width;
    }

    // Defines an airport an immediately calculates all relevant trajectories to and from other (known) airports.
    void defineAirport(float centerX, float centerZ, float centerToRunway0X, float centerToRunway0Z) {
        if (nbOfAirports == MAX_AIRPORTS) {
            printf("max # airporst reached ...");
            return;
        }
        if (++nbOfAirports > airportsCapacity) { // Resize array
            airportsCapacity = MAX(airportsCapacity * 2, nbOfAirports);
            airports = realloc(airports, sizeof(airport) * airportsCapacity);
        }
        airport newAirport;
        newAirport.centerX = centerX;
        newAirport.centerZ = centerZ;
        newAirport.centerToRunway0X = centerToRunway0X;
        newAirport.centerToRunway0Z = centerToRunway0Z;
        airports[nbOfAirports-1] = newAirport;
    }

    // Get the airport at the given index (access directly for speed-up, is here for reference)
    airport *getAirport(int index) {
        if (index >= nbOfAirports)
            return NULL;
        return &airports[index];
    }

#pragma mark - Trajectories

    // Registers the trajectories ; row = airport for departure ; column = airport for arrival.
    // Indices : SOURCE - DESTINATION
    trajectory trajectories[MAX_AIRPORTS*2][MAX_AIRPORTS*2];
    trajectory_segment *segments;
    int segment_count = 0;

    // Calculates all trajectories from/to the given airport.
    // Assumes
    //      (1) trajectories not calculated yet
    //      (2) airport is last one that was defined (parameter not necessary :3)
    void calculateTrajectories(int airport) {
        // Calculate take-off and landing checkpoints and determine Dubins in between
        // Calculate trajectory from and to airport
        int source = airport, destination;
        for (destination = 0 ; destination < airport ; destination++) {
            
            calculateTrajectory(source, 0, destination, 0);
            calculateTrajectory(source, 1, destination, 0);
            calculateTrajectory(source, 0, destination, 1);
            calculateTrajectory(source, 1, destination, 1);
            
            calculateTrajectory(destination, 0, source, 0);
            calculateTrajectory(destination, 1, source, 0);
            calculateTrajectory(destination, 0, source, 1);
            calculateTrajectory(destination, 1, source, 1);
            
        }
    }

    // Calculate trajectory from source to destination
    void calculateTrajectory(int source, char sourceGate, int destination, char destinationGate) {
        
        // Calculate trajectory
        trajectory *newTrajectory = (trajectory *)malloc(sizeof(trajectory));
        newTrajectory->path = NULL;
        newTrajectory->takeoffPoint = takeoffPointForAirport(source, sourceGate, 900.0f);
        newTrajectory->landingPoint1 = landingPointForAirport(destination, destinationGate, 1500.0f);
        newTrajectory->landingPoint2 = landingPointForAirport(destination, destinationGate, 900.0f);
        newTrajectory->landingPoint3 = landingPointForAirport(destination, destinationGate, 600.0f);
        double q0[] = {(double)newTrajectory->takeoffPoint.x,(double)newTrajectory->takeoffPoint.z,0.0};
        double q1[] = {(double)newTrajectory->landingPoint1.x,(double)newTrajectory->landingPoint1.z,0.0};
        dubins_shortest_path(newTrajectory->path, q0, q1);
        loadKeyPoints(newTrajectory);
        newTrajectory->keyPointFirst.x = keypointFirst.x, newTrajectory->keyPointFirst.y = keypointFirst.z;
        newTrajectory->keyPointSecond.x = keypointSecond.x, newTrajectory->keyPointSecond.y = keypointSecond.z;
        newTrajectory->centerFirst.x = centerFirst.x, newTrajectory->centerFirst.y = centerFirst.z;
        newTrajectory->centerSecond.x = centerSecond.x, newTrajectory->centerSecond.y = centerSecond.z;
        
        // Calculate the segments of the path
        // Start end angles not important for arcs, use check points
        segment s1, s2, s3, s4, s5;
        s1.x1 = centerFirst.x, s1.y1 = centerFirst.z, s1.x2 = keypointFirst.x, s1.y2 = keypointFirst.z;
        s2.x1 = centerFirst.x, s2.y1 = centerFirst.z, s2.x2 = keypointFirst.x, s2.y2 = keypointFirst.z;
        if (keypointFirst.x < keypointSecond.x)
            s3.x1 = keypointFirst.x, s3.y1 = keypointFirst.z, s3.x2 = keypointSecond.x, s1.y2 = keypointSecond.z;
        else
            s3.x1 = keypointSecond.x, s3.y1 = keypointSecond.z, s3.x2 = keypointFirst.x, s1.y2 = keypointFirst.z;
        s4.x1 = centerSecond.x, s4.y1 = centerSecond.z, s4.x2 = keypointSecond.x, s4.y2 = keypointSecond.z;
        s5.x1 = centerSecond.x, s5.y1 = centerSecond.z, s5.x2 = keypointSecond.x, s5.y2 = keypointSecond.z;
        
        // Save the trajectory segments for determining collisions later on
        trajectory_segment arc1up, arc1down, line, arc2up, arc2down;
        arc1up.segment = s1, arc1up.trajectory = newTrajectory, arc1up.type = 0;
        arc1down.segment = s2, arc1down.trajectory = newTrajectory, arc1down.type = 1;
        line.segment = s3, line.trajectory = newTrajectory, line.type = 2;
        arc2up.segment = s4, arc2up.trajectory = newTrajectory, arc2up.type = 3;
        arc2down.segment = s5, arc2down.trajectory = newTrajectory, arc2down.type = 4;
        segments[segment_count] = arc1up;
        segments[segment_count+1] = arc1down;
        segments[segment_count+2] = line;
        segments[segment_count+3] = arc2up;
        segments[segment_count+4] = arc2down;
        segment_count += 5;
        
        // Save trajectory
        trajectories[source*2+sourceGate][destination*2+destinationGate] = *newTrajectory;
        
    }

    // Get trajectory from source to destination
    trajectory *getTrajectory(int source, char sourceGate, int destination, char destinationGate) {
        return &trajectories[source*2+sourceGate][destination*2+destinationGate];
        /*
         // If you want to do checks ...
        if (source*2+sourceGate < MAX_AIRPORTS && destination*2+destinationGate < MAX_AIRPORTS)
            return &trajectories[source*2+sourceGate][destination*2+destinationGate];
        return NULL;
         */
    }

#pragma mark - Intersections

    void reportIntersection(trajectory_segment *segment1, trajectory_segment *segment2, point_2Df *p);

    // Trajectory intersections
    void calculateIntersections() {
        // setReportFunction(reportIntersection);
        // reportIntersections(segments, segment_count);
        free(segments);
    }

    // t = collided trajectory, d = colliding trajectory, ip = intersection point, par = param
#if TESTING_ENABLED
    #define ADD_INTERSECTION(t, ip, d, par) printf("-> intersection at : (%f %f)\n", (ip).x, (ip).y);
#else
    #define ADD_INTERSECTION(t, ip, d, par) \
                { \
                trajectory_collision *collision = malloc(sizeof(trajectory_collision)); \
                collision->trajectory = d; \
                collision->point = ip; \
                collision->param = par; \
                int idx = d->source*2 + d->sourceGate; \
                if (t->collisions[idx] == NULL) t->collisions[idx] = collision; \
                else t->collisions[idx]->next = collision;\
                }
#endif

    // Note intersection for given segments of trajectories
    // Uses https://stackoverflow.com/questions/13640931/how-to-determine-if-a-vector-is-between-two-other-vectors
    //  and https://stackoverflow.com/questions/14066933/direct-way-of-computing-clockwise-angle-between-2-vectors
    void reportIntersectionInternal(trajectory_segment *segment1, trajectory_segment *segment2, point_2Df *p) {
        
#if TESTING_ENABLED
    #if TESTING_PRINT
        printf("-> intersection at : (%f %f)\n", p->x, p->y);
    #endif
#else
        
        float angle, param;
        
        // First segment = intersected, Second segment = intersecting
        if (segment1->type < 2
            && p->y >= (segment1->type == 0 ? segment1->segment.y1 : -segment1->segment.y1)) { // First arc
            float x1 = (segment1->trajectory->takeoffPoint.x - segment1->segment.x1);
            float y1 = (segment1->trajectory->takeoffPoint.y - segment1->segment.y1);
            float x2 = (p->x - segment1->segment.x1);
            float y2 = (p->y - segment1->segment.y1);
            if (segment1->trajectory->path->type > 1) /* RSC */
                CLOCKWISE_ANGLE(x1, y1, x2, y2, angle)
                else
                    COUNTER_CLOCKWISE_ANGLE(x1, y1, x2, y2, angle);
            if (angle < segment1->trajectory->path->param[0])
                ADD_INTERSECTION(segment1->trajectory, *p, segment2->trajectory, angle*RHO);
        }
        else {
            param = segment1->trajectory->path->param[0] * RHO;
            if (segment1->type == 2) { // Line segment
                param += sqrtf((p->x-segment1->segment.x1)*(p->x-segment1->segment.x1)
                               + (p->y-segment1->segment.y1)*(p->y-segment1->segment.y1));
                ADD_INTERSECTION(segment1->trajectory, *p, segment2->trajectory, param);
            }
            else if (p->y >= (segment1->type == 3 ? segment1->segment.y1 : -segment1->segment.y1)) { // Second arc
                param += segment1->trajectory->path->param[1] * RHO;
                float x1 = (segment1->trajectory->landingPoint1.x - segment1->segment.x1);
                float y1 = (segment1->trajectory->landingPoint1.y - segment1->segment.y1);
                float x2 = (p->x - segment1->segment.x1);
                float y2 = (p->y - segment1->segment.y1);
                if (segment1->trajectory->path->type % 2 != 0) /* CSR */
                    CLOCKWISE_ANGLE(x1, y1, x2, y2, angle)
                    else
                        COUNTER_CLOCKWISE_ANGLE(x1, y1, x2, y2, angle);
                if (angle < segment1->trajectory->path->param[2])
                    ADD_INTERSECTION(segment1->trajectory, *p, segment2->trajectory, param+angle*RHO);
            }
        }
        
#endif
        
    }

    // Calls internal method for each segment
    int count = 0;
    void reportIntersection(trajectory_segment *segment1, trajectory_segment *segment2, point_2Df *p) {
        reportIntersectionInternal(segment1, segment2, p);
        reportIntersectionInternal(segment2, segment1, p);
        count++;
    }

    // Reset # intersections
    int resetNbIntersections() {
        return count;
    }

    // Get # intersections
    int getNbIntersections() {
        return count;
    }

#pragma mark - Scheduling

    // Registers the trajectories for drones that are flying.
    active_trajectory *activeTrajectories;

#pragma mark - Setup

    // Set up planning engine
    void setupPlanning() {
        
        // Airports
        airports = (airport *)malloc(sizeof(airport) * MAX_AIRPORTS);
        nbOfAirports = 0;
        
        // Segments
        segments = (trajectory_segment *)malloc(sizeof(trajectory_segment) * (MAX_AIRPORTS*2*(MAX_AIRPORTS*2-2)*5));
        
    }

    // Cleanup planning engine
    void cleanupPlanning() {
        free(airports);
        // No need to free segments (already done) or trajectories
    }

#pragma mark - Utility Methods

    // Calculate point hovering gate 0/1 for the given airport
    // https://gamedev.stackexchange.com/questions/70075/how-can-i-find-the-perpendicular-to-a-2d-vector

    point_3Df hoverPointGate0(int airport) {
        point_3Df hoverPoint;
        hoverPoint.x = airports[airport].centerX - airports[airport].centerToRunway0Z;
        hoverPoint.y = HOVER_POINT_ALTITUDE;
        hoverPoint.z = airports[airport].centerZ + airports[airport].centerToRunway0X;
        return hoverPoint;
    }

    point_3Df hoverPointGate1(int airport) {
        point_3Df hoverPoint;
        hoverPoint.x = airports[airport].centerX + airports[airport].centerToRunway0Z;
        hoverPoint.y = HOVER_POINT_ALTITUDE;
        hoverPoint.z = airports[airport].centerZ - airports[airport].centerToRunway0X;
        return hoverPoint;
    }

    point_3Df hoverPoint(int airport, char gate) {
        return (gate == 0 ? hoverPointGate0(airport) : hoverPointGate1(airport));
    }

    point_3Df takeoffPointForAirport(int airport, int gate, float distance) {
        point_3Df point;
        return point;
    }

    point_3Df landingPointForAirport(int airport, int gate, float distance) {
        point_3Df point;
        return point;
    }

    // Calculation of key points on trajectory
    void loadKeyPoints(trajectory *trajectory) {
        
        point_3Df startConfiguration = trajectory->takeoffPoint;
        point_3Df endConfiguration = trajectory->landingPoint1;
        
        // Calculate circle centers
        float sigmaFirst = trajectory->path->startTheta + (trajectory->path->type > 1 /* First direction == R */ ? (-M_PI/2) : M_PI/2);
        float sigmaSecond = trajectory->path->endTheta + (trajectory->path->type % 2 != 0 /* Second direction == R */ ? (-M_PI/2) : M_PI/2);
        centerFirst.x = startConfiguration.x - RHO * sin(sigmaFirst);
        centerFirst.z = startConfiguration.z - RHO * cos(sigmaFirst);
        centerSecond.x = endConfiguration.x - RHO * sin(sigmaSecond);
        centerSecond.z = endConfiguration.z - RHO * cos(sigmaSecond);
        
        // Calculate first circle key point
        double deltaAngleFirst = (trajectory->path->type > 1 /* First direction == R */ ? trajectory->path->param[0] : -trajectory->path->param[0]);
        double centerTargetAngleFirst = atan2(centerFirst.x - startConfiguration.x, centerFirst.z - startConfiguration.z);
        double totalAngleFirst = centerTargetAngleFirst + (trajectory->path->type > 1 /* First direction == R */ ? (-deltaAngleFirst) : deltaAngleFirst);
        keypointFirst.x = centerFirst.x - RHO * sin(totalAngleFirst);
        keypointFirst.z = centerFirst.z - RHO * cos(totalAngleFirst);
        
        // Calculate second circle key point
        double deltaAngleSecond = (trajectory->path->type % 2 != 0 /* Second direction == R */ ? trajectory->path->param[2] : -trajectory->path->param[2]);
        double centerTargetAngleSecond = atan2(centerSecond.x - endConfiguration.x, centerSecond.z - endConfiguration.z);
        double totalAngleSecond = centerTargetAngleSecond + (trajectory->path->type % 2 != 0 /* Second direction == R */ ? deltaAngleSecond : (-deltaAngleSecond));
        keypointSecond.x = centerSecond.x - RHO * sin(totalAngleSecond);
        keypointSecond.z = centerSecond.z - RHO * cos(totalAngleSecond);
        
    }