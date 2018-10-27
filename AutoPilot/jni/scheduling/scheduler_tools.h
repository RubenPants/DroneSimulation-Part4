/**
 * Tools for scheduling engine.
 *
 * @version Team Saffier
 * @version 1.0
 */

#ifndef _SCHEDULER_TOOLS_
#define _SCHEDULER_TOOLS_

#define TESTING_PRINT 0
#define TESTING_ENABLED 1

#define MAX_AIRPORTS 20

#ifndef MIN
#define MIN(a,b) (((a)<(b))?(a):(b))
#endif

#ifndef MAX
#define MAX(a,b) (((a)>(b))?(a):(b))
#endif

// Vector operations
#define CROSS(x1,y1,x2,y2) x1*y2-y1*x2;
#define COUNTER_CLOCKWISE_ANGLE(x1,y1,x2,y2,sol) {sol=atan2f(x1*y2-y1*x2,x1*x2+y1*y2);if (sol<0) sol+=2*M_PI;}
#define CLOCKWISE_ANGLE(x1,y1,x2,y2,sol) {sol=atan2f(x1*y2-y1*x2,x1*x2+y1*y2);if (sol<0) sol+=2*M_PI;sol=2*M_PI-sol;}

#define RHO 400.0

typedef struct point2df_struct {
    float x;
    float y;
} point_2Df;

typedef struct line2df_struct {
    float x1;
    float y1;
    float x2;
    float y2;
} line_2Df;

typedef struct point3df_struct {
    float x;
    float y;
    float z;
} point_3Df;

typedef struct airport_struct {
    float centerX;
    float centerZ;
    float centerToRunway0X;
    float centerToRunway0Z;
} airport;

#endif /* _SCHEDULER_TOOLS_ */