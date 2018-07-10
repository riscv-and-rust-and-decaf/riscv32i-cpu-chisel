module GCD( // @[:@3.2]
  input         clock, // @[:@4.4]
  input  [15:0] io_value1, // @[:@6.4]
  input  [15:0] io_value2, // @[:@6.4]
  input         io_loadingValues, // @[:@6.4]
  output [15:0] io_outputGCD // @[:@6.4]
);
  reg [15:0] x; // @[GCD.scala 21:15:@8.4]
  reg [31:0] _RAND_0;
  reg [15:0] y; // @[GCD.scala 22:15:@9.4]
  reg [31:0] _RAND_1;
  wire  _T_17; // @[GCD.scala 24:10:@10.4]
  wire [16:0] _T_18; // @[GCD.scala 24:24:@12.6]
  wire [16:0] _T_19; // @[GCD.scala 24:24:@13.6]
  wire [15:0] _T_20; // @[GCD.scala 24:24:@14.6]
  wire [16:0] _T_21; // @[GCD.scala 25:25:@18.6]
  wire [16:0] _T_22; // @[GCD.scala 25:25:@19.6]
  wire [15:0] _T_23; // @[GCD.scala 25:25:@20.6]
  wire [15:0] _GEN_0; // @[GCD.scala 24:15:@11.4]
  wire [15:0] _GEN_1; // @[GCD.scala 24:15:@11.4]
  assign _T_17 = x > y; // @[GCD.scala 24:10:@10.4]
  assign _T_18 = x - y; // @[GCD.scala 24:24:@12.6]
  assign _T_19 = $unsigned(_T_18); // @[GCD.scala 24:24:@13.6]
  assign _T_20 = _T_19[15:0]; // @[GCD.scala 24:24:@14.6]
  assign _T_21 = y - x; // @[GCD.scala 25:25:@18.6]
  assign _T_22 = $unsigned(_T_21); // @[GCD.scala 25:25:@19.6]
  assign _T_23 = _T_22[15:0]; // @[GCD.scala 25:25:@20.6]
  assign _GEN_0 = _T_17 ? _T_20 : x; // @[GCD.scala 24:15:@11.4]
  assign _GEN_1 = _T_17 ? y : _T_23; // @[GCD.scala 24:15:@11.4]
  assign io_outputGCD = x; // @[GCD.scala 32:16:@27.4]
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE
  integer initvar;
  initial begin
    `ifndef verilator
      #0.002 begin end
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{$random}};
  x = _RAND_0[15:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{$random}};
  y = _RAND_1[15:0];
  `endif // RANDOMIZE_REG_INIT
  end
`endif // RANDOMIZE
  always @(posedge clock) begin
    if (io_loadingValues) begin
      x <= io_value1;
    end else begin
      if (_T_17) begin
        x <= _T_20;
      end
    end
    if (io_loadingValues) begin
      y <= io_value2;
    end else begin
      if (!(_T_17)) begin
        y <= _T_23;
      end
    end
  end
endmodule
module MyTop( // @[:@31.2]
  input         clock, // @[:@32.4]
  input         reset, // @[:@33.4]
  input  [15:0] io_sw_num1, // @[:@34.4]
  input  [15:0] io_sw_num2, // @[:@34.4]
  input         io_loadingValues, // @[:@34.4]
  input         io_btn1, // @[:@34.4]
  input         io_btn2, // @[:@34.4]
  output [15:0] io_num // @[:@34.4]
);
  wire  gcd_clock; // @[Main.scala 15:19:@36.4]
  wire [15:0] gcd_io_value1; // @[Main.scala 15:19:@36.4]
  wire [15:0] gcd_io_value2; // @[Main.scala 15:19:@36.4]
  wire  gcd_io_loadingValues; // @[Main.scala 15:19:@36.4]
  wire [15:0] gcd_io_outputGCD; // @[Main.scala 15:19:@36.4]
  wire [15:0] _GEN_0; // @[Main.scala 21:22:@46.6]
  GCD gcd ( // @[Main.scala 15:19:@36.4]
    .clock(gcd_clock),
    .io_value1(gcd_io_value1),
    .io_value2(gcd_io_value2),
    .io_loadingValues(gcd_io_loadingValues),
    .io_outputGCD(gcd_io_outputGCD)
  );
  assign _GEN_0 = io_btn2 ? io_sw_num2 : gcd_io_outputGCD; // @[Main.scala 21:22:@46.6]
  assign io_num = io_btn1 ? io_sw_num1 : _GEN_0; // @[Main.scala 20:31:@43.6 Main.scala 21:31:@47.8 Main.scala 22:31:@50.8]
  assign gcd_clock = clock; // @[:@37.4]
  assign gcd_io_value1 = io_sw_num1; // @[Main.scala 16:17:@39.4]
  assign gcd_io_value2 = io_sw_num2; // @[Main.scala 17:17:@40.4]
  assign gcd_io_loadingValues = io_loadingValues; // @[Main.scala 18:24:@41.4]
endmodule
