 cmake_minimum_required(VERSION 3.4.1)

 add_definitions("-DSTDC_HEADERS")
 # 可以指定 so 文件输出目录
 # set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../lame/libs/${ANDROID_ABI})

 add_library(
         lamemp3
         SHARED
         src/main/cpp/LameUtil.c
         src/main/cpp/lamemp3/bitstream.c
         src/main/cpp/lamemp3/encoder.c
         src/main/cpp/lamemp3/fft.c
         src/main/cpp/lamemp3/gain_analysis.c
         src/main/cpp/lamemp3/id3tag.c
         src/main/cpp/lamemp3/lame.c
         src/main/cpp/lamemp3/mpglib_interface.c
         src/main/cpp/lamemp3/newmdct.c
         src/main/cpp/lamemp3/presets.c
         src/main/cpp/lamemp3/psymodel.c
         src/main/cpp/lamemp3/quantize_pvt.c
         src/main/cpp/lamemp3/quantize.c
         src/main/cpp/lamemp3/reservoir.c
         src/main/cpp/lamemp3/set_get.c
         src/main/cpp/lamemp3/tables.c
         src/main/cpp/lamemp3/takehiro.c
         src/main/cpp/lamemp3/util.c
         src/main/cpp/lamemp3/vbrquantize.c
         src/main/cpp/lamemp3/VbrTag.c
         src/main/cpp/lamemp3/version.c
 )