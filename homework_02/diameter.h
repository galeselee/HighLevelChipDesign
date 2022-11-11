#include <cstring>
#include <fstream>
#include <iostream>
#include <sys/time.h>
#include <vector>
#include <sstream>
#include <stdlib.h>
#include <algorithm>
#include <set>
#include <queue>
#include <math.h>
#include <stdlib.h>
#include <limits>
#include <ctime>
#include <hls_stream.h>
#include <cfloat>
#include <ap_int.h>
#include <stdint.h>

const int PARA_NUM = 8;        // Degree of paralleism
const int RANDOM_OFFSET = 380; // Start point

#define NUMVert 1015
#define NUMEdge 11003

template <typename MType>
union f_cast;

template <>
union f_cast<float> {
    float f;
    uint32_t i;
};

struct IdWeight {
    unsigned column;
    float weight;
};

void my_sssp(unsigned source,
             unsigned* offset,
             IdWeight* idw,
             unsigned& dst,
             float& dst_distance);

void diameter(unsigned* offset,
              unsigned* column,
              float* weight,
              float* max_dist,
              unsigned* src,
              unsigned* des);

void diameter_kernel(unsigned* offset,
					unsigned* column,
					float* weight,
					float* max_dist,
					unsigned* src,
					unsigned* des);
