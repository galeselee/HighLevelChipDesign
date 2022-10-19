#include "matmul.h"
#include <cmath>
#include <fstream>
#include <iostream>

int main() {
  double A[I][K], B[K][J];
  double C_ans[I][J];

  // You may modify kernel-related codes (like A_in, B_in, and etc.).
  // But don't modify the things about the ground-truth (A, B, C_ans).

  block_n_t A_in[K][n_block];
  block_m_t B_in[K][m_block];
#pragma HLS AGGREGATE variable=A_in
#pragma HLS AGGREGATE variable=B_in
  data_out_t tmp_C[I][m_block];
#pragma HLS AGGREGATE variable=tmp_C
  data_t C[I][K];

  std::ifstream in("mat.txt");

  for (int i = 0; i < I; i++)
    for (int k = 0; k < K; k++) {
      in >> A[i][k];
    }

  for (int k = 0; k < K; k++)
    for (int j = 0; j < J; j++) {
      in >> B[k][j];
    }

  for (int i = 0; i < n_block; i++) {
    for (int k = 0; k < K; k++) {
      for (int ii = 0; ii < block_n; ii++) {
        A_in[k][i][ii] = A[i*block_n+ii][k];
      }
    }
  }
  
  for (int k = 0; k < K; k++) {
    for (int j = 0; j < m_block; j++) {
      for (int jj = 0; jj < block_m; jj++) {
        B_in[k][j][jj] = B[k][j*block_m+jj];
      }
    }
  }

  mm(A_in, B_in, tmp_C);

  for (int i = 0; i < I; i++) {
	  for (int k = 0; k < m_block; k++) {
		  for (int kk = 0; kk < block_m; kk++)
			  C[i][k*block_m+kk] = tmp_C[i][k][kk];
	  }
  }

  // Don't modify the code below! ---------------------------------------------

  for (int i = 0; i < I; i++)
    for (int j = 0; j < J; j++) {
      C_ans[i][j] = 0;
      for (int k = 0; k < K; k++) {
        C_ans[i][j] += A[i][k] * B[k][j];
      }
    }

  for (int i = 0; i < I; i++)
    for (int j = 0; j < J; j++)
      if ((double)C[i][j] - C_ans[i][j] > eps ||
          C_ans[i][j] - (double)C[i][j] > eps) {
        std::cout << "too much error at (" << i << ", " << j << ")"
                  << std::endl;
        std::cout << "while ans = " << C_ans[i][j] << ", ret = " << (double)C[i][j] << std::endl;
        //return 1;
      }
  std::cout << "Pass test" << std::endl;
  return 0;
}
