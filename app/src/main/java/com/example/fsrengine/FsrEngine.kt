package com.example.fsrengine

import android.graphics.PointF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 1. Definition of dynamic, adjustable resolutions
class ResolutionConfig(
    val id: String,
    initialLabel: String,
    initialWidth: Int,
    initialHeight: Int,
    initialTargetWidth: Int = 1280,
    initialTargetHeight: Int = 720,
    val isCustom: Boolean = false
) {
    var label by mutableStateOf(initialLabel)
    var width by mutableStateOf(initialWidth)
    var height by mutableStateOf(initialHeight)
    var targetWidth by mutableStateOf(initialTargetWidth)
    var targetHeight by mutableStateOf(initialTargetHeight)
}

enum class UpscaleMode(val label: String, val description: String) {
    NATIVE_LOW("Nearest Neighbor", "Échelle brute sans filtre. Rendu pixelisé, scintillement extrême (aliasing spatial et temporel)."),
    BILINEAR("Bilinear standard", "Interpolation linéaire simple. Supprime la pixelisation mais crée un flou intense (« bouillie de pixels »)."),
    FSR_1_0("FSR 1.0 (Spatial)", "Algorithme EASU de reconstruction de gradients spatiaux + RCAS (Ajustement de netteté adaptatif au contraste)."),
    FSR_2_0("FSR 2.0 (Temporel)", "Reconstruction temporelle complexe. Utilise un historique de frames avec sous-pixel jittering et vecteurs de mouvement pour récupérer les détails sub-pixel.")
}

enum class FrameGenMode(val label: String, val scale: Int) {
    OFF("Désactivé (1x)", 1),
    LSFG_X2("LSFG x2 (Frame Gen)", 2),
    LSFG_X3("LSFG x3 (Frame Gen)", 3)
}

// 3D Point for our scene
data class Point3D(var x: Float, var y: Float, var z: Float) {
    fun rotateX(angle: Float): Point3D {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val ny = y * cosA - z * sinA
        val nz = y * sinA + z * cosA
        return Point3D(x, ny, nz)
    }

    fun rotateY(angle: Float): Point3D {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val nx = x * cosA + z * sinA
        val nz = -x * sinA + z * cosA
        return Point3D(nx, y, nz)
    }

    fun rotateZ(angle: Float): Point3D {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val nx = x * cosA - y * sinA
        val ny = x * sinA + y * cosA
        return Point3D(nx, ny, z)
    }
}

// Representing 3D lines
data class Line3D(val start: Point3D, val end: Point3D, val colorType: Int)

// Engine State class to be observed in View
class FsrEngine {
    // Dynamic Resolution database
    val availableResolutions = mutableStateListOf<ResolutionConfig>(
        ResolutionConfig("120p", "120p (Ultra Performance)", 213, 120, 1280, 720),
        ResolutionConfig("240p", "240p (Performance)", 426, 240, 1280, 720),
        ResolutionConfig("360p", "360p (Balanced)", 640, 360, 1280, 720),
        ResolutionConfig("480p", "480p (Quality)", 854, 480, 1280, 720),
        ResolutionConfig("720p", "720p (Native/Standard)", 1280, 720, 1280, 720),
        ResolutionConfig("960p", "960p (QHD Light)", 1706, 960, 1706, 960),
        ResolutionConfig("1080p", "1080p (Full HD)", 1920, 1080, 1920, 1080),
        ResolutionConfig("1440p", "1440p (Quad HD)", 2560, 1440, 2560, 1440)
    )

    // Current Configurations
    var sourceResolution by mutableStateOf(availableResolutions[1])
    var upscaleMode by mutableStateOf(UpscaleMode.FSR_2_0)
    var frameGenMode by mutableStateOf(FrameGenMode.LSFG_X2)
    var adaptiveFrameGen by mutableStateOf(true)
    var rcasSharpness by mutableStateOf(0.75f) // 0 to 1
    var baseFps by mutableStateOf(30) // 10, 15, 30, 45
    var gpuLoadSlider by mutableStateOf(40f) // 0 to 100%
    
    // Graphic tweaks
    var dynamicResolutionScaling by mutableStateOf(true)
    var variableRateShading by mutableStateOf(true)
    var foveatedVringEnabled by mutableStateOf(true)

    // Undervolting configurations (Without changing core frequencies)
    var cpuUndervoltEnabled by mutableStateOf(false)
    var cpuVoltOffsetMv by mutableStateOf(-80) // in mV, e.g. -50 to -150 mV
    var gpuUndervoltEnabled by mutableStateOf(false)
    var gpuVoltOffsetMv by mutableStateOf(-60) // in mV, e.g. -40 to -120 mV

    // Core Overclocking configurations (Allows custom core-by-core clock increases)
    var cpuOverclockEnabled by mutableStateOf(false)
    var cpuCoreFreqGhz by mutableStateOf(2.8f) // Range: 1.8f to 4.2f Ghz (Default base is 2.2f Ghz)
    var gpuOverclockEnabled by mutableStateOf(false)
    var gpuClockMhz by mutableStateOf(850) // Range: 500 to 1200 Mhz (Default base is 750 Mhz)

    // Modern Advanced Ultra RAM Compressor & Memory Compactor
    var ramCompressorEnabled by mutableStateOf(true)
    var ramCompressionAlgo by mutableStateOf("ZRAM (LZ4-compaction)") // ZRAM (LZ4-compaction), ZSTD-Ultra, LZO-Concurrent, Double-Huffman
    var rawMemorySizeGb by mutableStateOf(16.0f)
    var compressedMemorySizeGb by mutableStateOf(4.8f) // compressed footprint
    var decompressionLatencyMs by mutableStateOf(0.04f)
    var memoryCompactionCycles by mutableStateOf(14)

    // NEW: Modern High Tech ... GPU Texture Compressor
    var gpuTextureCompressorEnabled by mutableStateOf(true)
    var gpuTextureCompressionFormat by mutableStateOf("BC7 (Normale/Qualité)") // BC7 (Normale/Qualité), ASTC 4x4, ETC2 (Mobile), Wavlet Adaptive Huffman (Ultra)
    var compressedTextureMemorySizeMb by mutableStateOf(512f)
    var textureBandwidthSavedPct by mutableStateOf(75f)

    // NEW: Per-App Adjustable GPU Video Memory (VRAM) & Ultra Compression Core
    var gpuVideoMemorySizeMb by mutableStateOf(1024) // 128 to 4048 Mo
    var gpuVramCompressionEnabled by mutableStateOf(true)
    var gpuVramCompressionLevel by mutableStateOf("Extrême LZW-H") // "Standard (Sans perte)", "Élevée (Lossy 2:1)", "Extrême LZW-H (Highly Compressed 3:1)", "Ultra-Noyau Quantique (4:1 Compaction)"
    var activeVramFootprintMb by mutableStateOf(310f)

    // NEW: Extreme Automatic Background App Manager & Real-Time RAM Allocator
    var autoBackgroundManagerEnabled by mutableStateOf(true)
    var backgroundKillAgressiveness by mutableStateOf("Coercitif Automatique (Extrême)") // "Sélectif Léger", "Agressif Modéré", "Coercitif Automatique (Extrême)", "Z-Noyau Quantique Brut (Kill Tout)"
    var ramReallocatedToActiveAppMb by mutableStateOf(3450) // Mo
    var systemBackgroundProcessesKilled by mutableStateOf(10)
    var backgroundManagerLog by mutableStateOf("✓ Initialisé en arrière-plan. Veille active intelligente OOM.")
    var extremeAutoMemoryReclaimRate by mutableStateOf(85f)
    var activeAppRamPriorityLevel by mutableStateOf("Priorité Temps Réel")

    // NEW: Anti-Aliasing (AA) & Anisotropic Filtering (AF)
    var anisotropicLevel by mutableStateOf(16) // 1, 2, 4, 8, 16
    var antiAliasingMode by mutableStateOf("TAA (Temporel)") // Aucun, FXAA, SMAA, TAA (Temporel), DSAA (Deep Learning AA), MSAA 4x
    var aaSampleCount by mutableStateOf(4) // 2, 4, 8

    // NEW: Multi-Core Scheduling & Processing (CPU & GPU)
    var multiQueueGpuComputing by mutableStateOf(true) // Async Compute overlapping
    var multiCoreCpuDispatch by mutableStateOf(true) // Multi-threaded task scheduling
    var cpuWorkerThreads by mutableStateOf(8) // 2 to 32
    var gpuExecutionPipelines by mutableStateOf(8) // 2 to 16

    // NEW: Hardware Thermal & Temperature Limiter
    var thermalLimitCelsius by mutableStateOf(85) // 60 to 105
    var cpuThermalThrottlingEnabled by mutableStateOf(true)
    var gpuThermalThrottlingEnabled by mutableStateOf(true)
    var thermalThrottlingActive by mutableStateOf(false)
    
    // NEW: Adjustable TDP (Thermal Design Power) limits
    var cpuTdpWatts by mutableStateOf(35f) // 5W to 120W
    var gpuTdpWatts by mutableStateOf(45f) // 5W to 150W
    var dualPowerBoosterEnabled by mutableStateOf(true) // Dynamic power allocation (AMD SmartShift / Intel Dynamic Tuning)
    var powerProfileMode by mutableStateOf("Configurée (Custom)") // "Éco (15W/20W)", "Équilibrée (35W/45W)", "Performance (65W/80W)", "Configurée (Custom)"
    var cpuPowerDrawMw by mutableStateOf(1600f) // Realtime CPU calculation
    var cpuTdpThrottledActive by mutableStateOf(false)
    var gpuTdpThrottledActive by mutableStateOf(false)
    
