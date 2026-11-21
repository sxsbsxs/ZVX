cmake_minimum_required(VERSION 3.22.1)
project(zvx_native LANGUAGES CXX)

add_library(zvx_native SHARED
    zvx_native.cpp
    subtitle_timeline.cpp
    audio_dsp.cpp
)

target_compile_features(zvx_native PRIVATE cxx_std_17)
target_compile_options(zvx_native PRIVATE -Wall -Wextra -Werror)
