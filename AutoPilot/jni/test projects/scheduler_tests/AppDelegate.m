//
//  AppDelegate.m
//  scheduler_tests
//
//  Created by Bruno Vandekerkhove on 25/04/18.
//  Copyright (c) 2018 Bruno Vandekerkhove. All rights reserved.
//

#include <glpk.h>

#import "AppDelegate.h"

#import "dubins.h"
#import "scheduler.h"
#import "intersection.h"
#import "intersection_algorithms.h"
#import "vector.h"

#define LOO_MEASURE_TIME(__message) \
for (CFAbsoluteTime startTime##__LINE__ = CFAbsoluteTimeGetCurrent(), endTime##__LINE__ = 0.0; endTime##__LINE__ == 0.0; \
printf("--- '%s' took %.6fs\n", (__message), (endTime##__LINE__ = CFAbsoluteTimeGetCurrent()) - startTime##__LINE__))

int printConfiguration(double q[3], double x, void* user_data) {
    printf("%f, %f, %f, %f\n", q[0], q[1], q[2], x);
    return 0;
}

float float_rand(float min, float max) {
    return (float)((double)rand()/(double)(RAND_MAX/10000.0));
    //float scale = rand() / (float) RAND_MAX; /* [0, 1.0] */
    //return min + scale * ( max - min );      /* [min, max] */
}

@interface AppDelegate ()

@property (weak) IBOutlet NSWindow *window;
@end

@implementation AppDelegate

int testGLPK() {
    /* declare variables */
    glp_prob *lp;
    int ia[1+4], ja[1+4];
    double ar[1+4], z, x1, x2;
    /* create problem */
    lp = glp_create_prob();
    glp_set_prob_name(lp, "short");
    glp_set_obj_dir(lp, GLP_MIN);
    /* fill problem */
    glp_add_rows(lp, 2);
    
    glp_set_row_name(lp, 1, "p");
    glp_set_row_bnds(lp, 1, GLP_UP, 0.0, 1.0);
    glp_set_row_name(lp, 2, "q");
    glp_set_row_bnds(lp, 2, GLP_UP, 0.0, 2.0);
    glp_add_cols(lp, 2);
    glp_set_col_name(lp, 1, "x1");
    glp_set_col_bnds(lp, 1, GLP_LO, 0.0, 0.0);
    glp_set_obj_coef(lp, 1, 0.6);
    glp_set_col_name(lp, 2, "x2");
    glp_set_col_bnds(lp, 2, GLP_LO, 0.0, 0.0);
    glp_set_obj_coef(lp, 2, 0.5);
    ia[1] = 1, ja[1] = 1, ar[1] = 1.0; /* a[1,1] = 1 */
    ia[2] = 1, ja[2] = 2, ar[2] = 2.0; /* a[1,2] = 2 */
    ia[3] = 2, ja[3] = 1, ar[3] = 3.0; /* a[2,1] = 3 */
    ia[4] = 2, ja[4] = 2, ar[4] = 1.0; /* a[2,2] = 1 */
    glp_load_matrix(lp, 4, ia, ja, ar);
    /* solve problem */
    glp_simplex(lp, NULL);
    /* recover and display results */
    z = glp_get_obj_val(lp);
    x1 = glp_get_col_prim(lp, 1);
    x2 = glp_get_col_prim(lp, 2);
    printf("z = %g; x1 = %g; x2 = %g\n", z, x1, x2);
    /* housekeeping */
    glp_delete_prob(lp);
    glp_free_env();
    return 0;
}

- (void)applicationWillFinishLaunching:(NSNotification *)aNotification {
    
    // GLPK
    
    LOO_MEASURE_TIME("GLPK") {
        testGLPK();
    }
    
    // Snippet testing
    
    float angle;
    COUNTER_CLOCKWISE_ANGLE(10, 0, 0, -10, angle);
    printf("CCW angle : %f\n", angle);
    CLOCKWISE_ANGLE(0, 10, 0, -10, angle);
    printf("CW angle : %f\n", angle);
    
    segment s;
    s.x1 = -1.0;
    s.y1 = 1.0;
    s.x2 = 9.0;
    s.y2 = 11.0;
    s.is_line = 1;
    printf("Y for X = %f\n", getYForX(&s, 9));
        
    // Memory use monitoring
    
    printf("Memory for airports irrelevant\n");
    printf("Memory for trajectories (including some room for intersections) : %li x # = %li bytes\n",
          sizeof(trajectory),
          MAX_AIRPORTS*2*(MAX_AIRPORTS*2-2)*sizeof(trajectory));
    printf("Memory for segments : %li x # = %li bytes\n",
          sizeof(trajectory_segment),
          MAX_AIRPORTS*2*(MAX_AIRPORTS*2-2)*5*sizeof(trajectory_segment));
    printf("Memory for intersections inpredictable, depends on input\n");
    
    // Speed tests
    
    int nbAirports = 1;
    
    LOO_MEASURE_TIME("Quick Sort") {
        char print = 0;
        int N = 15200;
        end_point *transitions[N];
        if (print) printf("Transitions (unordered) : ");
        for (int i=0 ; i<N ; i++) {
            transitions[i] = malloc(sizeof(end_point));
            transitions[i]->x = (float)(rand() % N);
            if (print) printf("%.0f ", transitions[i]->x);
        }
        quicksort(transitions, 0, N-1);
        if (print) {
            printf("\nTransitions (ordered) : ");
            for (int i=0 ; i<N ; i++)
                printf("%.0f ", transitions[i]->x);
            printf("\n");
        }
    }
    
    int nbTrajectories = nbAirports*2*(nbAirports*2-2);
    
    LOO_MEASURE_TIME("Dubins Path Calculation") {
        for (int i=0 ; i<nbTrajectories ; i++){
            double q0[] = { -i,-i,i };
            double q1[] = { i,i,-i };
            DubinsPath path;
            dubins_shortest_path( &path, q0, q1);
            // printf("--- %f %f %f %i\n", path.param[0], path.param[1], path.param[2], path.type);
            // dubins_path_sample_many( &path, 0.1, printConfiguration, NULL);
        }
    }
    
    int nbSegments = (nbAirports*2*(nbAirports*2-2) * 5);
    
    LOO_MEASURE_TIME("Line-Line Intersection") {
        for (int i=0 ; i<nbSegments*nbSegments ; i++) {
            segment s1; s1.x1 = 0; s1.y1 = 0; s1.x2 = -10; s1.y2 = 10;
            segment s2; s2.x1 = 0; s2.y1 = 10; s2.x2 = 10; s2.y2 = 0;
            point_2Df p1;
            get_line_line_intersection(s1, s2, &p1);
            //printf("Intersection line-line (%i) : %f, %f\n", get_line_line_intersection(s1, s2, &p1), p1.x, p1.y);
        }
    }
    
    LOO_MEASURE_TIME("Line-Circle Intersection") {
        for (int i=0 ; i<nbSegments*nbSegments ; i++) {
            segment s; s.x1 = 0; s.y1 = -800; s.x2 = 0; s.y2 = 800;
            segment c; c.x1 = 0; c.y1 = 0; c.x2 = 0; c.y2 = 0;
            point_2Df p1, p2;
            get_line_circle_intersection(s, c, &p1, &p2);
            //printf("Intersection line-circle (%i) : %f, %f\n", get_line_circle_intersection(s, c, &p1, &p2), p1.x, p1.y);
        }
    }
        
    LOO_MEASURE_TIME("Circle-Circle Intersection") {
        for (int i=0 ; i<nbSegments*nbSegments ; i++) {
            segment c1; c1.x1 = 300; c1.y1 = 0; c1.x2 = 0; c1.y2 = 0;
            segment c2; c2.x1 = -400; c2.y1 = 0; c2.x2 = 0; c2.y2 = 0;
            point_2Df p1, p2;
            get_circle_circle_intersection(c1, c2, &p1, &p2);
            //printf("Intersection circle-circle (%i) : %f, %f\n", get_circle_circle_intersection(c1, c2, &p1, &p2), p2.x, p2.y);
        }
    }
    
    /*
    int nbInputSegments = 30;
    vector left_ep;
    vector_init(&left_ep);
    vector right_ep;
    vector_init(&right_ep);
    for (int i=0 ; i<200 ; i++) {
        int *x1 = malloc(sizeof(int *));
        *x1 = i;
        vector_add(&left_ep, x1);
        int *x2 = malloc(sizeof(int *));
        *x2 = i+200;
        vector_add(&right_ep, x2);
    }
    segment *segments = malloc(sizeof(trajectory_segment) * nbInputSegments);
    srand((unsigned int)time(NULL)); // Feed seed!
    for (int i=0 ; i<nbInputSegments ; i++) {
        segment *segment = malloc(sizeof(segment));
        segment->is_line = 1;
        float x1 = *(int *)vector_get(&left_ep, i);
        float y1 = (float)(rand() % 1000);
        float x2 = *(int *)vector_get(&right_ep, i);
        float y2 = (float)(rand() % 1000);
        vector_delete(&left_ep, i);
        vector_delete(&right_ep, i);
        printf("-> Segment at %f %f %f %f\n", MIN(x1, x2), y1, MAX(x1, x2), y2);
        segment->x1 = MIN(x1, x2);
        segment->y1 = y1;
        segment->x2 = MAX(x1, x2);
        segment->y2 = y2;
        segments[i] = *segment;
    }
    */
    
    // Intersection determination tests
    int nbInputSegments = 30000 /* nbSegments */;
    segment *segments = NULL;
    char ok = 0;
    while (!ok) {
        
        segments = malloc(sizeof(segment) * nbInputSegments);
        srand((unsigned int)time(NULL)); // Feed seed!
        for (int i=0 ; i<nbInputSegments ; i++) {
            segments[i].is_line = 1;
            // printf("-> Segment at %f %f %f %f\n", MIN(x1, x2), y1, MAX(x1, x2), y2);
            segments[i].x1 = i;
            segments[i].y1 = float_rand(0.0, 1000.0);
            segments[i].x2 = i+0.5;
            segments[i].y2 = float_rand(0.0, 1000.0);
        }
        
        char allok = 1;
        LOO_MEASURE_TIME("Segment List Intersections") {
        for (int i=0 ; i<nbInputSegments ; i++) {
            for (int j=i+1 ; j<nbInputSegments ; j++) {
                if (segments[i].x1 == segments[i].x2
                    || segments[i].x1 == segments[j].x1
                    || segments[i].x1 == segments[j].x2
                    || segments[i].x2 == segments[j].x1
                    || segments[i].x2 == segments[j].x2) {
                    printf("%f %f %f %f", segments[i].x1, segments[i].x2, segments[j].x1, segments[j].x2);
                    allok = 0;
                    break;
                }
            }
            if (allok == 0)
                break;
        }
        }
        
        if (allok)
            ok = 1;
        else
            free(segments);
        
    }
    
    printf("All ok\n");
    
    
    // Simple example
//    segment *se=malloc(sizeof(segment));se->x1=12;se->y1=7;se->x2=16;se->y2=4;se->is_line=1;segments[0]=*se;
//    se=malloc(sizeof(segment));se->x1=17;se->y1=0;se->x2=19;se->y2=0;se->is_line=1;segments[1]=*se;
//    se=malloc(sizeof(segment));se->x1=5;se->y1=5;se->x2=7;se->y2=2;se->is_line=1;segments[2]=*se;
//    se=malloc(sizeof(segment));se->x1=2;se->y1=8;se->x2=20;se->y2=9;se->is_line=1;segments[3]=*se;
    
    LOO_MEASURE_TIME("Segment List Intersections") {
        reportIntersections(segments, nbInputSegments);
    }
    
    exit(0);
    
}

@end