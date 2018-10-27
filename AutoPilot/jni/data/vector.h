/**
 * Vector tools (working with dynamic arrays).
 *
 * @author Team Saffier
 * @version 1.0
 * @note Adapted from https://gist.github.com/EmilHernvall/953968
 */

#ifndef _VECTOR_H__
#define _VECTOR_H__

// Data struct
typedef struct vector_ {
    void** data;
    int size;
    int count;
} vector;

// No documentation necessary
void vector_init(vector *);
void vector_copy(vector *, vector *);
void vector_deepcopy(vector *, vector *);
int vector_count(vector *);
void vector_add(vector *, void *);
void vector_set(vector *, int, void *);
void vector_insert(vector *, int, void *);
void *vector_get(vector *, int);
void vector_delete(vector *, int);
void vector_delete_element(vector *, void *);
void vector_free(vector *);
char vector_contains(vector *, void *);

#endif /* _VECTOR_H__ */