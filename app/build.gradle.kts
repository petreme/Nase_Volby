import java.awt.image.BufferedImage
import java.awt.Color
import java.awt.RenderingHints
import java.util.ArrayDeque
import javax.imageio.ImageIO

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.mojavolba.kymcrv"
    minSdk = 24
    targetSdk = 36
    versionCode = 17
    versionName = "17.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("generateCleanTransparentLogo") {
  val srcLogo = layout.projectDirectory.file("src/main/res/drawable/nase_volby_clean_logo_1780731853180.png")
  val destLogo = layout.projectDirectory.file("src/main/res/drawable/ic_app_icon_fg_clean.png")
  val destSplash = layout.projectDirectory.file("src/main/res/drawable/ic_splash_logo_clean.png")
  
  inputs.file(srcLogo).withPropertyName("srcLogo")
  outputs.file(destLogo).withPropertyName("destLogo")
  outputs.file(destSplash).withPropertyName("destSplash")
  
  val srcFile = srcLogo.asFile
  val destFile = destLogo.asFile
  val dSplashFile = destSplash.asFile
  
  doLast {
    if (srcFile.exists()) {
      val img = ImageIO.read(srcFile) ?: throw GradleException("Could not read logo image")
      val width: Int = img.width
      val height: Int = img.height
      
      val outImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val pixels = IntArray(width * height)
      img.getRGB(0, 0, width, height, pixels, 0, width)
      
      // Clear outer white / off-white borders using robust connected component flood fill starting from margins
      val visited = BooleanArray(width * height)
      val queue = ArrayDeque<Int>()
      
      val marginX: Int = (width * 0.15f).toInt().coerceAtLeast(10)
      val marginY: Int = (height * 0.15f).toInt().coerceAtLeast(10)
      
      fun push(x: Int, y: Int) {
        if (x in 0 until width && y in 0 until height) {
          val idx: Int = y * width + x
          if (!visited[idx]) {
            visited[idx] = true
            queue.add(idx)
          }
        }
      }
      
      for (x in 0 until width) {
        for (y in 0..marginY) {
          val color = pixels[y * width + x]
          val r = (color shr 16) and 0xFF
          val g = (color shr 8) and 0xFF
          val b = color and 0xFF
          val a = (color shr 24) and 0xFF
          if ((r > 165 && g > 165 && b > 165) || (r < 55 && g < 55 && b < 55) || a < 120) {
            push(x, y)
          }
        }
        for (y in (height - marginY) until height) {
          val color = pixels[y * width + x]
          val r = (color shr 16) and 0xFF
          val g = (color shr 8) and 0xFF
          val b = color and 0xFF
          val a = (color shr 24) and 0xFF
          if ((r > 165 && g > 165 && b > 165) || (r < 55 && g < 55 && b < 55) || a < 120) {
            push(x, y)
          }
        }
      }
      for (y in 0 until height) {
        for (x in 0..marginX) {
          val color = pixels[y * width + x]
          val r = (color shr 16) and 0xFF
          val g = (color shr 8) and 0xFF
          val b = color and 0xFF
          val a = (color shr 24) and 0xFF
          if ((r > 165 && g > 165 && b > 165) || (r < 55 && g < 55 && b < 55) || a < 120) {
            push(x, y)
          }
        }
        for (x in (width - marginX) until width) {
          val color = pixels[y * width + x]
          val r = (color shr 16) and 0xFF
          val g = (color shr 8) and 0xFF
          val b = color and 0xFF
          val a = (color shr 24) and 0xFF
          if ((r > 165 && g > 165 && b > 165) || (r < 55 && g < 55 && b < 55) || a < 120) {
            push(x, y)
          }
        }
      }
      
      val bgSet = BooleanArray(width * height)
      while (queue.size > 0) {
        val idx: Int = queue.poll() ?: break
        bgSet[idx] = true
        val x: Int = idx % width
        val y: Int = idx / width
        
        val color = pixels[idx]
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val a = (color shr 24) and 0xFF
        
        val isBg = (r > 165 && g > 165 && b > 165) || (r < 55 && g < 55 && b < 55) || a < 120
        if (isBg) {
          if (x + 1 < width && !visited[y * width + (x + 1)]) {
            visited[y * width + (x + 1)] = true
            queue.add(y * width + (x + 1))
          }
          if (x - 1 >= 0 && !visited[y * width + (x - 1)]) {
            visited[y * width + (x - 1)] = true
            queue.add(y * width + (x - 1))
          }
          if (y + 1 < height && !visited[(y + 1) * width + x]) {
            visited[(y + 1) * width + x] = true
            queue.add((y + 1) * width + x)
          }
          if (y - 1 >= 0 && !visited[(y - 1) * width + x]) {
            visited[(y - 1) * width + x] = true
            queue.add((y - 1) * width + x)
          }
        }
      }
      
      for (i in pixels.indices) {
        if (bgSet[i]) {
          pixels[i] = 0x00000000
        }
      }
      
      outImage.setRGB(0, 0, width, height, pixels, 0, width)
      
      // Save full cleaned image for Splash screen
      ImageIO.write(outImage, "png", dSplashFile)
      logger.lifecycle("Generated clean transparent splash logo at: ${dSplashFile.absolutePath}")
      
      // Now detect entire non-transparent logo content, crop and scale it centered on 512x512 canvas for App Icon
      var minLogoX = width
      var maxLogoX = 0
      var minLogoY = height
      var maxLogoY = 0
      for (y in 0 until height) {
        for (x in 0 until width) {
          val color = pixels[y * width + x]
          val a = (color shr 24) and 0xFF
          if (a > 10) {
            if (x < minLogoX) minLogoX = x
            if (x > maxLogoX) maxLogoX = x
            if (y < minLogoY) minLogoY = y
            if (y > maxLogoY) maxLogoY = y
          }
        }
      }
      
      val targetSize = 512
      val canvas = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
      val g = canvas.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      
      if (maxLogoX > minLogoX && maxLogoY > minLogoY) {
        val logoW = maxLogoX - minLogoX + 1
        val logoH = maxLogoY - minLogoY + 1
        val padX = (logoW * 0.05).toInt().coerceAtLeast(4)
        val padY = (logoH * 0.05).toInt().coerceAtLeast(4)
        val cropX = (minLogoX - padX).coerceAtLeast(0)
        val cropY = (minLogoY - padY).coerceAtLeast(0)
        val cropW = (maxLogoX + padX).coerceAtMost(width - 1) - cropX + 1
        val cropH = (maxLogoY + padY).coerceAtMost(height - 1) - cropY + 1
        
        val cropped = outImage.getSubimage(cropX, cropY, cropW, cropH)
        val maxTargetDim = (targetSize * 0.66).toInt() // 337 pixels (66% safe radius)
        val scale = Math.min(maxTargetDim.toDouble() / cropW, maxTargetDim.toDouble() / cropH)
        val drawW = (cropW * scale).toInt()
        val drawH = (cropH * scale).toInt()
        val drawX = (targetSize - drawW) / 2
        val drawY = (targetSize - drawH) / 2
        
        g.drawImage(cropped, drawX, drawY, drawW, drawH, null)
      } else {
        // Fallback
        val maxTargetDim = (targetSize * 0.66).toInt()
        val scale = Math.min(maxTargetDim.toDouble() / width, maxTargetDim.toDouble() / height)
        val drawW = (width * scale).toInt()
        val drawH = (height * scale).toInt()
        val drawX = (targetSize - drawW) / 2
        val drawY = (targetSize - drawH) / 2
        g.drawImage(outImage, drawX, drawY, drawW, drawH, null)
      }
      g.dispose()
      
      ImageIO.write(canvas, "png", destFile)
      logger.lifecycle("Generated clean transparent centered shield app icon at: ${destFile.absolutePath}")
    }
  }
}

