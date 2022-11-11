#include "diameter.h"
void my_sssp(uint16_t source,
             uint16_t* offset,
             IdWeight* idw,
             uint16_t& dst,
             float& dst_distance) {
    hls::stream<uint16_t, 4096> q;
    float distance[1024];
#pragma HLS BIND_STORAGE variable = q type = fifo
#pragma HLS ARRAY_PARTITION variable = distance dim = 1 type=cyclic factor=8

    uint16_t source_true = source + RANDOM_OFFSET;

    for (int i = 0; i < NUMVert; i++) {
        distance[i] = FLT_MAX;
    }
    distance[source_true] = 0;
    q.write(source_true);

    while (!q.empty()) {
        uint16_t tmp = q.read();
        int start = offset[tmp], end = offset[tmp + 1];
        for (int i = start; i < end; i++) {
#pragma HLS PIPELINE II = 1
#pragma HLS dependence variable = distance inter false
#pragma HLS dependence variable = distance intra true
            float fromDist = distance[tmp];
            float toDist = distance[idw[i].column];
            float curDist = fromDist + idw[i].weight;
            if (curDist < toDist)
            {
                distance[idw[i].column] = curDist;
                q.write(idw[i].column);
            }
        }
    }

    float max_distance = 0;
    uint16_t max_dist = 0;
    for (int i = 0; i < NUMVert; i++) {
        if (max_distance < distance[i] && distance[i] != FLT_MAX)
        {
            max_distance = distance[i];
            max_dist = i;
        }
    }

    dst = max_dist;
    dst_distance = max_distance;
}

void diameter(uint16_t* offset,
              uint16_t* column,
              float* weight,
              float* max_dist,
              uint16_t* src,
              uint16_t* des) {
#pragma HLS DATAFLOW
    static IdWeight idw[PARA_NUM][16000];
    static uint16_t offset_cpy[PARA_NUM][1600];
#pragma HLS ARRAY_PARTITION variable = idw dim = 1 complete
#pragma HLS ARRAY_PARTITION variable = offset_cpy dim = 1 complete

    static uint16_t source[PARA_NUM];
    static uint16_t destination[PARA_NUM];
    static float distance[PARA_NUM];
#pragma HLS ARRAY_PARTITION variable = source dim = 1 complete
#pragma HLS ARRAY_PARTITION variable = destination dim = 1 complete
#pragma HLS ARRAY_PARTITION variable = distance dim = 1 complete

    for (int i = 0; i < NUMEdge; i++) {
        for (int n = 0; n < PARA_NUM; n++) {
#pragma HLS unroll
            idw[n][i].column = column[i];
            idw[n][i].weight = weight[i];
            source[n] = (NUMVert - 1) / PARA_NUM * n;
        }
    }

    for (int i = 0; i < NUMVert + 1; i++) {
        for (int n = 0; n < PARA_NUM; n++) {
#pragma HLS unroll
            offset_cpy[n][i] = offset[i];
        }
    }

    for (int n = 0; n < PARA_NUM; n++) {
#pragma HLS unroll
       my_sssp(source[n], offset_cpy[n], idw[n], destination[n], distance[n]);
    }

    float max_distance = 0;
    uint16_t max_des = 0;
    uint16_t max_src = 0;
    for (int i = 0; i < PARA_NUM; i++) {
#pragma HLS pipeline
        if (max_distance < distance[i])
        {
            max_distance = distance[i];
            max_des = destination[i];
            max_src = source[i];
        }
    }

    src[0] = max_src + RANDOM_OFFSET;
    des[0] = max_des;
    max_dist[0] = max_distance;
}

/**
 * @brief diameter estimate based on the sssp algorithm
 *
 * @param offset row offset of CSR format
 * @param column column index of CSR format
 * @param weight weight value of CSR format
 * @param max_distance the result of max distance
 * @param src the result of source vertex
 * @param des the result of destination vertex
 *
 */
void diameter_kernel(uint16_t* offset,
                        uint16_t* column,
                        float* weight,
                        float* max_dist,
                        uint16_t* src,
                        uint16_t* des) {

#pragma HLS INTERFACE m_axi offset = slave latency = 32 num_read_outstanding = 32 max_read_burst_length = 8 bundle = \
    gmem0_0 port = column depth = 11003
#pragma HLS INTERFACE m_axi offset = slave latency = 32 num_read_outstanding = 32 max_read_burst_length = 8 bundle = \
    gmem0_1 port = offset depth = 1016
#pragma HLS INTERFACE m_axi offset = slave latency = 32 num_read_outstanding = 32 max_read_burst_length = 8 bundle = \
    gmem0_2 port = weight depth = 11003

#pragma HLS INTERFACE m_axi offset = slave latency = 32 num_write_outstanding = 64 max_write_burst_length = 2 bundle = \
    gmem1_0 port = max_dist depth = 1
#pragma HLS INTERFACE m_axi offset = slave latency = 32 num_write_outstanding = 64 max_write_burst_length = 2 bundle = \
    gmem1_1 port = src depth = 1
#pragma HLS INTERFACE m_axi offset = slave latency = 32 num_write_outstanding = 64 max_write_burst_length = 2 bundle = \
    gmem1_2 port = des depth = 1

#pragma HLS INTERFACE s_axilite port = column bundle = control
#pragma HLS INTERFACE s_axilite port = offset bundle = control
#pragma HLS INTERFACE s_axilite port = weight bundle = control
#pragma HLS INTERFACE s_axilite port = max_dist bundle = control
#pragma HLS INTERFACE s_axilite port = src bundle = control
#pragma HLS INTERFACE s_axilite port = des bundle = control
#pragma HLS INTERFACE s_axilite port = return bundle = control

    diameter(offset, column, weight, max_dist, src, des);
}

