# ProGuard rules for VideoEnhancer

# Keep TensorFlow Lite model classes
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }

# Keep Media3 classes
-keep class androidx.media3.** { *; }

# Keep data classes
-keep class com.videoenhancer.viewmodel.** { *; }
