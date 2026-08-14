# Keep Gemini / generative AI models
-keep class com.google.ai.client.generativeai.** { *; }

# Keep Room entities
-keep class com.jarvis.mark39.data.local.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
