module Lock(clock, value, new, reset, opened);
    input wire clock;
    input wire [3:0]value;
    input wire new;
    input wire reset;
    output reg opened;


    reg[3:0] status;
    initial 
    begin
        status <= 2'b00;
        opened <= 1'b0;
    end

    always @(posedge clock)
    begin
        if (reset === 1'b1)
        begin
            opened = 0;
            status = 0;
        end
        if (new == 1)
        begin
            if (status === 2'b00)
            begin
                if (value === 4'd1)
                    status = 2'b01;
                else
                    status = 2'b00;
            end

            else if (status === 2'b01)
            begin
                if (value === 4'b0010)
                    status = 2'b10;
                else 
                    status = 2'b00;
            end

            else if (status == 2'b10)
            begin
                if (value === 4'b0011)
                begin
                    opened = 1'b1;
                    status = 2'b11;
                end
                else
                    status = 2'b00;
            end
            
            else if (status == 2'b11)
            begin
                status = 0;
            end
        end
    end

endmodule
