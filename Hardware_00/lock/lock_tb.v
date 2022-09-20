module Lock_tb();
    reg clock;
    reg [3:0] value;
    reg new, reset;
    wire opened;

    Lock lock(.clock(clock), .value(value), .new(new), .reset(reset), .opened(opened));

    initial begin
        clock = 1'b0;
        forever begin
            #1 clock = 1'b1;
            #1 clock = 1'b0;
        end
    end

    initial begin
        $dumpvars(0, lock);
        reset = 1'b1;
        value = 4'dx;
        new = 1'b0;
        #2 reset = 1'b0;
        value = 4'd1;
        new = 1'b1;
        #2 value = 4'd2;
        #2 value = 4'd3;
        #2 $display("Input = 123, opened = %d", opened);
        new = 1'b0;
        reset = 1'b1;
        #2 reset = 1'b0;
        #2 value = 4'd1;
        #4 new = 1'b1;
        #2 value = 4'd2;
        new = 1'b0;
        #2 new = 1'b1;
        #2 new = 1'b0;
        #4 value = 4'd3;
        #4 new = 1'b1;
        #2 $display("Delayed Input = 123, opened = %d", opened);
        new = 1'b0;
        reset = 1'b1;
        #2 value = 4'd1;
        reset = 1'b0;
        new = 1'b1;
        #2 new = 1'b0;
        #2 value = 4'd2;
        new = 1'b1;
        #2 value = 4'd9;
        #2 $display("Wrong Input = 129, opened = %d", opened);
        #2 value = 4'd7;
        #2 $display("Wrong Input = 1297, opened = %d", opened);
        #2 new = 1'b0;
        value = 4'd8;
        #2 $display("Wrong Input = 12978, opened = %d", opened);
        reset = 1'b1;
        #2 reset = 1'b0;
        value = 4'd1;
        #2 new = 1'b1;
        #2 value = 4'd2;
        #2 new = 1'b0;
        #10 value= 4'd3;
        #10 new = 1'b1;
        #2 $display("Reseted Input = 123, opened = %d", opened);
        $finish;
    end
endmodule
