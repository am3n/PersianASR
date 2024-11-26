# Android Persian ASR

![MinAPI](https://img.shields.io/badge/API-21%2B-blue)
[![Release](https://jitpack.io/v/am3n/PersianASR.svg)](https://jitpack.io/#am3n/PersianASR)

<b>Android persian automatic speech recognition library based on Alphacephei VOSK</b>

## Resources

* [VOSK Speech Recognition Toolkit](https://github.com/alphacep/vosk-api)
* [VOSK Models](https://alphacephei.com/vosk/models)
* [VOSK Demo APK (en)](https://github.com/alphacep/vosk-android-demo/releases/download/2021-04/vosk-android-demo-0.3.23.apk)

## Installation

To use this library in your project add this to your build.gradle:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
    implementation 'com.github.am3n:PersianASR:NEWEST-VERSION'
}
```

Add this lines to the `proguard-rules.pro`

```
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
```

### Different languages

If you want to load another language model, just download the 
model from [VOSK Models](https://alphacephei.com/vosk/models) 
and place it in your project assets folder with the name 
`asr-model` and then **clean & rebuild**.

See the [sample app](https://github.com/am3n/PersianASR/tree/a060923df13aaa925b7be1e871e573698af091c7/app/src/main)