buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build", "gradle", "8.0.1")
        classpath("org.jetbrains.kotlin", "kotlin-gradle-plugin", "1.8.21")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
