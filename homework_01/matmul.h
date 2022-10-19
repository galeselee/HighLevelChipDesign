// -----------------------------|
// Don't modify these macros ---|
#define eps 1e-6
#define I 64
#define J 64
#define K 64
// -----------------------------|

#include <ap_fixed.h>
#include <hls_vector.h>

// m j
#define block_m 4
#define m_block 16

// n i
#define block_n 4
#define n_block 16

typedef ap_ufixed<8, 5> in_t;
typedef ap_ufixed<22, 16> data_t;

typedef hls::vector<in_t, block_m> block_m_t;
typedef hls::vector<in_t, block_n> block_n_t;
typedef hls::vector<data_t, block_m> data_out_t;

void mm(const block_n_t A[K][n_block], const block_m_t B[K][m_block], data_out_t C[I][m_block]);
