// Author : Zeyu Li
// Time: 2022-09-21 00:15
module Accumulator(
        clock,
        start,
        data,
        sum);
    parameter N = 1;
    parameter M = 1;
    
    input wire clock;
    input wire start;
    input wire[N-1:0] data;
    output reg[M-1:0] sum;

    reg [N-1:0] iters;
    reg flag;

    initial
    begin
        iters = {(N){1'b0}};
        flag = 1'b0;
    end

    always @(posedge clock) 
    begin
        if (start === 1'b1) 
        begin
            sum <= {(M){1'b0}};
            iters <= data;
            flag <= 1'b1;
        end
        else if (flag === 1'b1)
        begin
            if (iters != {(N){1'b0}})
            begin
                iters <= iters - {{(N-1){1'b0}}, {1'b1}};
                sum <= sum + {{(M-N){1'b0}}, {data}};
            end
        end
    end
endmodule
                