tasks.register("generateMipmapPngIcons") {
  dependsOn("generateCleanTransparentLogo")
  val srcLogo = layout.projectDirectory.file("src/main/res/drawable/ic_app_icon_fg_clean.png")
  inputs.file(srcLogo).withPropertyName("srcLogo")
  
  val densities = mapOf(
    "mdpi" to 48,
    "hdpi" to 72,
    "xhdpi" to 96,
    "xxhdpi" to 144,
    "xxxhdpi" to 192
  )
  
  val destDirsAndSizes = densities.map { (density, size) ->
    layout.projectDirectory.dir("src/main/res/mipmap-$density").asFile to size
  }
  
  val srcLogoFile = srcLogo.asFile
  
  doLast {
    if (srcLogoFile.exists()) {
      val fgImg = ImageIO.read(srcLogoFile) ?: throw GradleException("Could not read clean transparent logo")
      
      destDirsAndSizes.forEach { (dir, size) ->
        dir.mkdirs()
        
        val scaled = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        
        // Solid deep blue `#0B2240`
        g.color = Color(11, 34, 64)
        g.fillRect(0, 0, size, size)
        
        // Draw the shield (fgImg is already 512x512 centered) scaled to the full legacy size
        g.drawImage(fgImg, 0, 0, size, size, null)
        g.dispose()
        
        ImageIO.write(scaled, "png", File(dir, "ic_launcher.png"))
        ImageIO.write(scaled, "png", File(dir, "ic_launcher_round.png"))
      }
      logger.lifecycle("Successfully updated legacy launcher icons with background merge!")
    }
  }
}

tasks.named("preBuild") {
  dependsOn("generateCleanTransparentLogo", "generateMipmapPngIcons")
}

tasks.register("copyApkToRoot") {
  val srcFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
  val destFile = layout.projectDirectory.file("../MojaVolba.apk")
  
  inputs.file(srcFile).withPropertyName("srcFile").optional()
  outputs.file(destFile).withPropertyName("destFile")

  val sFile = srcFile.get().asFile
  val dFile = destFile.asFile

  doLast {
    if (sFile.exists()) {
      sFile.copyTo(dFile, overwrite = true)
      logger.lifecycle("Successfully copied compiled APK to: ${dFile.absolutePath} (${dFile.length()} bytes)")
    } else {
      logger.error("Source APK not found at: ${sFile.absolutePath}")
    }
  }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
  finalizedBy("copyApkToRoot")
}

