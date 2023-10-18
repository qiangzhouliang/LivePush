cmake_minimum_required(VERSION 3.22.1)

project("livepush")

# 添加 define -DNO_CRYPTO
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

# 指定共享库目录
link_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI})

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
        # 链接 OpenSLES
        OpenSLES
        log
        music-player
        # 编解码(最重要的库)
        avcodec-57
        # 设备信息
        avdevice-57
        # 滤镜特效处理库
        avfilter-6
        # 封装格式处理库
        avformat-57
        # 工具库(大部分库都需要这个库的支持)
        avutil-55
        # 后期处理
        postproc-54
        # 音频采样数据格式转换库
        swresample-2
        # 视频像素数据格式转换
        swscale-4)