    // NEW: Multi-architecture & SDK Compatibility parameters
    var targetIsaArchitecture by mutableStateOf("ARMv9-A (Modern 64-bit)") // "ARMv9-A (Modern 64-bit)", "ARM64-v8a (ARMv8 64-bit)", "ARMv7-A (32-bit legacy)", "x86_64 (Intel/AMD)", "x86 (Legacy Emulator)"
    var targetAndroidSdkVersion by mutableStateOf("Android 14/15/16 (API 34-36)") //  "Android 14/15/16 (API 34-36)", "Android 10 - 13 (API 29-33)", "Android 7.0 - 9.0 (API 24-28)", "Android 5.0 - 6.0 (API 21-23)"
    var ndkAbiFiltersEnabled by mutableStateOf(true)
    var linkerLtoOptimizations by mutableStateOf(true)
    
    // NEW: VM Heap & Runtime Optimization Parameters
    var vmHeapSize by mutableStateOf("256M") // "128M", "256M", "512M", "1024M"
    var largeHeapEnabled by mutableStateOf(true)
    var hardwareAcceleratedEnabled by mutableStateOf(true)

    // NEW: Extreme & Modern GOUVERNEURS (CPU/GPU)
    var cpuGovernor by mutableStateOf("Schedutil (Réactif)") // "Performance (Extrême)", "Schedutil (Réactif)", "Interactive (Moderne)", "Conservative (Éco)", "Powersave (Ultra-Éco)", "Lulzactive"
    var gpuGovernor by mutableStateOf("Devfreq-Ondemand") // "Performance (Extrême)", "Simple-Ondemand", "Devfreq-Ondemand", "Adreno-Boost", "Conservative", "Powersave"

    // NEW: CPU Core-by-Core adjustments (4 Big, 4 Little)
    var cpuBigCore0Enabled by mutableStateOf(true)
    var cpuBigCore0FreqGhz by mutableStateOf(2.8f) // 1.5 to 4.2 GHz
    var cpuBigCore1Enabled by mutableStateOf(true)
    var cpuBigCore1FreqGhz by mutableStateOf(2.8f)
    var cpuBigCore2Enabled by mutableStateOf(true)
    var cpuBigCore2FreqGhz by mutableStateOf(2.8f)
    var cpuBigCore3Enabled by mutableStateOf(true)
    var cpuBigCore3FreqGhz by mutableStateOf(2.8f)

    var cpuLittleCore4Enabled by mutableStateOf(true)
    var cpuLittleCore4FreqGhz by mutableStateOf(1.8f) // 0.8 to 2.4 GHz
    var cpuLittleCore5Enabled by mutableStateOf(true)
    var cpuLittleCore5FreqGhz by mutableStateOf(1.8f)
    var cpuLittleCore6Enabled by mutableStateOf(true)
    var cpuLittleCore6FreqGhz by mutableStateOf(1.8f)
    var cpuLittleCore7Enabled by mutableStateOf(true)
    var cpuLittleCore7FreqGhz by mutableStateOf(1.8f)

    // NEW: Battery Optimizer (Autonomiser) Modern & Extreme
    var batteryOptimizerMode by mutableStateOf("Équilibré Auto") // "Ultra-Low Power (Extrême)", "Éco Ultra-Économique", "Équilibré Auto", "Haute Performance", "Extrême High Power (Overlimit)"
    var batteryBypassChargingEnabled by mutableStateOf(true)
    var batteryHighTempThresholdAlert by mutableStateOf(42) // 35 to 55 °C
    var batteryHealthStatus by mutableStateOf("Excellent (Refroidissement Actif)")
    var batteryCurrentDrawMa by mutableStateOf(-320)
    var batteryEstimatedRemainingHours by mutableStateOf(8.5f)

    // NEW: FPS LIMITER (30 to 240 FPS and Custom)
    var fpsLimiterEnabled by mutableStateOf(false)
    var fpsLimitPreset by mutableStateOf("Illimité") // "30 FPS", "60 FPS", "120 FPS", "144 FPS", "240 FPS", "Personnalisé (Custom)"
    var fpsLimitValue by mutableStateOf(120) // Custom limit value from 30 to 240 FPS

    // NEW: TWEAKS EXTRÊMES ET COMPATIBILITÉ CHIPSET MULTI-MARQUES
    var selectedHardwareVendor by mutableStateOf("Détection Automatique (Intelligente)")
    var brandTweakActive by mutableStateOf(true)
    var vulkanRamdiskCacheEnabled by mutableStateOf(false)
    var easBypassActive by mutableStateOf(false)
    var turboStoragePipelineEnabled by mutableStateOf(false)
    var tcpCongestionControl by mutableStateOf("BBR-v3 (Ultra Latency)")
    
    // NEW: Access Privileges (No Root, Shizuku, Root)
    enum class AccessMode(val label: String, val description: String, val icon: String) {
        NO_ROOT("Sans Root (Standard)", "Optimisations logicielles via API locales et overlays émulés.", "📱"),
        SHIZUKU("Shizuku API (ADB)", "Performance accrue sans Root via le serveur de débogage Shizuku.", "🛰️"),
        ROOT("Root (Super-Utilisateur)", "Contrôle hardware total : TDP réels, injection kernel et débridage thermique.", "⚡")
    }

    var systemAccessMode by mutableStateOf(AccessMode.NO_ROOT)
    var isShizukuConnected by mutableStateOf(false)
    var isRootGranted by mutableStateOf(false)
    val accessShellDiagnosticsLogs = androidx.compose.runtime.mutableStateListOf<String>(
        "[init] Engin FSR initialisé en mode standard Sans Root.",
        "[init] Tampon de rendu framebuffer mappé avec succès."
    )

    // Access mode specific controls
    var forceAggressiveCpuGovernor by mutableStateOf(false)
    var useRishShellService by mutableStateOf(true)
    var floatingAdbOverlayEnabled by mutableStateOf(true)

    fun logShellCommand(commandText: String, outputText: String) {
        if (accessShellDiagnosticsLogs.size > 30) {
            accessShellDiagnosticsLogs.removeAt(0)
        }
        accessShellDiagnosticsLogs.add("• $commandText")
        if (outputText.isNotEmpty()) {
            accessShellDiagnosticsLogs.add("  ➜ $outputText")
        }
    }
    
    // Viewport Overlays
    var showMotionVectors by mutableStateOf(false)
    var showJitterGrid by mutableStateOf(false)
    var showArtifactMask by mutableStateOf(false)
    var showSplitScreen by mutableStateOf(true)
    var splitRatio by mutableStateOf(0.5f) // split ratio

    // Real-Time Stats (Updated dynamically)
    var activeSourceResolution by mutableStateOf(availableResolutions[1])
    var actualRenderFps by mutableStateOf(30f)
    var interpolatedFps by mutableStateOf(60f)
    var renderTimeMs by mutableStateOf(5.4f)
    var frameGenTimeMs by mutableStateOf(2.1f)
    var totalLatencyMs by mutableStateOf(16.5f)
    var gpuPowerDrawMw by mutableStateOf(1800f)
    var gpuTempCelsius by mutableStateOf(48f)
    var staggerRatePercent by mutableStateOf(1.2f)
    var autoThrottledActive by mutableStateOf(false)
    var frameGenThrottledActive by mutableStateOf(false)

    // --- PER-APP PROFILE MANAGER DATA STRUCTURES & LOGIC ---
    data class InstalledApp(
        val name: String,
        val packageName: String,
        val type: String, // "Jeu" or "Application"
        val defaultCategory: String, // e.g. "Aventure RPG", "Action FPS"
        val developer: String,
        val iconEmoji: String,
        var hasCustomProfile: Boolean = false
    )

    data class BackgroundProcess(
        val name: String,
        val packageName: String,
        val memoryMb: Int,
        val importance: String, // "Critique", "Moyenne", "Inutile"
        var active: Boolean = true
    )

