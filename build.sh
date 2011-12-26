#!/bin/bash
export ANDROID_SDK=/Programmas/android-sdk-linux
export ANDROID_NDK=/Programmas/android-ndk-r7
scons -Q android=1 install=1
