module Mux2to1(d0,
	           d1,
	           s,
	           y);
    parameter N = 1;
    input wire[N-1:0] d0;
    input wire[N-1:0] d1;
    input wire s;
    output wire[N-1:0] y;
    wire[N-1:0] tmp_0;
    wire[N-1:0] tmp_1;
    wire[N-1:0] exp_s;
    wire[N-1:0] not_s;
    assign exp_s = {(N){s}};
    not gate_not[N-1:0](not_s, exp_s);
    and gate_and_0[N-1:0](tmp_0, not_s, d0);
    and gate_and_1[N-1:0](tmp_1, exp_s, d1);
    or gale_or_0[N-1:0](y, tmp_0, tmp_1);
endmodule
