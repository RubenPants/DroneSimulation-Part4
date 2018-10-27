/**
 * Dubins path calculation.
 *
 * @author Team Saffier
 * @version 1.0
 */
#ifdef WIN32
#define _USE_MATH_DEFINES
#endif
#include <math.h>
#include "dubins.h"

typedef enum {
    L_SEG = 0,
    S_SEG = 1,
    R_SEG = 2
} SegmentType;

/* The segment types for each of the Path types */
const SegmentType DIRDATA[][3] = {
    { L_SEG, S_SEG, L_SEG },
    { L_SEG, S_SEG, R_SEG },
    { R_SEG, S_SEG, L_SEG },
    { R_SEG, S_SEG, R_SEG }/*,
    { R_SEG, L_SEG, R_SEG },
    { L_SEG, R_SEG, L_SEG }*/
};

typedef struct {
    double alpha;
    double beta;
    double d;
    double sa;
    double sb;
    double ca;
    double cb;
    double c_ab;
    double d_sq;
} DubinsIntermediateResults;

int dubins_word(DubinsIntermediateResults* in, DubinsPathType pathType, double out[3]);
int dubins_intermediate_results(DubinsIntermediateResults* in, double q0[3], double q1[3]);

/**
 * Floating point modulus suitable for rings
 *
 * fmod doesn't behave correctly for angular quantities, this function does
 */
double fmodr(double x, double y) {
    return x - y*floor(x/y);
}

double mod2pi(double theta) {
    return fmodr( theta, 2 * M_PI );
}

int dubins_shortest_path(DubinsPath *path, double q0[3], double q1[3]) {
    
    int i, errcode;
    DubinsIntermediateResults in;
    double params[3];
    double cost;
    double best_cost = INFINITY;
    int best_word = -1;
    errcode = dubins_intermediate_results(&in, q0, q1);
    if(errcode != EDUBOK) {
        return errcode;
    }
    
    path->startTheta = q0[2];
    path->endTheta = q1[2];

    for( i = 0; i < 6; i++ ) {
        DubinsPathType pathType = (DubinsPathType)i;
        errcode = dubins_word(&in, pathType, params);
        if(errcode == EDUBOK) {
            cost = params[0] + params[1] + params[2];
            if(cost < best_cost) {
                best_word = i;
                best_cost = cost;
                path->param[0] = params[0];
                path->param[1] = params[1];
                path->param[2] = params[2];
                path->type = pathType;
            }
        }
    }
    
    if(best_word == -1) {
        return EDUBNOPATH;
    }
    
    return EDUBOK;
    
}

int dubins_path(DubinsPath *path, double q0[3], double q1[3], DubinsPathType pathType) {
    int errcode;
    DubinsIntermediateResults in;
    errcode = dubins_intermediate_results(&in, q0, q1);
    if(errcode == EDUBOK) {
        double params[3];
        errcode = dubins_word(&in, pathType, params);
        if(errcode == EDUBOK) {
            path->param[0] = params[0];
            path->param[1] = params[1];
            path->param[2] = params[2];
            path->type = pathType;
        }
    }
    path->startTheta = q0[2];
    path->endTheta = q1[2];
    return errcode;
}

double dubins_path_length(DubinsPath *path) {
    double length = 0.;
    length += path->param[0];
    length += path->param[1];
    length += path->param[2];
    length = length * RHO;
    return length;
}

int dubins_intermediate_results(DubinsIntermediateResults *in, double q0[3], double q1[3]) {
    
    double dx, dy, D, d, theta, alpha, beta;
    /*
    if (RHO <= 0.0)
        return EDUBBADRHO;
    */

    dx = q1[0] - q0[0];
    dy = q1[1] - q0[1];
    D = sqrt( dx * dx + dy * dy );
    d = D / RHO;
    theta = 0;

    // test required to prevent domain errors if dx=0 and dy=0
    if(d > 0) {
        theta = mod2pi(atan2( dy, dx ));
    }
    alpha = mod2pi(q0[2] - theta);
    beta  = mod2pi(q1[2] - theta);

    in->alpha = alpha;
    in->beta  = beta;
    in->d     = d;
    in->sa    = sin(alpha);
    in->sb    = sin(beta);
    in->ca    = cos(alpha);
    in->cb    = cos(beta);
    in->c_ab  = cos(alpha - beta);
    in->d_sq  = d * d;

    return EDUBOK;
    
}

