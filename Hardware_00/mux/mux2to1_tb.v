module Mux2to1_tb();
  reg d0, d1, s;
  wire y;
  Mux2to1 dut(.d0(d0), .d1(d1), .s(s), .y(y));


  initial
  begin
    $dumpvars(0, d0, d1, s, y);
    d0 = 1'b0;
    d1 = 1'b1;
    s = 1'b0;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 1'b0;
    d1 = 1'b1;
    s = 1'b1;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 1'b1;
    d1 = 1'b0;
    s = 1'b1;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 1'b1;
    d1 = 1'b0;
    s = 1'b0;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    $finish;
  end
endmodule
