cmake_minimum_required(VERSION 3.22.1)

# 添加 define -DNO_CRYPTO
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

project("livepush")

# 指定引入头文件
include_directories(${CMAKE_SOURCE_DIR}/util)
include_directories(${CMAKE_SOURCE_DIR}/librtmp)
# src/main/cpp 目录下所有的.cpp 文件都需要编译进来
file(GLOB SOURCE ${CMAKE_SOURCE_DIR}/librtmp/*.c)
file(GLOB SRC_SOURCE ${CMAKE_SOURCE_DIR}/util/*.cpp)


add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        native-lib.cpp
        ${SOURCE}
        ${SRC_SOURCE})

target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        android
        log)