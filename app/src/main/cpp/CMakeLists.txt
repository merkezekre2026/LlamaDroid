cmake_minimum_required(VERSION 3.22.1)
project(llamadroid)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/llama.cpp")
if(NOT EXISTS "${LLAMA_CPP_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "llama.cpp not found. Run: git submodule update --init --recursive")
endif()

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_subdirectory("${LLAMA_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp")

add_library(llamadroid SHARED
    llama_jni.cpp
    llama_engine.cpp
)

target_include_directories(llamadroid PRIVATE
    "${LLAMA_CPP_DIR}/include"
    "${LLAMA_CPP_DIR}/ggml/include"
)

target_link_libraries(llamadroid
    llama
    android
    log
)
