module Mux4to1_tb();
  reg [3:0] d;
  reg [1:0] s;
  wire y;
  Mux4to1 dut(.d(d), .s(s), .y(y));


  initial
  begin
    $dumpvars(0, d, s, y);
    d = 4'b1100;
    s = 2'b00;
    #1 $display("D=%b, S=%d, Y=%d\n", d, s, y);
    d = 4'b1100;
    s = 2'b01;
    #1 $display("D=%b, S=%d, Y=%d\n", d, s, y);
    d = 4'b1100;
    s = 2'b10;
    #1 $display("D=%b, S=%d, Y=%d\n", d, s, y);
    d = 4'b1100;
    s = 2'b11;
    #1 $display("D=%b, S=%d, Y=%d\n", d, s, y);
    $finish;
  end
endmodule
