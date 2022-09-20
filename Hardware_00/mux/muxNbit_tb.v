module Mux2to14Bit_tb();
  reg [3:0] d0, d1;
  reg s;
  wire [3:0] y;
  Mux2to1 #(.N(4)) dut(.d0(d0), .d1(d1), .s(s), .y(y));

  initial
  begin
    $dumpvars(0, d0, d1, s, y);
    $display("Testing 2:1 mux N=4");
    d0 = 4'b1100;
    d1 = 4'b0101;
    s = 1'b0;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 4'b1100;
    d1 = 4'b0101;
    s = 1'b1;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 4'b1111;
    d1 = 4'b0001;
    s = 1'b0;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 4'b1101;
    d1 = 4'b0110;
    s = 1'b1;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    $finish;
  end
endmodule

module Mux2to18Bit_tb();
  reg [7:0] d0, d1;
  reg s;
  wire [7:0] y;
  Mux2to1 #(.N(8)) dut(.d0(d0), .d1(d1), .s(s), .y(y));

  initial
  begin
    $dumpvars(0, d0, d1, s, y);
    $display("Testing 2:1 mux N=8");
    d0 = 8'd255;
    d1 = 8'd127;
    s = 1'b0;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 8'd255;
    d1 = 8'd127;
    s = 1'b1;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 8'd93;
    d1 = 8'd5;
    s = 1'b0;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    d0 = 8'd38;
    d1 = 8'd201;
    s = 1'b1;
    #1 $display("D0=%d, D1=%d, S=%d, Y=%d\n", d0, d1, s, y);
    $finish;
  end
endmodule

/*
module Mux4to16Bit_tb();
  reg [5:0] d0, d1, d2, d3;
  reg [1:0] s;
  wire [5:0] y;
  Mux4to1 #(.N(6)) dut(.d0(d0), .d1(d1), .d2(d2), .d3(d3), .s(s), .y(y));

  initial
  begin
    $dumpvars(0, d0, d1, d2, d3, s, y);
    $display("Testing 4:1 mux N=6");
    d0 = 6'd63;
    d1 = 6'd21;
    d2 = 6'd0;
    d3 = 6'd44;
    s = 2'd0;
    #1 $display("D0=%d, D1=%d, D2=%d, D3=%d, S=%d, Y=%d\n", d0, d1, d2, d3, s, y);
    d0 = 6'd63;
    d1 = 6'd21;
    d2 = 6'd0;
    d3 = 6'd44;
    s = 2'd1;
    #1 $display("D0=%d, D1=%d, D2=%d, D3=%d, S=%d, Y=%d\n", d0, d1, d2, d3, s, y);
    d0 = 6'd63;
    d1 = 6'd21;
    d2 = 6'd0;
    d3 = 6'd44;
    s = 2'd2;
    #1 $display("D0=%d, D1=%d, D2=%d, D3=%d, S=%d, Y=%d\n", d0, d1, d2, d3, s, y);
    d0 = 6'd63;
    d1 = 6'd21;
    d2 = 6'd0;
    d3 = 6'd44;
    s = 2'd3;
    #1 $display("D0=%d, D1=%d, D2=%d, D3=%d, S=%d, Y=%d\n", d0, d1, d2, d3, s, y);
    $finish;
  end
endmodule
*/

module muxNBit_tb();
  Mux2to14Bit_tb test2to1_4bit();
  Mux2to18Bit_tb test2to1_8bit();
  //Mux4to16Bit_tb test4to1_6bit();
endmodule
