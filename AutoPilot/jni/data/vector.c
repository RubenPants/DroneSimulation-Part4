/**
 * Vector tools (working with dynamic arrays).
 *
 * @author Team Saffier
 * @version 1.0
 * @note Adapted from https://gist.github.com/EmilHernvall/953968
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "vector.h"

/*
 * Macros.
 */
#define INIT_SIZE 50

void vector_init(vector *v) {
    v->data = NULL;
    v->size = 0;
    v->count = 0;
}

void vector_copy(vector *v, vector *tocopy) {
    v->data = tocopy->data;
    v->size = tocopy->size;
    v->count = tocopy->count;
}

void vector_deepcopy(vector *v, vector *tocopy) {
    if (v->size > 0)
        free(v->data);
    v->data = malloc(sizeof(void*) * tocopy->size);
    memcpy(v->data, tocopy->data, sizeof(void *) * tocopy->size);
    v->size = tocopy->size;
    v->count = tocopy->count;
}

int vector_count(vector *v) {
    return v->count;
}

void vector_add(vector *v, void *e) {
    if (vector_contains(v, e))
        return;
    if (v->size == 0) {
        v->size = INIT_SIZE;
        v->data = malloc(sizeof(void*) * v->size);
        memset(v->data, '\0', sizeof(void*) * v->size);
    }

    if (v->size == v->count) {
        v->size *= 2;
        v->data = realloc(v->data, sizeof(void*) * v->size);
    }

    v->data[v->count] = e;
    v->count++;
}

char vector_contains(vector *v, void *e) {
    for (int i=0 ; i<v->size ; i++)
        if (v->data[i] == e)
            return 1;
    return 0;
}

void vector_set(vector *v, int index, void *e) {
    if (index >= v->count) {
        return;
    }

    v->data[index] = e;
}

void vector_insert(vector *v, int index, void *e) {
    if (index >= 0 && index <= v->count) {
        vector_add(v, e);
        for (int i=v->count-1 ; i>index ; i--)
            v->data[i] = v->data[i-1];
        v->data[index] = e;
    }
}

void *vector_get(vector *v, int index) {
    if (index >= v->count || index < 0) {
        return NULL;
    }

    return v->data[index];
}

void vector_delete(vector *v, int index) {
    if (index >= v->count) {
        return;
    }

    for (int i = index +1, j = index; i < v->count; ++i) {
        v->data[j] = v->data[i];
        ++j;
    }

    v->count--;
}

void vector_delete_element(vector *v, void *el) {
    for (int i=0 ; i<v->count ; i++)
        if (v->data[i] == el)
            return vector_delete(v, i);
}

void vector_free(vector *v) {
    free(v->data);
}