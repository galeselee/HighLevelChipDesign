/*module Mux2to1(input wire d0,
	           input wire d1,
	           input wire s,
	           output wire y);
    wire tmp_0;
    wire not_s;
    not(not_s, s);
    and(tmp_0, not_s, d0);
    and(tmp_1, s, d1);
    or(y, tmp_0, tmp_1);
endmodule
*/
module Mux4to1(input wire[3:0] d,
               input wire[1:0] s,
               output wire y);
    wire not_s_0;
    wire not_s_1;
    not(not_s_0, s[0]);
    not(not_s_1, s[1]);
    wire tmp0;
    wire tmp1;
    wire tmp2;
    wire tmp3;
    and(tmp0, d[0], not_s_0);
    and(tmp1, d[1], s[0]);
    and(tmp2, d[2], not_s_0);
    and(tmp3, d[3], s[0]);
    wire tmp_00;
    wire tmp_01;
    or(tmp_00, tmp0, tmp1);
    or(tmp_01, tmp2, tmp3);
    wire tmp_10;
    wire tmp_11;
    and(tmp_10, tmp_00, not_s_1);
    and(tmp_11, tmp_01, s[1]);
    or(y, tmp_10, tmp_11);
endmodule
    