    class AppProfile(
        var upscaleMode: UpscaleMode = UpscaleMode.FSR_2_0,
        var frameGenMode: FrameGenMode = FrameGenMode.LSFG_X2,
        var adaptiveFrameGen: Boolean = true,
        var rcasSharpness: Float = 0.75f,
        var baseFps: Int = 30,
        var gpuLoadSlider: Float = 40f,
        var dynamicResolutionScaling: Boolean = true,
        var variableRateShading: Boolean = true,
        var foveatedVringEnabled: Boolean = true,
        
        var cpuUndervoltEnabled: Boolean = false,
        var cpuVoltOffsetMv: Int = -80,
        var gpuUndervoltEnabled: Boolean = false,
        var gpuVoltOffsetMv: Int = -60,
        
        var cpuOverclockEnabled: Boolean = false,
        var cpuCoreFreqGhz: Float = 2.8f,
        var gpuOverclockEnabled: Boolean = false,
        var gpuClockMhz: Int = 850,
        
        var ramCompressorEnabled: Boolean = true,
        var ramCompressionAlgo: String = "ZRAM (LZ4-compaction)",
        
        var gpuTextureCompressorEnabled: Boolean = true,
        var gpuTextureCompressionFormat: String = "BC7 (Normale/Qualité)",
        
        var anisotropicLevel: Int = 16,
        var antiAliasingMode: String = "TAA (Temporel)",
        var aaSampleCount: Int = 4,
        
        var multiQueueGpuComputing: Boolean = true,
        var multiCoreCpuDispatch: Boolean = true,
        var cpuWorkerThreads: Int = 8,
        var gpuExecutionPipelines: Int = 8,
        
        var thermalLimitCelsius: Int = 85,
        var cpuThermalThrottlingEnabled: Boolean = true,
        var gpuThermalThrottlingEnabled: Boolean = true,
        
        var cpuTdpWatts: Float = 35f,
        var gpuTdpWatts: Float = 45f,
        var dualPowerBoosterEnabled: Boolean = true,
        var powerProfileMode: String = "Configurée (Custom)",
        
        var targetIsaArchitecture: String = "ARMv9-A (Modern 64-bit)",
        var targetAndroidSdkVersion: String = "Android 14/15/16 (API 34-36)",
        var ndkAbiFiltersEnabled: Boolean = true,
        var linkerLtoOptimizations: Boolean = true,
        
        var vmHeapSize: String = "256M",
        var largeHeapEnabled: Boolean = true,
        var hardwareAcceleratedEnabled: Boolean = true,
        
        var gpuVideoMemorySizeMb: Int = 1024,
        var gpuVramCompressionEnabled: Boolean = true,
        var gpuVramCompressionLevel: String = "Extrême LZW-H",
        
        var cpuGovernor: String = "Schedutil (Réactif)",
        var gpuGovernor: String = "Devfreq-Ondemand",
        var cpuBigCore0Enabled: Boolean = true,
        var cpuBigCore0FreqGhz: Float = 2.8f,
        var cpuBigCore1Enabled: Boolean = true,
        var cpuBigCore1FreqGhz: Float = 2.8f,
        var cpuBigCore2Enabled: Boolean = true,
        var cpuBigCore2FreqGhz: Float = 2.8f,
        var cpuBigCore3Enabled: Boolean = true,
        var cpuBigCore3FreqGhz: Float = 2.8f,
        var cpuLittleCore4Enabled: Boolean = true,
        var cpuLittleCore4FreqGhz: Float = 1.8f,
        var cpuLittleCore5Enabled: Boolean = true,
        var cpuLittleCore5FreqGhz: Float = 1.8f,
        var cpuLittleCore6Enabled: Boolean = true,
        var cpuLittleCore6FreqGhz: Float = 1.8f,
        var cpuLittleCore7Enabled: Boolean = true,
        var cpuLittleCore7FreqGhz: Float = 1.8f,
        var batteryOptimizerMode: String = "Équilibré Auto",
        var batteryBypassChargingEnabled: Boolean = true,
        var fpsLimiterEnabled: Boolean = false,
        var fpsLimitPreset: String = "Illimité",
        var fpsLimitValue: Int = 120,
        var selectedHardwareVendor: String = "Détection Automatique (Intelligente)",
        var brandTweakActive: Boolean = true,
        var vulkanRamdiskCacheEnabled: Boolean = false,
        var easBypassActive: Boolean = false,
        var turboStoragePipelineEnabled: Boolean = false,
        var tcpCongestionControl: String = "BBR-v3 (Ultra Latency)"
    )

    val installedApps = androidx.compose.runtime.mutableStateListOf<InstalledApp>()
    val backgroundProcesses = androidx.compose.runtime.mutableStateListOf<BackgroundProcess>()
    
    fun killAllBackgroundProcesses() {
        for (i in backgroundProcesses.indices) {
            val proc = backgroundProcesses[i]
            if (proc.importance != "Critique") {
                backgroundProcesses[i] = proc.copy(active = false)
            }
        }
        val sumKilledMb = backgroundProcesses.filter { !it.active }.sumOf { it.memoryMb }
        ramReallocatedToActiveAppMb = sumKilledMb + 1024
        systemBackgroundProcessesKilled = backgroundProcesses.count { !it.active }
        backgroundManagerLog = "✓ OPÉRATION PURGE EXTRÊME : $systemBackgroundProcessesKilled processus inutiles résiliés par signal coercitif. $ramReallocatedToActiveAppMb Mo de mémoire RAM réalloués à la tâche principale active."
    }