int dubins_LSL(DubinsIntermediateResults* in, double out[3])  {
    double tmp0, tmp1, p_sq;
    
    tmp0 = in->d + in->sa - in->sb;
    p_sq = 2 + in->d_sq - (2*in->c_ab) + (2 * in->d * (in->sa - in->sb));

    if(p_sq >= 0) {
        tmp1 = atan2( (in->cb - in->ca), tmp0 );
        out[0] = mod2pi(tmp1 - in->alpha);
        out[1] = sqrt(p_sq);
        out[2] = mod2pi(in->beta - tmp1);
        return EDUBOK;
    }
    return EDUBNOPATH;
}


int dubins_RSR(DubinsIntermediateResults* in, double out[3]) {
    double tmp0 = in->d - in->sa + in->sb;
    double p_sq = 2 + in->d_sq - (2 * in->c_ab) + (2 * in->d * (in->sb - in->sa));
    if( p_sq >= 0 ) {
        double tmp1 = atan2( (in->ca - in->cb), tmp0 );
        out[0] = mod2pi(in->alpha - tmp1);
        out[1] = sqrt(p_sq);
        out[2] = mod2pi(tmp1 -in->beta);
        return EDUBOK;
    }
    return EDUBNOPATH;
}

int dubins_LSR(DubinsIntermediateResults* in, double out[3]) {
    double p_sq = -2 + (in->d_sq) + (2 * in->c_ab) + (2 * in->d * (in->sa + in->sb));
    if( p_sq >= 0 ) {
        double p    = sqrt(p_sq);
        double tmp0 = atan2( (-in->ca - in->cb), (in->d + in->sa + in->sb) ) - atan2(-2.0, p);
        out[0] = mod2pi(tmp0 - in->alpha);
        out[1] = p;
        out[2] = mod2pi(tmp0 - mod2pi(in->beta));
        return EDUBOK;
    }
    return EDUBNOPATH;
}

int dubins_RSL(DubinsIntermediateResults* in, double out[3]) {
    double p_sq = -2 + in->d_sq + (2 * in->c_ab) - (2 * in->d * (in->sa + in->sb));
    if( p_sq >= 0 ) {
        double p    = sqrt(p_sq);
        double tmp0 = atan2( (in->ca + in->cb), (in->d - in->sa - in->sb) ) - atan2(2.0, p);
        out[0] = mod2pi(in->alpha - tmp0);
        out[1] = p;
        out[2] = mod2pi(in->beta - tmp0);
        return EDUBOK;
    }
    return EDUBNOPATH;
}

/*
int dubins_RLR(DubinsIntermediateResults* in, double out[3]) {
    double tmp0 = (6. - in->d_sq + 2*in->c_ab + 2*in->d*(in->sa - in->sb)) / 8.;
    double phi  = atan2( in->ca - in->cb, in->d - in->sa + in->sb );
    if( fabs(tmp0) <= 1) {
        double p = mod2pi((2*M_PI) - acos(tmp0) );
        double t = mod2pi(in->alpha - phi + mod2pi(p/2.));
        out[0] = t;
        out[1] = p;
        out[2] = mod2pi(in->alpha - in->beta - t + mod2pi(p));
        return EDUBOK;
    }
    return EDUBNOPATH;
}

int dubins_LRL(DubinsIntermediateResults* in, double out[3]) {
    double tmp0 = (6. - in->d_sq + 2*in->c_ab + 2*in->d*(in->sb - in->sa)) / 8.;
    double phi = atan2( in->ca - in->cb, in->d + in->sa - in->sb );
    if( fabs(tmp0) <= 1) {
        double p = mod2pi( 2*M_PI - acos( tmp0) );
        double t = mod2pi(-in->alpha - phi + p/2.);
        out[0] = t;
        out[1] = p;
        out[2] = mod2pi(mod2pi(in->beta) - in->alpha -t + mod2pi(p));
        return EDUBOK;
    }
    return EDUBNOPATH;
}
 */

int dubins_word(DubinsIntermediateResults* in, DubinsPathType pathType, double out[3]) {
    int result;
    switch(pathType) {
    case LSL:
        result = dubins_LSL(in, out);
        break;
    case RSL:
        result = dubins_RSL(in, out);
        break;
    case LSR:
        result = dubins_LSR(in, out);
        break;
    case RSR:
        result = dubins_RSR(in, out);
        break;
            /*
    case LRL:
        result = dubins_LRL(in, out);
        break;
    case RLR:
        result = dubins_RLR(in, out);
        break;
             */
    default:
        result = EDUBNOPATH;
    }
    return result;
}