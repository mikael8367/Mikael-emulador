cmake_minimum_required(VERSION 3.22.1)
project(mikael_native)

add_library(mikael_native SHARED mikael_native.cpp)

find_library(log-lib log)
target_link_libraries(mikael_native ${log-lib})

target_compile_features(mikael_native PRIVATE cxx_std_17)
