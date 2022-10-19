#include "matmul.h"
#include <cstring>

// Match mm's function declaration in matmul.h
void mm(const block_n_t A[K][n_block], const block_m_t B[K][m_block], data_out_t C[I][m_block]) {

#pragma HLS interface ap_ctrl_none port = return

#pragma HLS interface m_axi port = A bundle=m1
#pragma HLS interface m_axi port = B bundle=m2
#pragma HLS interface m_axi port = C bundle=m3

  block_n_t local_A[K][n_block];
  block_m_t local_B[K][m_block];
  data_out_t ret[block_n];
  data_out_t block_b;
  data_out_t block_a;
  data_out_t zero_block{0,0,0,0};
#pragma HLS ARRAY_PARTITION variable=ret type=complete

  for (int k = 0; k < K; k++) {
	  for (int l = 0; l < m_block; l++) {
#pragma HLS pipeline II=1
		  local_B[k][l] = B[k][l];
		  local_A[k][l] = A[k][l];
	  }
  }

// k i j or i k j
  for (int i = 0; i < n_block; i++) {
	  int idx_c = i * block_n;
      for (int j = 0; j < m_block; j++) {

init_ret:
    	  for (int ii = 0; ii < block_n; ii++) {
#pragma HLS unroll
    		  ret[ii] = zero_block;
    	  }

COMPUTE:
          for (int k = 0; k < K; k++) {
#pragma HLS pipeline II=2
        	  block_b[0] = local_B[k][j][0];
        	  block_b[1] = local_B[k][j][1];
        	  block_b[2] = local_B[k][j][2];
        	  block_b[3] = local_B[k][j][3];
COMPUTE_UNIT:
        	  for (int ii = 0; ii < block_n; ii++) {
#pragma HLS pipeline II=1
        		  block_a[0] = local_A[k][i][ii];
        		  block_a[1] = local_A[k][i][ii];
        		  block_a[2] = local_A[k][i][ii];
        		  block_a[3] = local_A[k][i][ii];
        		  ret[ii] += block_a * block_b;
        	  }
          }
STORE:
          for (int ll = 0; ll < block_n; ll++) {
#pragma HLS pipeline II=1
        	C[idx_c+ll][j] = ret[ll];
          }
      }
  }

}
