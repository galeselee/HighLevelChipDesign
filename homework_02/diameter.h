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
    uint16_t column;
    float weight;
};

void my_sssp(uint16_t source,
             uint16_t* offset,
             IdWeight* idw,
             uint16_t& dst,
             float& dst_distance);

void diameter(uint16_t* offset,
              uint16_t* column,
              float* weight,
              float* max_dist,
              uint16_t* src,
              uint16_t* des);

void diameter_kernel(uint16_t* offset,
					uint16_t* column,
					float* weight,
					float* max_dist,
					uint16_t* src,
					uint16_t* des);