    fun restartBackgroundProcess(packageName: String) {
        val index = backgroundProcesses.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val proc = backgroundProcesses[index]
            backgroundProcesses[index] = proc.copy(active = true)
            
            val activeKilled = backgroundProcesses.count { !it.active }
            val sumKilledMb = backgroundProcesses.filter { !it.active }.sumOf { it.memoryMb }
            ramReallocatedToActiveAppMb = if (activeKilled > 0) sumKilledMb + 1024 else 0
            systemBackgroundProcessesKilled = activeKilled
            backgroundManagerLog = "Relance manuelle du processus : ${proc.name}. RAM active réallouée ajustée à $ramReallocatedToActiveAppMb Mo."
        }
    }
    
    fun manualKillBackgroundProcess(packageName: String) {
        val index = backgroundProcesses.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val proc = backgroundProcesses[index]
            backgroundProcesses[index] = proc.copy(active = false)
            
            val activeKilled = backgroundProcesses.count { !it.active }
            val sumKilledMb = backgroundProcesses.filter { !it.active }.sumOf { it.memoryMb }
            ramReallocatedToActiveAppMb = sumKilledMb + 1024
            systemBackgroundProcessesKilled = activeKilled
            backgroundManagerLog = "✓ Force-Kill : ${proc.name} fermé d'autorité. ${proc.memoryMb} Mo supplémentaires récupérés."
        }
    }

    val appProfiles = androidx.compose.runtime.mutableStateMapOf<String, AppProfile>()
    var globalProfile = AppProfile()
    var currentActivePackageName by mutableStateOf("global")

    fun saveActiveStateToProfile(profile: AppProfile) {
        profile.upscaleMode = upscaleMode
        profile.frameGenMode = frameGenMode
        profile.adaptiveFrameGen = adaptiveFrameGen
        profile.rcasSharpness = rcasSharpness
        profile.baseFps = baseFps
        profile.gpuLoadSlider = gpuLoadSlider
        profile.dynamicResolutionScaling = dynamicResolutionScaling
        profile.variableRateShading = variableRateShading
        profile.foveatedVringEnabled = foveatedVringEnabled
        
        profile.cpuUndervoltEnabled = cpuUndervoltEnabled
        profile.cpuVoltOffsetMv = cpuVoltOffsetMv
        profile.gpuUndervoltEnabled = gpuUndervoltEnabled
        profile.gpuVoltOffsetMv = gpuVoltOffsetMv
        
        profile.cpuOverclockEnabled = cpuOverclockEnabled
        profile.cpuCoreFreqGhz = cpuCoreFreqGhz
        profile.gpuOverclockEnabled = gpuOverclockEnabled
        profile.gpuClockMhz = gpuClockMhz
        
        profile.ramCompressorEnabled = ramCompressorEnabled
        profile.ramCompressionAlgo = ramCompressionAlgo
        
        profile.gpuTextureCompressorEnabled = gpuTextureCompressorEnabled
        profile.gpuTextureCompressionFormat = gpuTextureCompressionFormat
        
        profile.anisotropicLevel = anisotropicLevel
        profile.antiAliasingMode = antiAliasingMode
        profile.aaSampleCount = aaSampleCount
        
        profile.multiQueueGpuComputing = multiQueueGpuComputing
        profile.multiCoreCpuDispatch = multiCoreCpuDispatch
        profile.cpuWorkerThreads = cpuWorkerThreads
        profile.gpuExecutionPipelines = gpuExecutionPipelines
        
        profile.thermalLimitCelsius = thermalLimitCelsius
        profile.cpuThermalThrottlingEnabled = cpuThermalThrottlingEnabled
        profile.gpuThermalThrottlingEnabled = gpuThermalThrottlingEnabled
        
        profile.cpuTdpWatts = cpuTdpWatts
        profile.gpuTdpWatts = gpuTdpWatts
        profile.dualPowerBoosterEnabled = dualPowerBoosterEnabled
        profile.powerProfileMode = powerProfileMode
        
        profile.targetIsaArchitecture = targetIsaArchitecture
        profile.targetAndroidSdkVersion = targetAndroidSdkVersion
        profile.ndkAbiFiltersEnabled = ndkAbiFiltersEnabled
        profile.linkerLtoOptimizations = linkerLtoOptimizations
        
        profile.vmHeapSize = vmHeapSize
        profile.largeHeapEnabled = largeHeapEnabled
        profile.hardwareAcceleratedEnabled = hardwareAcceleratedEnabled
        profile.gpuVideoMemorySizeMb = gpuVideoMemorySizeMb
        profile.gpuVramCompressionEnabled = gpuVramCompressionEnabled
        profile.gpuVramCompressionLevel = gpuVramCompressionLevel
        
        profile.cpuGovernor = cpuGovernor
        profile.gpuGovernor = gpuGovernor
        profile.cpuBigCore0Enabled = cpuBigCore0Enabled
        profile.cpuBigCore0FreqGhz = cpuBigCore0FreqGhz
        profile.cpuBigCore1Enabled = cpuBigCore1Enabled
        profile.cpuBigCore1FreqGhz = cpuBigCore1FreqGhz
        profile.cpuBigCore2Enabled = cpuBigCore2Enabled
        profile.cpuBigCore2FreqGhz = cpuBigCore2FreqGhz
        profile.cpuBigCore3Enabled = cpuBigCore3Enabled
        profile.cpuBigCore3FreqGhz = cpuBigCore3FreqGhz
        profile.cpuLittleCore4Enabled = cpuLittleCore4Enabled
        profile.cpuLittleCore4FreqGhz = cpuLittleCore4FreqGhz
        profile.cpuLittleCore5Enabled = cpuLittleCore5Enabled
        profile.cpuLittleCore5FreqGhz = cpuLittleCore5FreqGhz
        profile.cpuLittleCore6Enabled = cpuLittleCore6Enabled
        profile.cpuLittleCore6FreqGhz = cpuLittleCore6FreqGhz
        profile.cpuLittleCore7Enabled = cpuLittleCore7Enabled
        profile.cpuLittleCore7FreqGhz = cpuLittleCore7FreqGhz
        profile.batteryOptimizerMode = batteryOptimizerMode
        profile.batteryBypassChargingEnabled = batteryBypassChargingEnabled
        profile.fpsLimiterEnabled = fpsLimiterEnabled
        profile.fpsLimitPreset = fpsLimitPreset
        profile.fpsLimitValue = fpsLimitValue
        profile.selectedHardwareVendor = selectedHardwareVendor
        profile.brandTweakActive = brandTweakActive
        profile.vulkanRamdiskCacheEnabled = vulkanRamdiskCacheEnabled
        profile.easBypassActive = easBypassActive
        profile.turboStoragePipelineEnabled = turboStoragePipelineEnabled
        profile.tcpCongestionControl = tcpCongestionControl
    }

    fun loadProfileToActiveState(profile: AppProfile) {
        upscaleMode = profile.upscaleMode
        frameGenMode = profile.frameGenMode
        adaptiveFrameGen = profile.adaptiveFrameGen
        rcasSharpness = profile.rcasSharpness
        baseFps = profile.baseFps
        gpuLoadSlider = profile.gpuLoadSlider
        dynamicResolutionScaling = profile.dynamicResolutionScaling
        variableRateShading = profile.variableRateShading
        foveatedVringEnabled = profile.foveatedVringEnabled
        
        cpuUndervoltEnabled = profile.cpuUndervoltEnabled
        cpuVoltOffsetMv = profile.cpuVoltOffsetMv
        gpuUndervoltEnabled = profile.gpuUndervoltEnabled
        gpuVoltOffsetMv = profile.gpuVoltOffsetMv
        
        cpuOverclockEnabled = profile.cpuOverclockEnabled
        cpuCoreFreqGhz = profile.cpuOverclockEnabled.let { if (it) profile.cpuCoreFreqGhz else cpuCoreFreqGhz }
        gpuOverclockEnabled = profile.gpuOverclockEnabled
        gpuClockMhz = profile.gpuClockMhz
        
        ramCompressorEnabled = profile.ramCompressorEnabled
        ramCompressionAlgo = profile.ramCompressionAlgo
        
        gpuTextureCompressorEnabled = profile.gpuTextureCompressorEnabled
        gpuTextureCompressionFormat = profile.gpuTextureCompressionFormat
        
        anisotropicLevel = profile.anisotropicLevel
        antiAliasingMode = profile.antiAliasingMode
        aaSampleCount = profile.aaSampleCount
        
        multiQueueGpuComputing = profile.multiQueueGpuComputing
        multiCoreCpuDispatch = profile.multiCoreCpuDispatch
        cpuWorkerThreads = profile.cpuWorkerThreads
        gpuExecutionPipelines = profile.gpuExecutionPipelines
        
        thermalLimitCelsius = profile.thermalLimitCelsius
        cpuThermalThrottlingEnabled = profile.cpuThermalThrottlingEnabled
        gpuThermalThrottlingEnabled = profile.gpuThermalThrottlingEnabled
        
        cpuTdpWatts = profile.cpuTdpWatts
        gpuTdpWatts = profile.gpuTdpWatts
        dualPowerBoosterEnabled = profile.dualPowerBoosterEnabled
        powerProfileMode = profile.powerProfileMode
        
        targetIsaArchitecture = profile.targetIsaArchitecture
        targetAndroidSdkVersion = profile.targetAndroidSdkVersion
        ndkAbiFiltersEnabled = profile.ndkAbiFiltersEnabled
        linkerLtoOptimizations = profile.linkerLtoOptimizations
        
        vmHeapSize = profile.vmHeapSize
        largeHeapEnabled = profile.largeHeapEnabled
        hardwareAcceleratedEnabled = profile.hardwareAcceleratedEnabled
        gpuVideoMemorySizeMb = profile.gpuVideoMemorySizeMb
        gpuVramCompressionEnabled = profile.gpuVramCompressionEnabled
        gpuVramCompressionLevel = profile.gpuVramCompressionLevel
        
        cpuGovernor = profile.cpuGovernor
        gpuGovernor = profile.gpuGovernor
        cpuBigCore0Enabled = profile.cpuBigCore0Enabled
        cpuBigCore0FreqGhz = profile.cpuBigCore0FreqGhz
        cpuBigCore1Enabled = profile.cpuBigCore1Enabled
        cpuBigCore1FreqGhz = profile.cpuBigCore1FreqGhz
        cpuBigCore2Enabled = profile.cpuBigCore2Enabled
        cpuBigCore2FreqGhz = profile.cpuBigCore2FreqGhz
        cpuBigCore3Enabled = profile.cpuBigCore3Enabled
        cpuBigCore3FreqGhz = profile.cpuBigCore3FreqGhz
        cpuLittleCore4Enabled = profile.cpuLittleCore4Enabled
        cpuLittleCore4FreqGhz = profile.cpuLittleCore4FreqGhz
        cpuLittleCore5Enabled = profile.cpuLittleCore5Enabled
        cpuLittleCore5FreqGhz = profile.cpuLittleCore5FreqGhz
        cpuLittleCore6Enabled = profile.cpuLittleCore6Enabled
        cpuLittleCore6FreqGhz = profile.cpuLittleCore6FreqGhz
        cpuLittleCore7Enabled = profile.cpuLittleCore7Enabled
        cpuLittleCore7FreqGhz = profile.cpuLittleCore7FreqGhz
        batteryOptimizerMode = profile.batteryOptimizerMode
        batteryBypassChargingEnabled = profile.batteryBypassChargingEnabled
        fpsLimiterEnabled = profile.fpsLimiterEnabled
        fpsLimitPreset = profile.fpsLimitPreset
        fpsLimitValue = profile.fpsLimitValue
        selectedHardwareVendor = profile.selectedHardwareVendor
        brandTweakActive = profile.brandTweakActive
        vulkanRamdiskCacheEnabled = profile.vulkanRamdiskCacheEnabled
        easBypassActive = profile.easBypassActive
        turboStoragePipelineEnabled = profile.turboStoragePipelineEnabled
        tcpCongestionControl = profile.tcpCongestionControl
    }

    fun switchActiveProfile(nextPackageName: String) {
        if (currentActivePackageName == nextPackageName) return

        // 1. Save current active state to its profile
        if (currentActivePackageName == "global") {
            saveActiveStateToProfile(globalProfile)
        } else {
            val prof = appProfiles[currentActivePackageName]
            if (prof != null) {
                saveActiveStateToProfile(prof)
            }
        }

        // 2. Load the new profile
        currentActivePackageName = nextPackageName
        if (nextPackageName == "global") {
            loadProfileToActiveState(globalProfile)
        } else {
            val prof = appProfiles[nextPackageName]
            if (prof != null) {
                loadProfileToActiveState(prof)
            } else {
                // fallback if not configured
                loadProfileToActiveState(globalProfile)
            }
        }
    }

    fun toggleCustomProfileForApp(packageName: String, enable: Boolean) {
        val appIndex = installedApps.indexOfFirst { it.packageName == packageName }
        if (appIndex != -1) {
            val app = installedApps[appIndex]
            val updatedApp = app.copy(hasCustomProfile = enable)
            installedApps[appIndex] = updatedApp
            
            if (enable) {
                val newProfile = AppProfile()
                saveActiveStateToProfile(newProfile)
                appProfiles[packageName] = newProfile
                if (currentActivePackageName == packageName) {
                    loadProfileToActiveState(newProfile)
                }
            } else {
                appProfiles.remove(packageName)
                if (currentActivePackageName == packageName) {
                    loadProfileToActiveState(globalProfile)
                }
            }
        }
    }
    
    // Scene state
    var rotationAngleX = 0f
    var rotationAngleY = 0f
    var rotationAngleZ = 0f
    
    // 3D Objects in the scene
    var rawVertices = ArrayList<Point3D>()
    var rawLines = ArrayList<Line3D>()
    var starfield = ArrayList<Point3D>()
    
    // Halton sequence for subpixel FSR 2.0 jitter simulation
    private val haltonX = floatArrayOf(0.5f, 0.25f, 0.75f, 0.125f, 0.625f, 0.375f, 0.875f, 0.0625f)
    private val haltonY = floatArrayOf(0.333f, 0.667f, 0.111f, 0.444f, 0.778f, 0.222f, 0.556f, 0.889f)
    private var jitterIndex = 0

    init {
        generate3DScene()
        installedApps.addAll(listOf(
            InstalledApp("Genshin Impact", "com.miHoYo.GenshinImpact", "Jeu", "Aventure RPG", "COGNOSPHERE", "⚔️"),
            InstalledApp("Honkai: Star Rail", "com.miHoYo.HSR", "Jeu", "Aventure RPG", "COGNOSPHERE", "☄️"),
            InstalledApp("PUBG Mobile", "com.tencent.ig", "Jeu", "Action FPS", "Tencent Games", "🔫"),
            InstalledApp("Call of Duty: Warzone", "com.activision.callofduty.warzone", "Jeu", "Action FPS", "Activision", "🎖️"),
            InstalledApp("AetherSX2", "com.tahlreth.aethersx2", "Application", "Émulation", "Tahlreth", "👾"),
            InstalledApp("Yuzu Emulator", "org.yuzu.yuzu_emu", "Application", "Émulation", "Yuzu Project", "🎮"),
            InstalledApp("Nomad Sculpt", "com.stephaneginier.nomadsculpt", "Application", "Création & Rendu", "Stephane Ginier", "🎨"),
            InstalledApp("CapCut Premium", "com.lemon.lvoverseas", "Application", "Création & Rendu", "ByteDance", "🎬"),
            InstalledApp("Netflix High-Res", "com.netflix.mediaclient", "Application", "Multimédia", "Netflix Inc.", "🍿")
        ))
        
        backgroundProcesses.addAll(listOf(
            BackgroundProcess("Chrome Render Service", "com.android.chrome:sandboxed_process", 420, "Inutile", active = false),
            BackgroundProcess("WhatsApp Notification Poller", "com.whatsapp:sync", 180, "Moyenne", active = false),
            BackgroundProcess("Facebook App Linker", "com.facebook.katana:services", 520, "Inutile", active = false),
            BackgroundProcess("Instagram Prefetch Feed", "com.instagram.android:background", 610, "Inutile", active = false),
            BackgroundProcess("TikTok Buffet Manager", "com.zhiliaoapp.musically:prefetch", 730, "Inutile", active = false),
            BackgroundProcess("Spotify Sync Cache", "com.spotify.music:service", 290, "Moyenne", active = false),
            BackgroundProcess("Google Telemetry Agent", "com.google.android.gms:persistent", 310, "Moyenne", active = true),
            BackgroundProcess("System Log Dump Service", "com.android.system.logdump", 150, "Inutile", active = false),
            BackgroundProcess("Bluetooth Device Tracker", "com.android.bluetooth:tracker", 80, "Critique", active = true),
            BackgroundProcess("Microsoft Teams Worker", "com.microsoft.teams:sync", 340, "Moyenne", active = false)
        ))
    }

    private fun generate3DScene() {
        // Generate Starfield
        for (i in 0..100) {
            starfield.add(
                Point3D(
                    (Random.nextFloat() * 2000f - 1000f),
                    (Random.nextFloat() * 1200f - 600f),
                    (Random.nextFloat() * 800f + 100f)
                )
            )
        }

        // Generate a 3D Cyber-Ship (Wireframe Starship)
        // Main hull vertices
        val shipPoints = arrayOf(
            Point3D(0f, 0f, 150f),      // Nose [0]
            Point3D(-50f, -20f, -50f),   // Left Wingtip [1]
            Point3D(50f, -20f, -50f),    // Right Wingtip [2]
            Point3D(0f, 25f, -80f),      // Upper Spine [3]
            Point3D(0f, -15f, -100f),    // Tail Thruster center [4]
            Point3D(-25f, -10f, -80f),   // Rear frame left [5]
            Point3D(25f, -10f, -80f),    // Rear frame right [6]
            // Outer geometric protection rings
            Point3D(0f, 100f, 0f),       // Shield apex Top [7]
            Point3D(0f, -100f, 0f),      // Shield apex Bottom [8]
            Point3D(-150f, 0f, 0f),      // Shield Left [9]
            Point3D(150f, 0f, 0f)        // Shield Right [10]
        )
        rawVertices.addAll(shipPoints)

        // Hull framing lines
        rawLines.add(Line3D(shipPoints[0], shipPoints[1], 0)) // Nose to Left Wing
        rawLines.add(Line3D(shipPoints[0], shipPoints[2], 0)) // Nose to Right Wing
        rawLines.add(Line3D(shipPoints[0], shipPoints[3], 1)) // Nose to Tail Spine (Cyan)
        rawLines.add(Line3D(shipPoints[1], shipPoints[5], 0))
        rawLines.add(Line3D(shipPoints[2], shipPoints[6], 0))
        rawLines.add(Line3D(shipPoints[5], shipPoints[4], 0))
        rawLines.add(Line3D(shipPoints[6], shipPoints[4], 0))
        rawLines.add(Line3D(shipPoints[3], shipPoints[4], 1))
        
        // Wing cross beams
        rawLines.add(Line3D(shipPoints[1], shipPoints[3], 2)) // Wing to Spine (Green)
        rawLines.add(Line3D(shipPoints[2], shipPoints[3], 2))
        rawLines.add(Line3D(shipPoints[5], shipPoints[3], 1))
        rawLines.add(Line3D(shipPoints[6], shipPoints[3], 1))
        
        // Circular Orbital Shield Ring (around ship)
        val shieldSteps = 16
        val shieldRadius = 160f
        val shieldPoints = ArrayList<Point3D>()
        for (i in 0 until shieldSteps) {
            val angle = (2 * Math.PI * i / shieldSteps).toFloat()
            shieldPoints.add(Point3D(cos(angle) * shieldRadius, sin(angle) * shieldRadius, -10f))
        }
        rawVertices.addAll(shieldPoints)
        for (i in 0 until shieldSteps) {
            rawLines.add(Line3D(shieldPoints[i], shieldPoints[(i + 1) % shieldSteps], 1))
        }
    }

    val targetResolutionLabel: String
        get() = "${sourceResolution.targetHeight}p"

    val targetWidth: Int
        get() = sourceResolution.targetWidth

    val targetHeight: Int
        get() = sourceResolution.targetHeight

    // Advanced dynamic system updates (simulating true GPU mechanics)
    fun tick(deltaTimeS: Float) {
        // Rotate Scene
        rotationAngleX = (rotationAngleX + deltaTimeS * 15f) % 360f
        rotationAngleY = (rotationAngleY + deltaTimeS * 24f) % 360f
        rotationAngleZ = (rotationAngleZ + deltaTimeS * 8f) % 360f

        // Starfield Scrolling
        for (i in starfield.indices) {
            val pt = starfield[i]
            pt.z -= deltaTimeS * 400f
            if (pt.z < 50f) {
                pt.z = 850f
                pt.x = (Random.nextFloat() * 2000f - 1000f)
                pt.y = (Random.nextFloat() * 1200f - 600f)
            }
        }

        // Halton Jitter Progression
        jitterIndex = (jitterIndex + 1) % haltonX.size

        // RAM compaction cycles tick
        if (ramCompressorEnabled && Random.nextFloat() < 0.05f) {
            memoryCompactionCycles += 1
        }

        // Calculate actual performance metrics depending on GPU load, tweaks and configurations
        computeMetrics(deltaTimeS)
    }

    private fun computeMetrics(deltaTimeS: Float) {
        // VRAM dynamic highly-compressor calculations
        var vramRatio = 1.0f
        if (gpuVramCompressionEnabled) {
            vramRatio = when (gpuVramCompressionLevel) {
                "Standard (Sans perte)" -> 1.5f
                "Élevée (Lossy 2:1)" -> 2.0f
                "Extrême LZW-H" -> 3.2f
                "Ultra-Noyau Quantique (4:1 Compaction)" -> 4.1f
                else -> 1.0f
            }
        }
        activeVramFootprintMb = gpuVideoMemorySizeMb.toFloat() / vramRatio

        // 1. Texture Compression & Savings Profile
        var bandwidthSavingFraction = 0f
        if (gpuTextureCompressorEnabled) {
            val ratio = when (gpuTextureCompressionFormat) {
                "BC7 (Normale/Qualité)" -> 4.0f
                "ASTC 4x4" -> 6.0f
                "ETC2 (Mobile)" -> 3.5f
                "Wavlet Adaptive Huffman (Ultra)" -> 8.0f
                else -> 4.0f
            }
            compressedTextureMemorySizeMb = 2048f / ratio
            textureBandwidthSavedPct = (1f - (1f / ratio)) * 100f
            bandwidthSavingFraction = (ratio - 1f) / 10f // reduces power strain
        } else {
            compressedTextureMemorySizeMb = 2048f
            textureBandwidthSavedPct = 0f
        }

        // Base overhead
        var calculatedGpuLoad = gpuLoadSlider
        
        // If DRS (Dynamic Resolution Scaling) is ON:
        // Automatically drop source resolution if simulated GPU load rises above 60%
        autoThrottledActive = false
        if (dynamicResolutionScaling && calculatedGpuLoad > 55f) {
            autoThrottledActive = true
            when {
                calculatedGpuLoad > 85f -> activeSourceResolution = availableResolutions.find { it.id == "120p" } ?: availableResolutions[0]
                calculatedGpuLoad > 70f -> activeSourceResolution = availableResolutions.find { it.id == "240p" } ?: availableResolutions[1]
                else -> activeSourceResolution = availableResolutions.find { it.id == "360p" } ?: availableResolutions[2]
            }
        } else {
            activeSourceResolution = sourceResolution
        }

        // Impact of VRS (Variable Rate Shading)
        // If VRS is enabled, it decreases the pixel shading load, effectively reducing the active load on GPU
        var loadReduction = 0f
        if (variableRateShading) {
            loadReduction += 12f // VRS blocks 2x2 or 4x4 on edges save ~12% overall load
            if (foveatedVringEnabled) {
                loadReduction += 8f // Additional foveated ring save 8%
            }
        }
        val targetLoad = (calculatedGpuLoad - loadReduction).coerceIn(5f, 100f)

        // Core multipliers from Overclock / Undervolt parameters
        val cpuOverclockFactor = if (cpuOverclockEnabled) (cpuCoreFreqGhz / 2.2f).coerceAtLeast(1.0f) else 1.0f
        val gpuOverclockFactor = if (gpuOverclockEnabled) (gpuClockMhz.toFloat() / 750f).coerceAtLeast(1.0f) else 1.0f

        // --- 0. BATTERY OPTIMIZER ENFORCEMENT ---
        when (batteryOptimizerMode) {
            "Ultra-Low Power (Extrême)" -> {
                cpuTdpWatts = 6f
                gpuTdpWatts = 8f
                cpuGovernor = "Powersave (Ultra-Éco)"
                gpuGovernor = "Powersave"
                cpuBigCore0Enabled = false
                cpuBigCore1Enabled = false
                cpuBigCore2Enabled = false
                cpuBigCore3Enabled = false
                cpuLittleCore4Enabled = true
                cpuLittleCore5Enabled = true
                cpuLittleCore6Enabled = false
                cpuLittleCore7Enabled = false
                cpuUndervoltEnabled = true
                cpuVoltOffsetMv = -180
                cpuOverclockEnabled = false
                gpuOverclockEnabled = false
                gpuVramCompressionEnabled = true
                gpuVramCompressionLevel = "Ultra-Noyau Quantique (4:1 Compaction)"
                ramCompressorEnabled = true
                ramCompressionAlgo = "ZSTD-Ultra"
                batteryHealthStatus = "Impeccable (Refroidi / Veille Profonde)"
            }
            "Éco Ultra-Économique" -> {
                cpuTdpWatts = 12f
                gpuTdpWatts = 15f
                cpuGovernor = "Conservative (Éco)"
                gpuGovernor = "Conservative"
                cpuBigCore0Enabled = true
                cpuBigCore1Enabled = false
                cpuBigCore2Enabled = false
                cpuBigCore3Enabled = false
                cpuLittleCore4Enabled = true
                cpuLittleCore5Enabled = true
                cpuLittleCore6Enabled = true
                cpuLittleCore7Enabled = true
                cpuUndervoltEnabled = true
                cpuVoltOffsetMv = -120
                cpuOverclockEnabled = false
                gpuOverclockEnabled = false
                batteryHealthStatus = "Excellent (Consommation Minimale)"
            }
            "Extrême High Power (Overlimit)" -> {
                cpuTdpWatts = 110f
                gpuTdpWatts = 140f
                cpuGovernor = "Performance (Extrême)"
                gpuGovernor = "Performance (Extrême)"
                cpuBigCore0Enabled = true
                cpuBigCore1Enabled = true
                cpuBigCore2Enabled = true
                cpuBigCore3Enabled = true
                cpuLittleCore4Enabled = true
                cpuLittleCore5Enabled = true
                cpuLittleCore6Enabled = true
                cpuLittleCore7Enabled = true
                cpuUndervoltEnabled = false
                cpuOverclockEnabled = true
                cpuCoreFreqGhz = 4.2f
                gpuOverclockEnabled = true
                gpuClockMhz = 1150
                batteryHealthStatus = "Surchauffe Potentielle (Ventilation Requise)"
            }
            "Haute Performance" -> {
                cpuTdpWatts = 70f
                gpuTdpWatts = 90f
                cpuGovernor = "Lulzactive"
                gpuGovernor = "Adreno-Boost"
                cpuBigCore0Enabled = true
                cpuBigCore1Enabled = true
                cpuBigCore2Enabled = true
                cpuBigCore3Enabled = true
                cpuLittleCore4Enabled = true
                cpuLittleCore5Enabled = true
                cpuLittleCore6Enabled = true
                cpuLittleCore7Enabled = true
                batteryHealthStatus = "Excellent (Performances Débridées)"
            }
            else -> {
                // "Équilibré Auto" or manual custom presets
                batteryHealthStatus = "Excellent (Régulation Intelligente)"
            }
        }

        // --- 1. CORE-BY-CORE CPU CLOCK & CORE RATINGS ---
        val sumBigFreq = (if (cpuBigCore0Enabled) cpuBigCore0FreqGhz else 0f) +
                         (if (cpuBigCore1Enabled) cpuBigCore1FreqGhz else 0f) +
                         (if (cpuBigCore2Enabled) cpuBigCore2FreqGhz else 0f) +
                         (if (cpuBigCore3Enabled) cpuBigCore3FreqGhz else 0f)

        val sumLittleFreq = (if (cpuLittleCore4Enabled) cpuLittleCore4FreqGhz else 0f) +
                            (if (cpuLittleCore5Enabled) cpuLittleCore5FreqGhz else 0f) +
                            (if (cpuLittleCore6Enabled) cpuLittleCore6FreqGhz else 0f) +
                            (if (cpuLittleCore7Enabled) cpuLittleCore7FreqGhz else 0f)

        val activeCoresCount = (if (cpuBigCore0Enabled) 1 else 0) +
                               (if (cpuBigCore1Enabled) 1 else 0) +
                               (if (cpuBigCore2Enabled) 1 else 0) +
                               (if (cpuBigCore3Enabled) 1 else 0) +
                               (if (cpuLittleCore4Enabled) 1 else 0) +
                               (if (cpuLittleCore5Enabled) 1 else 0) +
                               (if (cpuLittleCore6Enabled) 1 else 0) +
                               (if (cpuLittleCore7Enabled) 1 else 0)

        // Baseline sum for 4 cores @ 2.8GHz and 4 cores @ 1.8GHz is 11.2f + 7.2f = 18.4f
        val clockRatingRatio = if (activeCoresCount == 0) 0.05f else (sumBigFreq + sumLittleFreq) / 18.4f

        // --- 2. GOVERNORS MULTIPLIERS ---
        var governorCpuPerformanceMultiplier = 1.0f
        var governorCpuPowerMultiplier = 1.0f
        when (cpuGovernor) {
            "Performance (Extrême)" -> {
                governorCpuPerformanceMultiplier = 1.25f
                governorCpuPowerMultiplier = 1.6f
            }
            "Schedutil (Réactif)" -> {
                governorCpuPerformanceMultiplier = 1.05f
                governorCpuPowerMultiplier = 1.02f
            }
            "Interactive (Moderne)" -> {
                governorCpuPerformanceMultiplier = 1.15f
                governorCpuPowerMultiplier = 1.15f
            }
            "Lulzactive" -> {
                governorCpuPerformanceMultiplier = 1.20f
                governorCpuPowerMultiplier = 1.35f
            }
            "Conservative (Éco)" -> {
                governorCpuPerformanceMultiplier = 0.85f
                governorCpuPowerMultiplier = 0.7f
            }
            "Powersave (Ultra-Éco)" -> {
                governorCpuPerformanceMultiplier = 0.55f
                governorCpuPowerMultiplier = 0.4f
            }
        }

        var governorGpuPerformanceMultiplier = 1.0f
        var governorGpuPowerMultiplier = 1.0f
        when (gpuGovernor) {
            "Performance (Extrême)" -> {
                governorGpuPerformanceMultiplier = 1.25f
                governorGpuPowerMultiplier = 1.55f
            }
            "Simple-Ondemand" -> {
                governorGpuPerformanceMultiplier = 1.02f
                governorGpuPowerMultiplier = 1.0f
            }
            "Devfreq-Ondemand" -> {
                governorGpuPerformanceMultiplier = 1.06f
                governorGpuPowerMultiplier = 1.05f
            }
            "Adreno-Boost" -> {
                governorGpuPerformanceMultiplier = 1.16f
                governorGpuPowerMultiplier = 1.22f
            }
            "Conservative" -> {
                governorGpuPerformanceMultiplier = 0.85f
                governorGpuPowerMultiplier = 0.72f
            }
            "Powersave" -> {
                governorGpuPerformanceMultiplier = 0.58f
                governorGpuPowerMultiplier = 0.45f
            }
        }

        // 2. CPU Multi-Thread and GPU Async Compute Multipliers
        val cpuThreadsSpeedup = if (multiCoreCpuDispatch) {
            1.0f / (1.0f + (cpuWorkerThreads - 2) * 0.06f)
        } else {
            1.8f // Single-core dispatch lock bottleneck
        }

        val gpuPipelinesSpeedup = if (multiQueueGpuComputing) {
            1.0f / (1.0f + (gpuExecutionPipelines - 2) * 0.04f)
        } else {
            1.4f // Serial synchronous queues
        }

        // 3. Anti-Aliasing (AA) & Anisotropic Filtering (AF) Simulation Overhead
        var additionalAaCostMs = when (antiAliasingMode) {
            "Aucun" -> 0.0f
            "FXAA" -> 0.15f
            "SMAA" -> 0.4f
            "TAA (Temporel)" -> 0.7f * (aaSampleCount / 4f)
            "DSAA (Deep Learning AA)" -> 1.3f // Powered by GPU Tensor Cores
            "MSAA 4x" -> 2.6f * (aaSampleCount / 4f) // pure super-sampling cost
            else -> 0.6f
        }
        val afCostMs = (anisotropicLevel - 1) * 0.04f

        // Calculate Frame Times based on targetResolution and load
        // Pixels render factor relative to designated dynamic resolution targets!
        val targetW = targetWidth.toFloat()
        val targetH = targetHeight.toFloat()
        val pixelCountRatio = (activeSourceResolution.width * activeSourceResolution.height).toFloat() / (targetW * targetH)
        
        // Base Render Time of a frame in ms (incorporating multi-thread, core clocks and AA/AF profiles)
        var baseRenderMs = ((1.0f + (targetLoad * 0.14f) * (pixelCountRatio * 0.8f + 0.2f)) * cpuThreadsSpeedup) / (clockRatingRatio * governorCpuPerformanceMultiplier)
        
        // Adding upscaling algorithm cost (divided by GPU Overclock factor & GPU speedup pipelines)
        val upscaleCostMod = when (upscaleMode) {
            UpscaleMode.NATIVE_LOW -> 0.1f
            UpscaleMode.BILINEAR -> 0.2f
            UpscaleMode.FSR_1_0 -> 1.0f // Spatial edge filter
            UpscaleMode.FSR_2_0 -> 2.2f // Temporal history + projection
        }
        baseRenderMs += ((upscaleCostMod * (1.0f + (targetLoad * 0.05f))) * gpuPipelinesSpeedup) / (gpuOverclockFactor * governorGpuPerformanceMultiplier)
        
        // Add AA + AF filtering overhead
        baseRenderMs += (additionalAaCostMs + afCostMs) / (gpuOverclockFactor * governorGpuPerformanceMultiplier)

        // --- Multi-Architecture ISA / SDK multipliers ---
        val isaSpeedFactor = when (targetIsaArchitecture) {
            "ARMv9-A (Modern 64-bit)" -> 0.82f // SVE2/SME vectorization speed boost (18% faster)
            "ARM64-v8a (ARMv8 64-bit)" -> 1.0f // Baseline
            "ARMv7-A (32-bit legacy)" -> 1.55f // No registers, branch penalty (+55% slow down)
            "x86_64 (Intel/AMD)" -> 0.95f // AVX instructions
            "x86 (Legacy Emulator)" -> 1.4f // SSE limited x86 bounds (+40% slow down)
            else -> 1.0f
        }

        val sdkSpeedFactor = when (targetAndroidSdkVersion) {
            "Android 14/15/16 (API 34-36)" -> 0.9f // ART 14 with ultra GCs and Choreographer Vsync alignments
            "Android 10 - 13 (API 29-33)" -> 1.0f // Standard modern
            "Android 7.0 - 9.0 (API 24-28)" -> 1.2f // Middle desugaring layer overhead
            "Android 5.0 - 6.0 (API 21-23)" -> 1.45f // Old runtime + slow memory sweeps
            else -> 1.0f
        }

        val linkerFactor = if (linkerLtoOptimizations) 0.92f else 1.0f
        
        // Apply ISA & SDK architecture multipliers
        baseRenderMs *= (isaSpeedFactor * sdkSpeedFactor * linkerFactor)

        // Auto-Adaptive Frame Generation check
        frameGenThrottledActive = false
        var currentFrameGen = frameGenMode
        if (frameGenMode != FrameGenMode.OFF && adaptiveFrameGen) {
            // If total rendering + frame gen time goes beyond refresh budget, drop generator to maintain stability
            val calculatedPreFrameTime = baseRenderMs + ((if (frameGenMode == FrameGenMode.LSFG_X3) 4.5f else 2.5f) / gpuOverclockFactor)
            if (calculatedPreFrameTime > 28.0f) { // Too heavy!
                frameGenThrottledActive = true
                currentFrameGen = if (frameGenMode == FrameGenMode.LSFG_X3) FrameGenMode.LSFG_X2 else FrameGenMode.OFF
            }
        }

        // Frame Generation Costs scaling down with GPU overclock!
        val frameGenCostMs = when (currentFrameGen) {
            FrameGenMode.OFF -> 0f
            FrameGenMode.LSFG_X2 -> (1.8f + (targetLoad * 0.03f)) / gpuOverclockFactor
            FrameGenMode.LSFG_X3 -> (3.6f + (targetLoad * 0.06f)) / gpuOverclockFactor
        }

        // --- PROFILE PRESETS ENFORCEMENT ---
        if (batteryOptimizerMode == "Équilibré Auto" || batteryOptimizerMode == "Manuel") {
            when (powerProfileMode) {
                "Éco (15W/20W)" -> {
                    cpuTdpWatts = 15f
                    gpuTdpWatts = 20f
                }
                "Équilibrée (35W/45W)" -> {
                    cpuTdpWatts = 35f
                    gpuTdpWatts = 45f
                }
                "Performance (65W/80W)" -> {
                    cpuTdpWatts = 65f
                    gpuTdpWatts = 80f
                }
            }
        }

        // --- MODERN TDP CALCULATIONS ---
        // base inputs for power calculations
        val cpuPowerSavings = if (cpuUndervoltEnabled) (cpuVoltOffsetMv.toFloat() / -1000f).coerceIn(0f, 0.2f) else 0f
        val gpuPowerSavings = if (gpuUndervoltEnabled) (gpuVoltOffsetMv.toFloat() / -800f).coerceIn(0f, 0.22f) else 0f
        val systemUndervoltSavings = 1.0f - cpuPowerSavings - gpuPowerSavings - (bandwidthSavingFraction * 0.15f)

        // Overclocking and active pipelines power overheads
        val cpuOverclockPowerAdd = if (cpuOverclockEnabled) ((cpuCoreFreqGhz - 2.2f) * 900f).coerceAtLeast(0f) else 0f
        val gpuOverclockPowerAdd = if (gpuOverclockEnabled) ((gpuClockMhz - 750) * 4.5f).coerceAtLeast(0f) else 0f

        val aaPowerAdd = when (antiAliasingMode) {
            "DSAA (Deep Learning AA)" -> 450f
            "MSAA 4x" -> 600f
            else -> 100f
        }

        // --- RAW CPU & GPU POWER REQUESTS ---
        val baseCpuMultiplier = if (multiCoreCpuDispatch) (cpuWorkerThreads / 8f) else 2.5f
        val cpuPowerNeededMw = ((2000f + (targetLoad * 15f) + (baseCpuMultiplier * 1400f) * clockRatingRatio + (if (ramCompressorEnabled) 300f else 0f)) * (1.0f - cpuPowerSavings) + cpuOverclockPowerAdd) * governorCpuPowerMultiplier
        val gpuPowerNeededMw = ((500f + (targetLoad * 35f) + (upscaleCostMod * 150f) + (frameGenCostMs * 180f) + aaPowerAdd) * (1.0f - gpuPowerSavings) + gpuOverclockPowerAdd) * governorGpuPowerMultiplier

        // --- DUAL POWER BOOSTER (SmartShift / Dynamic Power Boost) ---
        var effectiveCpuLimitMw = cpuTdpWatts * 1000f
        var effectiveGpuLimitMw = gpuTdpWatts * 1000f

        if (dualPowerBoosterEnabled) {
            val cpuHeadroomMw = (effectiveCpuLimitMw - cpuPowerNeededMw).coerceAtLeast(0f)
            val gpuHeadroomMw = (effectiveGpuLimitMw - gpuPowerNeededMw).coerceAtLeast(0f)
            
            // dynamically reallocate 85% of unused TDP to each other
            effectiveGpuLimitMw += cpuHeadroomMw * 0.85f
            effectiveCpuLimitMw += gpuHeadroomMw * 0.85f
        }

        // --- THROTTLING LOGIC CORES BY TDP ENVELOPE ---
        cpuTdpThrottledActive = cpuPowerNeededMw > effectiveCpuLimitMw
        gpuTdpThrottledActive = gpuPowerNeededMw > effectiveGpuLimitMw

        val cpuTdpThrottleFactor = if (cpuTdpThrottledActive) {
            (effectiveCpuLimitMw / cpuPowerNeededMw).coerceIn(0.35f, 1.0f)
        } else 1.0f

        val gpuTdpThrottleFactor = if (gpuTdpThrottledActive) {
            (effectiveGpuLimitMw / gpuPowerNeededMw).coerceIn(0.35f, 1.0f)
        } else 1.0f

        // Apply performance penalties if throttled by dynamic TDP constraints
        baseRenderMs /= cpuTdpThrottleFactor
        baseRenderMs /= gpuTdpThrottleFactor

        // Calculate actual final power consumption conforming to limits with hardware vendor optimizations
        val vendorPowerMultiplier = if (selectedHardwareVendor != "Détection Automatique (Intelligente)" && brandTweakActive) 0.90f else 1.0f
        cpuPowerDrawMw = cpuPowerNeededMw * cpuTdpThrottleFactor * vendorPowerMultiplier
        gpuPowerDrawMw = gpuPowerNeededMw * gpuTdpThrottleFactor * vendorPowerMultiplier

        renderTimeMs = baseRenderMs
        frameGenTimeMs = frameGenCostMs

        // Base FPS achievable
        val maxFeasibleBaseFps = (1000f / baseRenderMs).coerceAtMost(baseFps.toFloat())
        actualRenderFps = maxFeasibleBaseFps

        // Output FPS after Frame Generation
        val maxCap = if (fpsLimiterEnabled) {
            when (fpsLimitPreset) {
                "30 FPS" -> 30f
                "60 FPS" -> 60f
                "120 FPS" -> 120f
                "144 FPS" -> 144f
                "240 FPS" -> 240f
                "Personnalisé (Custom)" -> fpsLimitValue.toFloat()
                else -> 240f
            }
        } else {
            240f
        }

        interpolatedFps = when (currentFrameGen) {
            FrameGenMode.OFF -> actualRenderFps
            FrameGenMode.LSFG_X2 -> actualRenderFps * 2
            FrameGenMode.LSFG_X3 -> actualRenderFps * 3
        }.coerceAtMost(maxCap)

        // Compute complex active RAM compressor attributes
        if (ramCompressorEnabled) {
            val ratio = when (ramCompressionAlgo) {
                "ZRAM (LZ4-compaction)" -> 3.3f
                "ZSTD-Ultra" -> 4.8f
                "LZO-Concurrent" -> 2.8f
                "Double-Huffman" -> 1.9f
                else -> 3.3f
            }
            compressedMemorySizeGb = rawMemorySizeGb / ratio
            decompressionLatencyMs = when (ramCompressionAlgo) {
                "ZRAM (LZ4-compaction)" -> 0.04f
                "ZSTD-Ultra" -> 0.09f
                "LZO-Concurrent" -> 0.02f
                "Double-Huffman" -> 0.05f
                else -> 0.04f
            }
        } else {
            compressedMemorySizeGb = rawMemorySizeGb
            decompressionLatencyMs = 0.0f
        }

        // Latency in ms (Base display cycle + interpolation overhead + tiny RAM decompression delay) with extreme schedulers
        var calculatedLatency = (1000f / actualRenderFps) + frameGenCostMs + (if (currentFrameGen != FrameGenMode.OFF) 8.0f else 2.0f) + decompressionLatencyMs
        if (easBypassActive) calculatedLatency -= 1.8f
        if (brandTweakActive) calculatedLatency -= 0.9f
        totalLatencyMs = calculatedLatency.coerceAtLeast(3.2f)

        // Temperature computation matching the scaled power and throttle limits
        val thermalK = 0.4f
        val baseTemp = 36f
        val cpuTempAdd = if (cpuOverclockEnabled) ((cpuCoreFreqGhz - 2.2f) * 16f) else 0f
        val gpuTempAdd = if (gpuOverclockEnabled) ((gpuClockMhz - 750) * 0.07f) else 0f
        val cpuTempDrop = if (cpuUndervoltEnabled) (cpuVoltOffsetMv.toFloat() / -10f) else 0f
        val gpuTempDrop = if (gpuUndervoltEnabled) (gpuVoltOffsetMv.toFloat() / -6f) else 0f
        val multiCoreTempDrop = if (multiCoreCpuDispatch) 2.5f else 0f // multi threading runs slightly cooler

        // Temperature depends on power draw relative to standard loads
        var targetTemp = baseTemp + ((gpuPowerDrawMw / 1000f) * 5.0f) + ((cpuPowerDrawMw / 1000f) * 2.5f) + cpuTempAdd + gpuTempAdd - cpuTempDrop - gpuTempDrop - (if (variableRateShading) 3f else 0f) - (if (dynamicResolutionScaling && calculatedGpuLoad > 55f) 5f else 0f) - multiCoreTempDrop
        
        // Adjust temperature depending on governor and battery optimizer
        if (batteryOptimizerMode == "Ultra-Low Power (Extrême)") {
            targetTemp -= 12f
        } else if (batteryOptimizerMode == "Extrême High Power (Overlimit)") {
            targetTemp += 15f
        }
        targetTemp = targetTemp.coerceIn(24f, 115f)

        // 4. Adjustable Temperature Limiter & Active Control
        if (targetTemp > thermalLimitCelsius) {
            thermalThrottlingActive = true
            // Reduce performance progressively to drop temp back to limit
            val excessTemp = targetTemp - thermalLimitCelsius
            val throttleMultiplier = (1.0f - (excessTemp / 50f)).coerceIn(0.4f, 0.95f)

            // Applying progressive performance penalty
            renderTimeMs /= throttleMultiplier
            actualRenderFps *= throttleMultiplier
            interpolatedFps *= throttleMultiplier
            totalLatencyMs /= throttleMultiplier
            gpuPowerDrawMw *= throttleMultiplier
            cpuPowerDrawMw *= throttleMultiplier

            // Stabilize simulated temp near the set thermal target limit
            gpuTempCelsius = thermalLimitCelsius + (Random.nextFloat() * 0.8f - 0.4f)
        } else {
            thermalThrottlingActive = false
            gpuTempCelsius = targetTemp
        }

        // Stutter Rate with ultra-rare optimizations
        var baseStutterMitigation = if (ramCompressorEnabled) 0.25f else 1.0f
        if (vulkanRamdiskCacheEnabled) baseStutterMitigation *= 0.40f
        if (turboStoragePipelineEnabled) baseStutterMitigation *= 0.70f
        staggerRatePercent = ((1.0f + (targetLoad * 0.12f) + (if (frameGenThrottledActive) 6.5f else 0f) + (if (thermalThrottlingActive) 5.0f else 0f) + (if (cpuTdpThrottledActive || gpuTdpThrottledActive) 3.5f else 0f)) * baseStutterMitigation).coerceIn(0.05f, 18f)

        // --- MODERN BATTERY POWER DRAW CALCULATIONS ---
        val totalPowerDrawMw = cpuPowerDrawMw + gpuPowerDrawMw + 400f
        if (batteryBypassChargingEnabled) {
            batteryCurrentDrawMa = -12 // Ultra low leak
            batteryEstimatedRemainingHours = 999.0f // Connected to grid
        } else {
            // standard Li-ion voltage roughly 3.85V. mA = mW / V
            val currentMa = (totalPowerDrawMw / 3.85f).toInt()
            batteryCurrentDrawMa = -currentMa
            
            // Assume 5000 mAh total capacity
            val hours = 5000f / currentMa
            batteryEstimatedRemainingHours = hours.coerceIn(0.8f, 32f)
        }
    }

    // Get subpixel jitter offset for FSR 2.0 rendering in high-res coordinates
    fun getJitterOffset(scale: Float): PointF {
        if (upscaleMode != UpscaleMode.FSR_2_0) return PointF(0f, 0f)
        // Halton sequence centering around [-0.5, 0.5]
        val hX = (haltonX[jitterIndex] - 0.5f) * scale
        val hY = (haltonY[jitterIndex] - 0.5f) * scale
        return PointF(hX, hY)
    }

    // Return list of motion vectors representing current rotation speed (for overlay)
    fun getProjectedLinesForDrawing(
        width: Float,
        height: Float,
        isUpscaledPass: Boolean,
        interpolatedAlpha: Float = 0f // For frame generation blending simulation
    ): List<RenderLine> {
        val centerX = width / 2f
        val centerY = height / 2f
        val scale = Math.min(width, height) / 450f

        // Active resolution limits if simulating low-res projection
        val currentRes = if (isUpscaledPass) sourceResolution else activeSourceResolution
        val lowResW = currentRes.width.toFloat()
        val lowResH = currentRes.height.toFloat()

        // Calculate rotation angles including interpolation delta if we are drawing an interpolated frame
        val angleX = rotationAngleX + (if (interpolatedAlpha > 0f) 15f * interpolatedAlpha else 0f)
        val angleY = rotationAngleY + (if (interpolatedAlpha > 0f) 24f * interpolatedAlpha else 0f)
        val angleZ = rotationAngleZ + (if (interpolatedAlpha > 0f) 8f * interpolatedAlpha else 0f)

        // Project 3D lines to 2D
        val result = ArrayList<RenderLine>()

        // FSR 2.0 Subpixel Jitter (Only applied to low-res pass of our pipeline)
        val jitter = if (!isUpscaledPass && upscaleMode == UpscaleMode.FSR_2_0) {
            getJitterOffset(scale)
        } else {
            PointF(0f, 0f)
        }

        for (line in rawLines) {
            // First point
            var pt1 = line.start.rotateX(angleX).rotateY(angleY).rotateZ(angleZ)
            // Perspective Projection
            val dist1 = 500f + pt1.z
            val screen1X = centerX + (pt1.x * 500f / dist1) * scale + jitter.x
            val screen1Y = centerY + (pt1.y * 500f / dist1) * scale + jitter.y

            // Second point
            var pt2 = line.end.rotateX(angleX).rotateY(angleY).rotateZ(angleZ)
            val dist2 = 500f + pt2.z
            val screen2X = centerX + (pt2.x * 500f / dist2) * scale + jitter.x
            val screen2Y = centerY + (pt2.y * 500f / dist2) * scale + jitter.y

            // Render calculations simulating discretization of low-resolution grid
            val finalStart = projectPixel(screen1X, screen1Y, lowResW, lowResH, width, height, isUpscaledPass)
            val finalEnd = projectPixel(screen2X, screen2Y, lowResW, lowResH, width, height, isUpscaledPass)

            // Motion Vector calculation (Current pos - Pos from previous clock cycle)
            val prevPt1 = line.start.rotateX(angleX - 2.5f).rotateY(angleY - 4f).rotateZ(angleZ - 1.2f)
            val prevDist1 = 500f + prevPt1.z
            val prevScreen1X = centerX + (prevPt1.x * 500f / prevDist1) * scale
            val prevScreen1Y = centerY + (prevPt1.y * 500f / prevDist1) * scale
            val prevProjected = projectPixel(prevScreen1X, prevScreen1Y, lowResW, lowResH, width, height, isUpscaledPass)

            val motionX = finalStart.x - prevProjected.x
            val motionY = finalStart.y - prevProjected.y

            result.add(
                RenderLine(
                    startX = finalStart.x,
                    startY = finalStart.y,
                    endX = finalEnd.x,
                    endY = finalEnd.y,
                    motionVectorX = motionX,
                    motionVectorY = motionY,
                    colorType = line.colorType,
                    zDepth = (pt1.z + pt2.z) / 2f
                )
            )
        }
        
        // Sort by Z depth for beautiful layered render
        result.sortByDescending { it.zDepth }
        return result
    }

    // Helper to simulate rendering a vertex onto the low resolution grid, and upscaling it back
    private fun projectPixel(
        screenX: Float,
        screenY: Float,
        lowResW: Float,
        lowResH: Float,
        targetW: Float,
        targetH: Float,
        isNative720p: Boolean
    ): PointF {
        if (isNative720p) {
            return PointF(screenX, screenY)
        }

        // Map standard coordinate to [0,1] space inside viewport
        val u = (screenX / targetW).coerceIn(0f, 1f)
        val v = (screenY / targetH).coerceIn(0f, 1f)

        // Discretize into low resolution grid
        val pixelX = Math.floor((u * lowResW).toDouble()).toFloat()
        val pixelY = Math.floor((v * lowResH).toDouble()).toFloat()

        // Map back to screen coordinate space
        val upscaledX = (pixelX / lowResW) * targetW
        val upscaledY = (pixelY / lowResH) * targetH

        return PointF(upscaledX, upscaledY)
    }

    // Starfield projection
    fun getStarfieldForDrawing(
        width: Float,
        height: Float,
        isUpscaledPass: Boolean
    ): List<RenderStar> {
        val centerX = width / 2f
        val centerY = height / 2f
        val scale = Math.min(width, height) / 450f
        
        val currentRes = if (isUpscaledPass) sourceResolution else activeSourceResolution
        val lowResW = currentRes.width.toFloat()
        val lowResH = currentRes.height.toFloat()

        val result = ArrayList<RenderStar>()
        for (star in starfield) {
            val dist = 500f + star.z
            if (dist <= 0) continue

            val screenX = centerX + (star.x * 500f / dist) * scale
            val screenY = centerY + (star.y * 500f / dist) * scale

            val finalPt = projectPixel(screenX, screenY, lowResW, lowResH, width, height, isUpscaledPass)
            // Density/Brightness depends on depth (Z)
            val brightness = ((800f - star.z) / 800f).coerceIn(0f, 1f)
            
            result.add(RenderStar(finalPt.x, finalPt.y, brightness, star.z))
        }
        return result
    }
}

// Structures holding pre-calculated 2D projected layers for Compose Canvas render
data class RenderLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val motionVectorX: Float,
    val motionVectorY: Float,
    val colorType: Int, // 0 = standard, 1 = cyber accent, 2 = thruster
    val zDepth: Float
)

data class RenderStar(
    val x: Float,
    val y: Float,
    val brightness: Float,
    val zDepth: Float
)
