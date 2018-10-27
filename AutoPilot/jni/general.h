/**
 * General tools.
 *
 * @version Team Safier
 * @version 1.0
 */

#ifndef _TOOLS_
#define _TOOLS_

//
// MACROS
//

#define MIN(a,b) (((a)<(b))?(a):(b))
#define MAX(a,b) (((a)>(b))?(a):(b))

//
// DEBUGGING
//

/*
 * Print out the bits of the given unsigned integer.
 */
void showbits(unsigned int num);

#endif /* _TOOLS_ */