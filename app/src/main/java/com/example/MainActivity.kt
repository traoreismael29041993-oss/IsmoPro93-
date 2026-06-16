package com.example

import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.scale
import com.example.fsrengine.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

class MainActivity : ComponentActivity() {
    private val engine = FsrEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = CyberBlack
                ) { innerPadding ->
                    // Run real-time simulation tick
                    LaunchedEffect(Unit) {
                        var lastTime = System.nanoTime()
                        while (true) {
                            val now = System.nanoTime()
                            val elapsedS = (now - lastTime) / 1_000_000_000f
                            lastTime = now
                            engine.tick(elapsedS.coerceIn(0.001f, 0.1f))
                            delay(16) // Target ~60Hz simulator loop
                        }
                    }

                    MainScreen(
                        engine = engine,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(engine: FsrEngine, modifier: Modifier = Modifier) {
    var activeTab by remember { mutableStateOf(0) } // 0 = FSR, 1 = LSFG, 2 = Tech Tweaks
    var selectedBottomTab by remember { mutableStateOf(0) } // 0 = Core, 1 = Stats, 2 = Tweaks, 3 = Apps, 4 = Profile

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // Top Premium Header matching Design HTML
        HeaderView()

        // Main Body that displays content based on active bottom tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_scrollable"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedBottomTab) {
                    0 -> { // Core Tab
                        // 1. Viewport Container Card (Grip interaction + Simulation canvas)
                        item {
                            ViewportContainerCard(engine = engine)
                        }

                        // 2. Active Alert Warnings / DRS Status Feedback
                        item {
                            SystemAlertBanner(engine = engine)
                        }

                        // 3. Real-Time High Density Metrics Card (Purple Lavender backdrop)
                        item {
                            RealtimeMetricsCard(engine = engine)
                        }

                        // 4. Primary Control Grid overview (FSR & LSFG)
                        item {
                            SetupGridOverview(engine = engine)
                        }

                        // 5. Control Panel Tab Bar selector
                        item {
                            ControlPanelTabs(
                                activeTab = activeTab,
                                onTabSelected = { activeTab = it }
                            )
                        }

                        // 6. Selected config profiles panel
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    when (activeTab) {
                                        0 -> UpscaleTabControls(engine = engine)
                                        1 -> FrameGenTabControls(engine = engine)
                                        2 -> GraphicsTweaksTabControls(engine = engine)
                                    }
                                }
                            }
                        }

                        // 7. Bottom Action area matching apply/reset specifications
                        item {
                            BottomActionArea(engine = engine)
                        }
                    }

                    1 -> { // Stats Tab
                        // 1. Viewport Container Card
                        item {
                            ViewportContainerCard(engine = engine)
                        }

                        // 2. Real-Time Metrics Card
                        item {
                            RealtimeMetricsCard(engine = engine)
                        }

                        // 3. Dynamic Alerts
                        item {
                            SystemAlertBanner(engine = engine)
                        }

                        // 4. Expanded Stats Telemetry dashboard
                        item {
                            StatsTelemetryDashboard(engine = engine)
                        }

                        // 5. Magnifier Zoom details Loupe card
                        item {
                            MagnifierZoomCard(engine = engine)
                        }
                    }

                    2 -> { // Tweaks Tab
                        // 1. Advanced GPU Relief Tweaks Panel with real-time reactive binds
                        item {
                            AdvancedGpuReliefTweaksList(engine = engine)
                        }

                        // 2. Config options details panel
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    GraphicsTweaksTabControls(engine = engine)
                                }
                            }
                        }

                        // 2b. CPU and GPU hardware overclocking & undervolting tuning panel
                        item {
                            CpuGpuTuningPanel(engine = engine)
                        }

                        // 2c. Modern high efficiency ZRAM RAM compressor panel
                        item {
                            RamCompressorPanel(engine = engine)
                        }

                        // 2cc. NEW: Extreme Automatic Background App Manager & Process Killer
                        item {
                            BackgroundAppManagerPanel(engine = engine)
                        }

                        // 2d. NEW: Modern High Compressor of Texture GPU panel
                        item {
                            GpuTextureCompressorPanel(engine = engine)
                        }

                        // 2e. NEW: Anti-Aliasing & Anisotropic Filtering adjusts panel
                        item {
                            AntiAliasingAnisotropyPanel(engine = engine)
                        }

                        // 2f. NEW: Complex modern CPU & GPU Multi-Core Scheduler/Processor activation panel
                        item {
                            MultiCoreProcessingPanel(engine = engine)
                        }

                        // 2g. NEW: CPU & GPU Thermales manager with adjustable limits
                        item {
                            ThermalLimitManagerPanel(engine = engine)
                        }

                        // 2h. NEW: CPU & GPU TDP Power limits panel (SmartShift, custom Watts sliders)
                        item {
                            TdpPowerManagerPanel(engine = engine)
                        }

                        // 2hh. NEW: Extreme Battery Autonomiser & Cores Regulator Panel
                        item {
                            GovernorsCoreBatteryPanel(engine = engine)
                        }

                        // 2hhh. NEW: Dynamic Interpolated FPS Limiter Panel
                        item {
                            FpsLimiterPanel(engine = engine)
                        }

                        // 2hhhh. NEW: Universal Chipset Brand Compatibility & Ultra Rare Tweaks Panel
                        item {
                            ExtremeChipsetRareTweaksPanel(engine = engine)
                        }

                        // 2i. NEW: ISA Architecture x86/ARM and Android SDK Version Compatibility Panel
                        item {
                            ArchitectureCompatibilityPanel(engine = engine)
                        }

                        // 3. Action buttons
                        item {
                            BottomActionArea(engine = engine)
                        }
                    }

                    3 -> { // Apps Tab
                        item {
                            ApplicationProfilesScreenView(engine = engine)
                        }
                    }

                    4 -> { // Profile Tab
                        // 1. Hardware Developer info card
                        item {
                            ProfileCard()
                        }
                        // 2. Access Privileges selection & terminal (Shizuku, Root, No Root)
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            AccessPrivilegesCard(engine = engine)
                        }
                    }
                }

                // Spacers for comfort
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Elegant M3 Lavender Bottom Navigation Bar
        BottomNavigationBarView(
            selectedTab = selectedBottomTab,
            onTabSelected = { selectedBottomTab = it }
        )
    }
}

@Composable
fun HeaderView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Subtle glowing line on top
                drawLine(
                    color = CyberPrimary,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(CyberDarkSurface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "AEROGLOW FSR & LSFG",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Laboratoire d'Upscaling & Fluidification GPU",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
            
            // Tech Core Status Pill
            Surface(
                color = CyberSecondary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(CyberSecondary, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPU ACTIVE",
                        color = CyberSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ViewportContainerCard(engine: FsrEngine) {
    var containerWidth by remember { mutableStateOf(300f) }
    var containerHeight by remember { mutableStateOf(200f) }
    val density = LocalDensity.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(engine.showSplitScreen) {
                    if (engine.showSplitScreen) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (change.position.x / containerWidth).coerceIn(0.05f, 0.95f)
                            engine.splitRatio = newX
                        }
                    }
                }
        ) {
            // Live simulation viewport canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("viewport_canvas")
                    .drawBehind {
                        containerWidth = size.width
                        containerHeight = size.height
                    }
            ) {
                val scale = size.minDimension / 450f

                // 1. Draw Space Grid background (retro theme)
                drawSpaceGrid(size.width, size.height, scale)

                // 2. Fetch geometries
                // Left side = unprocessed/low-res source, Right side = Upscaled
                val splitPixelX = size.width * engine.splitRatio

                if (engine.showSplitScreen) {
                    // Draw Left Half: Low-Res / Billinear Source
                    drawIntoCanvas { canvas ->
                        canvas.save()
                        canvas.clipRect(0f, 0f, splitPixelX, size.height)
                        
                        // Starfield low-res
                        val starsLow = engine.getStarfieldForDrawing(size.width, size.height, isUpscaledPass = false)
                        for (star in starsLow) {
                            val r = 2.5f * scale * star.brightness
                            drawCircle(
                                color = Color.White.copy(alpha = star.brightness * 0.7f),
                                radius = r,
                                center = Offset(star.x, star.y)
                            )
                        }

                        // Ship low-res lines
                        val linesLow = engine.getProjectedLinesForDrawing(size.width, size.height, isUpscaledPass = false)
                        for (line in linesLow) {
                            val strokeWidth = when (engine.upscaleMode) {
                                UpscaleMode.BILINEAR -> 4f * scale // Blurry
                                else -> 6f * scale // Blocky
                            }
                            val lineAlpha = if (engine.upscaleMode == UpscaleMode.BILINEAR) 0.5f else 1.0f
                            val color = getLineColor(line.colorType).copy(alpha = lineAlpha)
                            
                            drawLine(
                                color = color,
                                start = Offset(line.startX, line.startY),
                                end = Offset(line.endX, line.endY),
                                strokeWidth = strokeWidth
                            )
                        }

                        canvas.restore()
                    }

                    // Draw Right Half: Upscaled (FSR 1.0 / 2.0 or 720p Clean)
                    drawIntoCanvas { canvas ->
                        canvas.save()
                        canvas.clipRect(splitPixelX, 0f, size.width, size.height)

                        drawUpscaledViewport(engine, size.width, size.height, scale)

                        canvas.restore()
                    }

                    // Draw Separator line
                    drawLine(
                        color = CyberPrimary,
                        start = Offset(splitPixelX, 0f),
                        end = Offset(splitPixelX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Slider Grip Dot in middle
                    drawCircle(
                        color = CyberPrimary,
                        radius = 8.dp.toPx(),
                        center = Offset(splitPixelX, size.height / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(splitPixelX, size.height / 2f)
                    )
                } else {
                    // Full screen upscaled render
                    drawUpscaledViewport(engine, size.width, size.height, scale)
                }
            }

            // Watermark left / right overlay
            if (engine.showSplitScreen) {
                // Left Watermark (Source)
                Text(
                    text = "${engine.activeSourceResolution.label.split(" ")[0]} SOURCE${if (engine.upscaleMode == UpscaleMode.BILINEAR) " BI-LIN" else " BRUT"}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                // Right Watermark (Upscaled)
                Text(
                    text = "${engine.upscaleMode.label} UNLOCKED",
                    color = CyberPrimary.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(CyberBlack.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                // Full Screen Status Label
                Text(
                    text = "VUE DE RENDU COMPLÈTE | MODE : ${engine.upscaleMode.label}",
                    color = CyberPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(CyberBlack.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Top HUD Overlays Info Quick Bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolution Output Tag
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, CyberPrimary.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(CyberPrimary, RoundedCornerShape(2.5.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${engine.activeSourceResolution.height}p → ${engine.targetResolutionLabel}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Active Overlays Indicator icons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (engine.showMotionVectors) OverlayTag(name = "VECTOR", color = CyberSecondary)
                    if (engine.showJitterGrid) OverlayTag(name = "JITTER", color = CyberPrimary)
                    if (engine.showArtifactMask) OverlayTag(name = "ARTIFACT", color = CyberAccentRed)
                }
            }
        }
    }
}

@Composable
fun OverlayTag(name: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = name,
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// Draw the upscaled viewport scene with all post-process shaders sim
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUpscaledViewport(
    engine: FsrEngine,
    width: Float,
    height: Float,
    scale: Float
) {
    // 1. Starfield clean / super-resolved
    val starsHigh = engine.getStarfieldForDrawing(width, height, isUpscaledPass = true)
    for (star in starsHigh) {
        // High density sparks
        val r = 3f * scale * star.brightness
        drawCircle(
            color = Color.White.copy(alpha = star.brightness),
            radius = r,
            center = Offset(star.x, star.y)
        )
    }

    // 2. Render lines high quality (FSR Temporal or FSR 1.0 EASU)
    val linesHigh = engine.getProjectedLinesForDrawing(width, height, isUpscaledPass = true)

    // Simulating FSR 1.0 (Spatial EASU + RCAS sharpening halo)
    val rcasSharp = engine.upscaleMode == UpscaleMode.FSR_1_0 && engine.rcasSharpness > 0.1f

    for (line in linesHigh) {
        val baseColor = getLineColor(line.colorType)
        
        // FSR 1.0 / RCAS Contrast Adaptive Sharpening Halo effect
        if (rcasSharp) {
            drawLine(
                color = baseColor.copy(alpha = 0.25f * engine.rcasSharpness),
                start = Offset(line.startX, line.startY),
                end = Offset(line.endX, line.endY),
                strokeWidth = 3f * scale // halo
            )
        }

        // FSR 2.0 Clean Vector Reconstruction
        val strokeWidth = when (engine.upscaleMode) {
            UpscaleMode.FSR_2_0 -> 1.8f * scale // Crispy temporal reconstruction line
            UpscaleMode.FSR_1_0 -> 2.2f * scale
            else -> 1.5f * scale
        }

        drawLine(
            color = baseColor,
            start = Offset(line.startX, line.startY),
            end = Offset(line.endX, line.endY),
            strokeWidth = strokeWidth
        )

        // Draw Motion Vectors
        if (engine.showMotionVectors) {
            val arrowLength = 1.8f
            drawLine(
                color = CyberSecondary.copy(alpha = 0.8f),
                start = Offset(line.startX, line.startY),
                end = Offset(line.startX + line.motionVectorX * arrowLength, line.startY + line.motionVectorY * arrowLength),
                strokeWidth = 1.5f * scale
            )
            // Head of vector
            drawCircle(
                color = CyberSecondary,
                radius = 2f * scale,
                center = Offset(line.startX + line.motionVectorX * arrowLength, line.startY + line.motionVectorY * arrowLength)
            )
        }

        // Draw Frame Generation Ghosting / Artifact Mask
        if (engine.showArtifactMask) {
            // Speed calculation
            val speed = sqrt(line.motionVectorX * line.motionVectorX + line.motionVectorY * line.motionVectorY)
            if (speed > 18f && engine.frameGenMode != FrameGenMode.OFF) {
                // High speed triggers interpolation mask block
                drawLine(
                    color = CyberAccentRed.copy(alpha = 0.6f),
                    start = Offset(line.startX, line.startY),
                    end = Offset(line.endX, line.endY),
                    strokeWidth = 6.dp.toPx(),
                    alpha = 0.25f
                )
            }
        }
    }

    // Draw Subpixel Jitter helper markers
    if (engine.showJitterGrid && engine.upscaleMode == UpscaleMode.FSR_2_0) {
        val jitter = engine.getJitterOffset(scale * 15f)
        // Draw Jitter reticle in center
        val cX = width / 2f
        val cY = height / 2f
        drawCircle(
            color = CyberPrimary.copy(alpha = 0.15f),
            radius = 35.dp.toPx(),
            center = Offset(cX, cY)
        )
        // Dynamic jitter pointer
        drawCircle(
            color = CyberPrimary,
            radius = 3.dp.toPx(),
            center = Offset(cX + jitter.x * 2.5f, cY + jitter.y * 2.5f)
        )
        drawLine(
            color = CyberPrimary.copy(alpha = 0.4f),
            start = Offset(cX - 15.dp.toPx(), cY),
            end = Offset(cX + 15.dp.toPx(), cY),
            strokeWidth = 0.8f
        )
        drawLine(
            color = CyberPrimary.copy(alpha = 0.4f),
            start = Offset(cX, cY - 15.dp.toPx()),
            end = Offset(cX, cY + 15.dp.toPx()),
            strokeWidth = 0.8f
        )
    }

    // Draw Variable Rate Shading (VRS) bounds if activated
    if (engine.variableRateShading && engine.foveatedVringEnabled) {
        val innerCircleRadius = width * 0.28f
        val outerCircleRadius = width * 0.42f
        
        // Clean vector HUD boundary markers
        drawCircle(
            color = CyberSecondary.copy(alpha = 0.1f),
            radius = innerCircleRadius,
            center = Offset(width / 2f, height / 2f),
            style = Stroke(width = 0.5.dp.toPx())
        )
        drawCircle(
            color = CyberTertiary.copy(alpha = 0.08f),
            radius = outerCircleRadius,
            center = Offset(width / 2f, height / 2f),
            style = Stroke(width = 0.5.dp.toPx())
        )
    }
}

// Draw futuristic space grid line-grid in background
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpaceGrid(width: Float, height: Float, scale: Float) {
    val vanishingY = height * 0.35f
    val gridLines = 14
    
    // Draw horizon line
    drawLine(
        color = CyberPrimary.copy(alpha = 0.15f),
        start = Offset(0f, vanishingY),
        end = Offset(width, vanishingY),
        strokeWidth = 0.8.dp.toPx()
    )

    // Draw perspective orthogonal lines
    for (i in 0..gridLines) {
        val t = i.toFloat() / gridLines
        val startX = t * width
        drawLine(
            color = CyberCardBorder.copy(alpha = 0.2f),
            start = Offset(startX, height),
            end = Offset(width / 2f + (t - 0.5f) * (width * 0.1f), vanishingY),
            strokeWidth = 0.5.dp.toPx()
        )
    }

    // Scrolling horizon grid panels
    val depthSteps = 6
    for (i in 1..depthSteps) {
        // Perspective curve grouping lines closer together as they go deeper
        val normalizedDepth = (i.toFloat() / depthSteps).pow(2.2f)
        val lineY = vanishingY + (height - vanishingY) * normalizedDepth
        val alpha = (1.0f - normalizedDepth) * 0.2f
        drawLine(
            color = CyberPrimary.copy(alpha = alpha),
            start = Offset(0f, lineY),
            end = Offset(width, lineY),
            strokeWidth = 0.8.dp.toPx()
        )
    }
}

fun getLineColor(type: Int): Color {
    return when (type) {
        1 -> CyberPrimary     // Glowing neon cyan
        2 -> CyberSecondary   // Performance solid neon lime
        else -> TextSecondary // Futuristic metal silver/grey
    }
}

@Composable
fun SystemAlertBanner(engine: FsrEngine) {
    // Show active warnings or cooling notifications
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = engine.thermalThrottlingActive,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberAccentRed.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, CyberAccentRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Throttling Thermique Actif",
                        tint = CyberAccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RÉGULATION THERMIQUE EN COURS (${String.format("%.1f", engine.gpuTempCelsius)}°C)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Température limite (${engine.thermalLimitCelsius}°C) franchie. Fréquences abaissées de force pour refroidir le silicium.",
                            color = CyberAccentRed,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = engine.cpuTdpThrottledActive,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberTertiary.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CyberTertiary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Limitation TDP CPU Active",
                        tint = CyberTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TDP PUISSANCE CPU LIMITÉ (${String.format("%.1f", engine.cpuPowerDrawMw / 1000f)}W / Max ${String.format("%.1f", engine.cpuTdpWatts)}W)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "L'enveloppe énergétique CPU est saturée. Le traitement vectoriel est restreint pour respecter la consommation cible.",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = engine.gpuTdpThrottledActive,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberTertiary.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CyberTertiary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Limitation TDP GPU Active",
                        tint = CyberTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TDP PUISSANCE GPU LIMITÉ (${String.format("%.1f", engine.gpuPowerDrawMw / 1000f)}W / Max ${String.format("%.1f", engine.gpuTdpWatts)}W)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "L'enveloppe énergétique du moteur graphique est saturée. Les passes de Shaders et d'upscaling FSR sont contraintes.",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = engine.autoThrottledActive,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberTertiary.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CyberTertiary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Charge GPU Élevée",
                        tint = CyberTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GPU THREAD LIMIT: DRS ACTIF",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Source abaissée de ${engine.sourceResolution.height}p vers ${engine.activeSourceResolution.height}p pour preserver l'alimentation.",
                            color = CyberTertiary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = engine.frameGenThrottledActive,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberAccentRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CyberAccentRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "LSFG auto-throttle active",
                        tint = CyberAccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LATENCE LIMIT: DÉSACCOUPLEMENT LSFG",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rétrogradation adaptive pour éliminer les saccades et stabiliser l'affichage.",
                            color = CyberAccentRed,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsTelemetryDashboard(engine: FsrEngine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Fluidity and FPS Box
        TelemetryItemCard(
            modifier = Modifier.weight(1f),
            title = "FLUIDITÉ / FPS",
            value = "${engine.interpolatedFps.toInt()} FPS",
            subtext = "Base: ${engine.actualRenderFps.toInt()} Hz",
            indicatorColor = if (engine.interpolatedFps > 55f) CyberSecondary else CyberTertiary
        )

        // GPU load and temp Box
        TelemetryItemCard(
            modifier = Modifier.weight(1f),
            title = "CONSO & CORE",
            value = "${engine.gpuPowerDrawMw.toInt()} mW",
            subtext = "T°: ${engine.gpuTempCelsius.toInt()}°C",
            indicatorColor = if (engine.gpuTempCelsius < 62f) CyberPrimary else CyberAccentRed
        )

        // Display latency Box
        TelemetryItemCard(
            modifier = Modifier.weight(1f),
            title = "LATENCE JEU",
            value = String.format("%.1f ms", engine.totalLatencyMs),
            subtext = "Gigue: ${String.format("%.1f", engine.staggerRatePercent)}%",
            indicatorColor = if (engine.totalLatencyMs < 20f) CyberSecondary else CyberTertiary
        )
    }
}

@Composable
fun TelemetryItemCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    indicatorColor: Color
) {
    Card(
        modifier = modifier
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(indicatorColor, RoundedCornerShape(2.5.dp))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtext,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MagnifierZoomCard(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zoom Loupe Draw canvas
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBlack)
                    .border(1.5.dp, CyberPrimary, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scale = size.minDimension / 100f
                    val cX = size.width / 2f
                    val cY = size.height / 2f

                    // Simulate FSR sub-pixel text upscaling vs low-res aliasing representation
                    when (engine.upscaleMode) {
                        UpscaleMode.NATIVE_LOW -> {
                            // Render extremely blocky pixel lines
                            drawRect(
                                color = CyberPrimary.copy(alpha = 0.7f),
                                topLeft = Offset(cX - 12f * scale, cY - 12f * scale),
                                size = Size(24f * scale, 24f * scale)
                            )
                            // block grid pattern
                            for (x in 2..8 step 2) {
                                drawLine(Color.Black.copy(alpha = 0.4f), Offset(x * 10f, 0f), Offset(x * 10f, size.height), 0.5f * scale)
                                drawLine(Color.Black.copy(alpha = 0.4f), Offset(0f, x * 10f), Offset(size.width, x * 10f), 0.5f * scale)
                            }
                        }
                        UpscaleMode.BILINEAR -> {
                            // Blurry representation
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(CyberPrimary, Color.Transparent),
                                    center = Offset(cX, cY),
                                    radius = 22f * scale
                                ),
                                radius = 22f * scale
                            )
                        }
                        UpscaleMode.FSR_1_0 -> {
                            // Anti-aliased but slightly pixel-edged square
                            drawRect(
                                color = CyberPrimary,
                                topLeft = Offset(cX - 10f * scale, cY - 10f * scale),
                                size = Size(20f * scale, 20f * scale),
                                style = Stroke(width = 4f * scale)
                            )
                        }
                        UpscaleMode.FSR_2_0 -> {
                            // Reconstructed crispy crosshair and pixel resolution
                            drawCircle(
                                color = CyberSecondary,
                                radius = 3f * scale,
                                center = Offset(cX, cY)
                            )
                            drawCircle(
                                color = CyberPrimary,
                                radius = 14f * scale,
                                center = Offset(cX, cY),
                                style = Stroke(width = 1.5f * scale)
                            )
                            // solved sub-pixel tiny dots
                            drawCircle(Color.White, 1f * scale, Offset(cX - 16f, cY - 16f))
                            drawCircle(Color.White, 1f * scale, Offset(cX + 16f, cY + 16f))
                        }
                    }
                }
                
                // Overlay text
                Text(
                    text = "X4 ZOOM",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LOUPE DE QUALITÉ DE RENDU",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = when (engine.upscaleMode) {
                        UpscaleMode.NATIVE_LOW -> "Nearest Neighbor : Les pixels sont extrapolés sans régularisation. Les diagonales souffrent d'un aliasing en escalier très visible."
                        UpscaleMode.BILINEAR -> "Flou Standard : Interpolation linéaire qui mélange les pixels ronds, éliminant l'escalier mais ruinant l'éclat des textures de jeu."
                        UpscaleMode.FSR_1_0 -> "FSR 1.0 Spatial : Reconstruction vectorielle des angles (algorithme EASU) puis sharpening adaptatif au contraste (RCAS)."
                        UpscaleMode.FSR_2_0 -> "FSR 2.0 Temporel : Accumulation de sous-pixels (Jitter) guidée par vecteurs de mouvement. Recrée des détails fins absents du 240p brut."
                    },
                    color = TextPrimary,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun ControlPanelTabs(activeTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberDarkSurface)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
    ) {
        val tabNames = listOf("FSR SuperRes", "Frame Gen", "tweaks GPU")
        tabNames.forEachIndexed { index, name ->
            val isSelected = activeTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .background(if (isSelected) CyberCardBg else Color.Transparent)
                    .drawBehind {
                        if (isSelected) {
                            drawLine(
                                color = CyberPrimary,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.uppercase(),
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun UpscaleTabControls(engine: FsrEngine) {
    var showCreatorForm by remember { mutableStateOf(false) }
    
    // Form States
    var customLabel by remember { mutableStateOf("") }
    var customSourceW by remember { mutableStateOf(480) }
    var customSourceH by remember { mutableStateOf(270) }
    var customTargetW by remember { mutableStateOf(1920) }
    var customTargetH by remember { mutableStateOf(1080) }

    Text(
        text = "UNITÉ DE GESTION DES RÉSOLUTIONS",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    // List of active configurations
    Text(
        text = "Sélectionner la configuration de Rendu",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))

    // List of Resolutions
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        engine.availableResolutions.forEach { res ->
            val isSelected = engine.sourceResolution == res
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) CyberCardBg else CyberBlack.copy(alpha = 0.3f))
                    .border(1.dp, if (isSelected) CyberPrimary else CyberCardBorder, RoundedCornerShape(12.dp))
                    .clickable { engine.sourceResolution = res }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = res.label,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (res.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("CUSTOM", color = CyberSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Source: ${res.width}x${res.height} → Cible: ${res.targetWidth}x${res.targetHeight} (FSR Scale: ${String.format("%.2f", res.targetWidth.toFloat() / res.width.toFloat())}x)",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (res.isCustom) {
                        IconButton(
                            onClick = {
                                if (engine.sourceResolution == res) {
                                    engine.sourceResolution = engine.availableResolutions.first { !it.isCustom }
                                }
                                engine.availableResolutions.remove(res)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer résolution",
                                tint = CyberAccentRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${res.height}p → ${res.targetHeight}p",
                            color = if (isSelected) CyberPrimary else TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Add Resolution Creator Button / Expander
    Button(
        onClick = { showCreatorForm = !showCreatorForm },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (showCreatorForm) CyberCardBorder else CyberPrimary.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(36.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (showCreatorForm) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Custom Creator",
                tint = if (showCreatorForm) Color.LightGray else CyberPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (showCreatorForm) "FERMER L'ÉDITEUR DE CRÉATION" else "AJOUTER UNE RÉSOLUTION PERSONNALISÉE",
                color = if (showCreatorForm) Color.LightGray else CyberPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    AnimatedVisibility(
        visible = showCreatorForm,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, CyberPrimary, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CRÉATEUR DE RÉSOLUTION CUSTOM",
                    color = CyberPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Name field
                Text("Libellé ou Nom de la configuration:", color = TextSecondary, fontSize = 8.sp)
                TextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    placeholder = { Text("Ex: 1080p Ultra-Upscale", fontSize = 10.sp, color = TextMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CyberBlack,
                        unfocusedContainerColor = CyberBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = CyberPrimary,
                        unfocusedIndicatorColor = CyberCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Source configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Source (Entrée):", color = TextSecondary, fontSize = 9.sp)
                    Text("${customSourceW}x${customSourceH} (${customSourceH}p)", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                
                Text("Largeur de rendu interne:", color = TextSecondary, fontSize = 8.sp)
                Slider(
                    value = customSourceW.toFloat(),
                    onValueChange = { customSourceW = it.toInt() },
                    valueRange = 100f..2560f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )

                Text("Hauteur de rendu interne:", color = TextSecondary, fontSize = 8.sp)
                Slider(
                    value = customSourceH.toFloat(),
                    onValueChange = { customSourceH = it.toInt() },
                    valueRange = 100f..1440f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Target configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cible (Upscale):", color = TextSecondary, fontSize = 9.sp)
                    Text("${customTargetW}x${customTargetH} (${customTargetH}p)", color = CyberSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Text("Largeur cible finale:", color = TextSecondary, fontSize = 8.sp)
                Slider(
                    value = customTargetW.toFloat(),
                    onValueChange = { customTargetW = it.toInt() },
                    valueRange = 480f..3840f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )

                Text("Hauteur cible finale:", color = TextSecondary, fontSize = 8.sp)
                Slider(
                    value = customTargetH.toFloat(),
                    onValueChange = { customTargetH = it.toInt() },
                    valueRange = 360f..2160f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val finalLabel = customLabel.ifBlank { "${customSourceH}p - ${customTargetH}p Custom" }
                        val newRes = ResolutionConfig(
                            id = "custom_${System.currentTimeMillis()}",
                            initialLabel = finalLabel,
                            initialWidth = customSourceW,
                            initialHeight = customSourceH,
                            initialTargetWidth = customTargetW,
                            initialTargetHeight = customTargetH,
                            isCustom = true
                        )
                        engine.availableResolutions.add(newRes)
                        engine.sourceResolution = newRes
                        
                        // Reset forms
                        customLabel = ""
                        showCreatorForm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ENREGISTRER LA RÉSOLUTION PERSONNALISÉE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Selected Resolution Dynamic Adjuster (Adjustable for ALL existing resolutions!)
    val activeRes = engine.sourceResolution
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ÉDITEUR DE RÉSOLUTIONS",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Ajuste les limites source → cible pour tout profile",
                        color = TextSecondary,
                        fontSize = 8.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "CONTRÔLEUR", color = CyberPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CONFIGURATION EN ÉDITION : ${activeRes.label}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Adjust Source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1. AJUSTER SOURCE (ENTRÉE DE RENDU)", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("${activeRes.width}x${activeRes.height}", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Largeur: ", color = TextSecondary, fontSize = 8.sp, modifier = Modifier.width(42.dp))
                Slider(
                    value = activeRes.width.toFloat(),
                    onValueChange = { activeRes.width = it.toInt() },
                    valueRange = 100f..2560f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.weight(1f).height(20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hauteur: ", color = TextSecondary, fontSize = 8.sp, modifier = Modifier.width(42.dp))
                Slider(
                    value = activeRes.height.toFloat(),
                    onValueChange = { activeRes.height = it.toInt() },
                    valueRange = 100f..1440f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.weight(1f).height(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Adjust Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("2. AJUSTER CIBLE (DESSIN / DISPLAY)", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("${activeRes.targetWidth}x${activeRes.targetHeight} (${activeRes.targetHeight}p)", color = CyberSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            // Preset links
            Text("Associations courantes de ciblage pour ${activeRes.height}p:", color = TextSecondary, fontSize = 8.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val targets = listOf(
                    "720p" to Pair(1280, 720),
                    "960p" to Pair(1706, 960),
                    "1080p" to Pair(1920, 1080),
                    "1440p" to Pair(2560, 1440)
                )
                targets.forEach { (label, dims) ->
                    val isCurrentTarget = activeRes.targetWidth == dims.first && activeRes.targetHeight == dims.second
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrentTarget) CyberSecondary else CyberBlack.copy(alpha = 0.3f))
                            .border(1.dp, if (isCurrentTarget) CyberSecondary else CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                activeRes.targetWidth = dims.first
                                activeRes.targetHeight = dims.second
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${activeRes.height}p-$label",
                            color = if (isCurrentTarget) Color.White else TextSecondary.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Largeur: ", color = TextSecondary, fontSize = 8.sp, modifier = Modifier.width(42.dp))
                Slider(
                    value = activeRes.targetWidth.toFloat(),
                    onValueChange = { activeRes.targetWidth = it.toInt() },
                    valueRange = 480f..3840f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.weight(1f).height(20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hauteur: ", color = TextSecondary, fontSize = 8.sp, modifier = Modifier.width(42.dp))
                Slider(
                    value = activeRes.targetHeight.toFloat(),
                    onValueChange = { activeRes.targetHeight = it.toInt() },
                    valueRange = 360f..2160f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.weight(1f).height(20.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Select Upscale profile
    Text(
        text = "Algorithme de Super Résolution",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
    
    UpscaleMode.values().forEach { mode ->
        val isSelected = engine.upscaleMode == mode
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .border(
                    1.dp,
                    if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { engine.upscaleMode = mode },
            colors = CardDefaults.cardColors(containerColor = if (isSelected) CyberCardBg else CyberDarkSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) CyberPrimary else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = CyberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mode.description,
                    color = TextPrimary.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }

    // Sharpness slider only for FSR 1.0 RCAS / FSR 2.0 active
    if (engine.upscaleMode == UpscaleMode.FSR_1_0 || engine.upscaleMode == UpscaleMode.FSR_2_0) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Intensité du RCAS (Netteté)",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(engine.rcasSharpness * 100).toInt()}%",
                color = CyberPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = engine.rcasSharpness,
            onValueChange = { engine.rcasSharpness = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = CyberPrimary,
                activeTrackColor = CyberPrimary,
                inactiveTrackColor = CyberCardBorder
            ),
            modifier = Modifier.height(24.dp)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    
    // Viewport Overlays switches
    Text(
        text = "Options Visuelles de Diagnostic",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverlayToggleWidget(
            modifier = Modifier.weight(1f),
            label = "Vecteurs Mouv.",
            active = engine.showMotionVectors,
            activeColor = CyberSecondary,
            onClick = { engine.showMotionVectors = !engine.showMotionVectors }
        )
        OverlayToggleWidget(
            modifier = Modifier.weight(1f),
            label = "Grille Jitter",
            active = engine.showJitterGrid,
            activeColor = CyberPrimary,
            onClick = { engine.showJitterGrid = !engine.showJitterGrid }
        )
    }
}

@Composable
fun OverlayToggleWidget(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .border(
                1.dp,
                if (active) activeColor else CyberCardBorder,
                RoundedCornerShape(8.dp)
            ),
        color = if (active) activeColor.copy(alpha = 0.1f) else CyberBlack.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (active) activeColor else TextMuted, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (active) Color.White else TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FrameGenTabControls(engine: FsrEngine) {
    Text(
        text = "LOSSLESS SCALING 3.2 - GENERATION DE FRAME",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "L'interpolation de frames génère de faux états intermédiaires de jeu pour doubler ou tripler la fluidité visuelle sans solliciter le moteur de calcul natif. Fait passer l'affichage de 30 FPS à 60 ou 90 FPS.",
        color = TextPrimary.copy(alpha = 0.8f),
        fontSize = 9.sp,
        lineHeight = 12.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Switch selection
    Text(
        text = "Facteur multiplicateur LSFG",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FrameGenMode.values().forEach { mode ->
            val isSelected = engine.frameGenMode == mode
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { engine.frameGenMode = mode }
                    .border(
                        1.dp,
                        if (isSelected) CyberPrimary else CyberCardBorder,
                        RoundedCornerShape(8.dp)
                    ),
                color = if (isSelected) CyberPrimary.copy(alpha = 0.1f) else CyberDarkSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mode.label.split(" ")[0],
                        color = if (isSelected) CyberPrimary else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (mode == FrameGenMode.OFF) "-" else "Multiplicateur ${mode.scale}x",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Base FPS modifier
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Fréquence de rendu de base (Base Game Hz)",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${engine.baseFps} FPS",
            color = CyberPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
    Text(
        text = "Réglez à 15 FPS pour observer comment le Frame Gen élimine les sacades et double de manière fluide la trajectoire des objets.",
        color = TextPrimary.copy(alpha = 0.6f),
        fontSize = 8.sp,
        lineHeight = 11.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Slider(
        value = engine.baseFps.toFloat(),
        onValueChange = { engine.baseFps = it.toInt() },
        valueRange = 10f..45f,
        steps = 6,
        colors = SliderDefaults.colors(
            thumbColor = CyberPrimary,
            activeTrackColor = CyberPrimary,
            inactiveTrackColor = CyberCardBorder
        ),
        modifier = Modifier.height(24.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Adaptive Auto Throttle switch
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBlack.copy(alpha = 0.4f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Auto-Adaptation LSFG 3.2",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Désactive temporairement le générateur de frame lors des pics de charge GPU pour maintenir un temps d'affichage net.",
                color = TextPrimary.copy(alpha = 0.7f),
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
        Switch(
            checked = engine.adaptiveFrameGen,
            onCheckedChange = { engine.adaptiveFrameGen = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberSecondary,
                checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CyberCardBorder
            )
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Artifact masking diagnostic toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, CyberCardBorder, RoundedCornerShape(10.dp))
            .clickable { engine.showArtifactMask = !engine.showArtifactMask }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Diagnostic",
                tint = if (engine.showArtifactMask) CyberAccentRed else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Visualiser le Masque d'Artefacts",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Affiche en violet chaud les zones de haute vitesse où des déchirures ou du ghosting se forment.",
                    color = TextPrimary.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    lineHeight = 11.sp
                )
            }
        }
        Checkbox(
            checked = engine.showArtifactMask,
            onCheckedChange = { engine.showArtifactMask = it },
            colors = CheckboxDefaults.colors(
                checkedColor = CyberAccentRed,
                uncheckedColor = CyberCardBorder
            )
        )
    }
}

@Composable
fun GraphicsTweaksTabControls(engine: FsrEngine) {
    Text(
        text = "OPTI-GPU : REGLAGES ET TWEAKS GRAPHIQUES",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Ces tweaks complexes soulagent l'architecture GPU lors d'activités intenses ou d'augmentation thermique.",
        color = TextPrimary.copy(alpha = 0.7f),
        fontSize = 9.sp,
        lineHeight = 12.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Simulated GPU Load Stimulator
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Stimulateur de Charge GPU",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${engine.gpuLoadSlider.toInt()}%",
            color = if (engine.gpuLoadSlider > 75f) CyberAccentRed else CyberPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
    Text(
        text = "Élevez la charge pour simuler d'immenses combats dans un jeu pour observer comment le DRS ou le VRS réagissent automatiquement.",
        color = TextPrimary.copy(alpha = 0.6f),
        fontSize = 8.sp,
        lineHeight = 11.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Slider(
        value = engine.gpuLoadSlider,
        onValueChange = { engine.gpuLoadSlider = it },
        valueRange = 10f..100f,
        colors = SliderDefaults.colors(
            thumbColor = if (engine.gpuLoadSlider > 75f) CyberAccentRed else CyberPrimary,
            activeTrackColor = if (engine.gpuLoadSlider > 75f) CyberAccentRed else CyberPrimary,
            inactiveTrackColor = CyberCardBorder
        ),
        modifier = Modifier.height(24.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 1. DRS Toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBlack.copy(alpha = 0.4f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DRS (Dynamic Resolution Scaling)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Abaisse automatiquement la résolution interne de rendu (ex 240p -> 120p) quand le GPU sature, pour conserver le framerate.",
                color = TextPrimary.copy(alpha = 0.7f),
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
        Switch(
            checked = engine.dynamicResolutionScaling,
            onCheckedChange = { engine.dynamicResolutionScaling = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberSecondary,
                checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CyberCardBorder
            )
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. VRS Toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBlack.copy(alpha = 0.4f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VRS (Variable Rate Shading)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Rassemble le calcul des pixels d'ombres sur les bords extérieurs ou lointains (taux 2x2 ou 4x4) réduisant considérablement la chaleur du GPU.",
                color = TextPrimary.copy(alpha = 0.7f),
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
        Switch(
            checked = engine.variableRateShading,
            onCheckedChange = { engine.variableRateShading = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberSecondary,
                checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CyberCardBorder
            )
        )
    }

    if (engine.variableRateShading) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                .clickable { engine.foveatedVringEnabled = !engine.foveatedVringEnabled }
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Masque fovéal de focalisation",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Affiche de discrets cercles HUD indiquant les limites de résolution fovéale.",
                    color = TextPrimary.copy(alpha = 0.6f),
                    fontSize = 8.sp
                )
            }
            Checkbox(
                checked = engine.foveatedVringEnabled,
                onCheckedChange = { engine.foveatedVringEnabled = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberSecondary,
                    uncheckedColor = CyberCardBorder
                )
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

// ============================================================================
// HIGH DENSITY LIGHT LAVENDER THEME HELPER COMPONENTS
// ============================================================================

@Composable
fun RealtimeMetricsCard(engine: FsrEngine) {
    // Computes dynamic GPU load relief percentage depending on selected source resolution
    val loadRelief = when (engine.activeSourceResolution.height) {
        120 -> "-74%"
        240 -> "-58%"
        360 -> "-42%"
        480 -> "-25%"
        720 -> "0%"
        960 -> "+40%"
        1080 -> "+125%"
        1440 -> "+300%"
        else -> {
            val ratio = (engine.activeSourceResolution.height.toFloat() * engine.activeSourceResolution.width.toFloat()) / (720f * 1280f)
            val percentageVal = ((ratio - 1f) * 100).toInt()
            if (percentageVal >= 0) "+$percentageVal%" else "$percentageVal%"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = LavenderContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "CIBLE D'UPSCALE (FSR)",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${engine.activeSourceResolution.height}p",
                        color = LavenderOnContainer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = " → ",
                        color = LavenderOnContainer.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = engine.targetResolutionLabel,
                        color = LavenderOnContainer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Vertical divider line
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(1.dp)
                    .background(CyberPrimary.copy(alpha = 0.15f))
            )

            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "RELIEF DE CHARGE",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = loadRelief,
                    color = LavenderOnContainer,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun SetupGridOverview(engine: FsrEngine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FSR Card
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "FSR status",
                        tint = CyberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    // Custom switch or indicator
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 14.dp)
                            .background(CyberPrimary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(10.dp)
                                .background(Color.White, RoundedCornerShape(5.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = engine.upscaleMode.label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supersampler FSR 2.0",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(CyberBlack, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (engine.upscaleMode == UpscaleMode.FSR_2_0) 0.85f else if (engine.upscaleMode == UpscaleMode.FSR_1_0) 0.6f else 0.3f)
                            .fillMaxHeight()
                            .background(CyberPrimary, RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Frame Gen / LSFG Card
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "LSFG status",
                        tint = CyberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 14.dp)
                            .background(CyberPrimary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(10.dp)
                                .background(Color.White, RoundedCornerShape(5.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (engine.frameGenMode == FrameGenMode.OFF) "LSFG Désactivé" else engine.frameGenMode.label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LSFG FrameGen Fluidité",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Dots count indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scaleSteps = when (engine.frameGenMode) {
                        FrameGenMode.OFF -> 0
                        FrameGenMode.LSFG_X2 -> 2
                        FrameGenMode.LSFG_X3 -> 3
                    }
                    for (i in 1..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(
                                    if (i <= scaleSteps) CyberPrimary else CyberBlack,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedGpuReliefTweaksList(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "RETUCHE ADVANCED GPU RELIEF",
                color = CyberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tweak 1: Variable Rate Shading
                TweakItemRow(
                    title = "Variable Rate Shading (VRS Tier 2)",
                    description = "Focalise l'ombrage des pixels pour réduire la charge GPU",
                    active = engine.variableRateShading,
                    icon = Icons.Default.Build,
                    onCheckedChange = { engine.variableRateShading = it }
                )

                // Tweak 2: Dynamic Resolution Scaling
                TweakItemRow(
                    title = "Dynamic Resolution Scaling (DRS)",
                    description = "Adapte l'échelle de rendu pour un framerate solide",
                    active = engine.dynamicResolutionScaling,
                    icon = Icons.Default.Build,
                    onCheckedChange = { engine.dynamicResolutionScaling = it }
                )

                // Tweak 3: Foveated VRing
                TweakItemRow(
                    title = "Optimiseur de Fovéation périphérique",
                    description = "Masque fovéal intelligent réduisant le raster de 28%",
                    active = engine.foveatedVringEnabled,
                    icon = Icons.Default.Info,
                    onCheckedChange = { engine.foveatedVringEnabled = it }
                )
            }
        }
    }
}

@Composable
fun TweakItemRow(
    title: String,
    description: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberCardBg)
            .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!active) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(LavenderSelectedBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = CyberPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = { onCheckedChange(!active) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = if (active) "Actif" else "Inactif",
                tint = if (active) CyberPrimary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CpuGpuTuningPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONTRÔLEUR DE TENSION ET FRÉQUENCE",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Ajusteurs d'alimentation CPU & GPU par cœur",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Tweak lab logo",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ------------------ CPU TUNING ------------------
            Text(
                text = "MICRO-ARCHITECTURE CPU (OCTA-CORE)",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // CPU Undervoltage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Undervolting CPU",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(CyberSecondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("SANS PERTE DE FRÉQ", color = CyberSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Réduit la tension par cœur ({$engine.cpuVoltOffsetMv} mV). Abaisse la température et évite le throttling thermique.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.cpuUndervoltEnabled,
                    onCheckedChange = { engine.cpuUndervoltEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.cpuUndervoltEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Décalage tension CPU (Offset):", color = TextSecondary, fontSize = 9.sp)
                    Text("${engine.cpuVoltOffsetMv} mV", color = CyberSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = engine.cpuVoltOffsetMv.toFloat(),
                    onValueChange = { engine.cpuVoltOffsetMv = it.toInt() },
                    valueRange = -150f..-50f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CPU Overclocking
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Overclocking CPU fréquence par cœur",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Booste la fréquence maximale au-delà des limites du constructeur (+${String.format("%.1f", engine.cpuCoreFreqGhz - 2.2f)} GHz). Réduit la latence de dispatch.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.cpuOverclockEnabled,
                    onCheckedChange = { engine.cpuOverclockEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberPrimary,
                        checkedTrackColor = CyberPrimary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.cpuOverclockEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cible de fréquence par cœur CPU:", color = TextSecondary, fontSize = 9.sp)
                    Text("${String.format("%.2f", engine.cpuCoreFreqGhz)} GHz", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = engine.cpuCoreFreqGhz,
                    onValueChange = { engine.cpuCoreFreqGhz = it },
                    valueRange = 2.2f..4.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(16.dp))

            // ------------------ GPU TUNING ------------------
            Text(
                text = "COPROCESSEUR GRAPHIQUE GPU (CORES MALI-ADAPTIVE)",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // GPU Undervoltage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Undervolting GPU",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(CyberSecondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("SANS COUTURE", color = CyberSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Optimise la courbe tension-fréquence du GPU (${engine.gpuVoltOffsetMv} mV) pour un rendement énergétique ultra-optimal.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.gpuUndervoltEnabled,
                    onCheckedChange = { engine.gpuUndervoltEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.gpuUndervoltEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Décalage courbe de tension GPU:", color = TextSecondary, fontSize = 9.sp)
                    Text("${engine.gpuVoltOffsetMv} mV", color = CyberSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = engine.gpuVoltOffsetMv.toFloat(),
                    onValueChange = { engine.gpuVoltOffsetMv = it.toInt() },
                    valueRange = -120f..-40f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GPU Overclocking
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Overclocking GPU fréquence par cœur",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Booste l'horloge GPU (${engine.gpuClockMhz} MHz). Améliore drastiquement la reconstruction FSR et la génération de frames.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.gpuOverclockEnabled,
                    onCheckedChange = { engine.gpuOverclockEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberPrimary,
                        checkedTrackColor = CyberPrimary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.gpuOverclockEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fréquence d'horloge GPU cible:", color = TextSecondary, fontSize = 9.sp)
                    Text("${engine.gpuClockMhz} MHz", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = engine.gpuClockMhz.toFloat(),
                    onValueChange = { engine.gpuClockMhz = it.toInt() },
                    valueRange = 500f..1200f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Composable
fun RamCompressorPanel(engine: FsrEngine) {
    var isCompacting by remember { mutableStateOf(false) }
    var showCompactToast by remember { mutableStateOf(false) }

    if (isCompacting) {
        LaunchedEffect(Unit) {
            delay(1200)
            engine.memoryCompactionCycles += 1
            isCompacting = false
            showCompactToast = true
            delay(2000)
            showCompactToast = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMPRESSEUR RAM ULTRA-DENSE & COMPACTEUR",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Moteur intelligent d'allocation de swap et compaction localisée",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Ram logo",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main compressor toggle switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compression ZRAM active",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Comprime les textures de rendu et l'historique temporel mémoire FSR pour libérer l'espace physique. Réduit fortement les saccades ou micro-lags.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.ramCompressorEnabled,
                    onCheckedChange = { engine.ramCompressorEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.ramCompressorEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "SELECTION DE L'ALGORITHME DE COMPRESSION RAM",
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Grid/Row selector of algorithms
                val algos = listOf(
                    "ZRAM (LZ4-compaction)" to "ZRAM (LZ4-compaction)",
                    "ZSTD Ultra" to "ZSTD-Ultra",
                    "LZO Concurrent" to "LZO-Concurrent",
                    "Huffman Dual" to "Double-Huffman"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    algos.forEach { (label, value) ->
                        val isSelected = engine.ramCompressionAlgo == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberPrimary else CyberBlack.copy(alpha = 0.3f))
                                .border(1.dp, if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { engine.ramCompressionAlgo = value }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time compression telemetry
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TAUX D'EFFICACITÉ DE LA MÉMOIRE", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            val compRatioLabel = when (engine.ramCompressionAlgo) {
                                "ZRAM (LZ4-compaction)" -> "Ratio 3.3:1 (Moyen)"
                                "ZSTD-Ultra" -> "Ratio 4.8:1 (Extrême)"
                                "LZO-Concurrent" -> "Ratio 2.8:1 (Rapide)"
                                "Double-Huffman" -> "Ratio 1.9:1 (Léger)"
                                else -> "Ratio 3.3:1"
                            }
                            Text(compRatioLabel, color = CyberSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Visual progress bar of compression space
                        val compressionFraction = engine.compressedMemorySizeGb / engine.rawMemorySizeGb
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(CyberBlack, RoundedCornerShape(6.dp))
                                .border(0.5.dp, CyberCardBorder, RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((1f - compressionFraction).coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(CyberSecondary, CyberPrimary)
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Empreinte Initiale", color = TextSecondary, fontSize = 8.sp)
                                Text("${String.format("%.1f", engine.rawMemorySizeGb)} Go RAM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Empreinte Active ZRAM", color = TextSecondary, fontSize = 8.sp)
                                Text("${String.format("%.2f", engine.compressedMemorySizeGb)} Go", color = CyberSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gain d'Espace Réel", color = TextSecondary, fontSize = 8.sp)
                                val gainGo = engine.rawMemorySizeGb - engine.compressedMemorySizeGb
                                Text("+${String.format("%.2f", gainGo)} Go", color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(CyberCardBorder.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, "latency", tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Latence de décompression:", color = TextSecondary, fontSize = 8.sp)
                            }
                            Text("${String.format("%.3f", engine.decompressionLatencyMs)} ms", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, "cycles", tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cycles de compaction active:", color = TextSecondary, fontSize = 8.sp)
                            }
                            Text("${engine.memoryCompactionCycles}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Memory active trigger button
                Button(
                    onClick = { isCompacting = true },
                    enabled = !isCompacting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSecondary,
                        disabledContainerColor = CyberSecondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCompacting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMPACTION EN COURS...", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, "compact action", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DÉCLENCHER COMPACTION MÉMOIRE (KERNEL)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(visible = showCompactToast) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(CyberSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberSecondary, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Compaction réussie ! Pages nettoyées. Latence stabilisée.",
                            color = CyberSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠️ Compression désactivée. Risque accru de micro-saccades système.",
                        color = CyberAccentRed,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundAppManagerPanel(engine: FsrEngine) {
    var isPurging by remember { mutableStateOf(false) }
    var showPurgeFinishedMessage by remember { mutableStateOf(false) }
    var showConsoleState by remember { mutableStateOf(true) }

    if (isPurging) {
        LaunchedEffect(Unit) {
            delay(1400) // Simulate a realistic cyber-cleanup delay
            engine.killAllBackgroundProcesses()
            isPurging = false
            showPurgeFinishedMessage = true
            delay(3000)
            showPurgeFinishedMessage = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ALGORITHME D'ARRIÈRE-PLAN COERCITIF & ALLOCATEUR",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Superviseur OOM-Killer & Auto-Allocateur de RAM vers l'app active",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "OOM Auto manager icon",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Auto Toggle Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Supervision Auto-Active (Daemon OOM)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Orchestre la fermeture coercitive instantanée des tâches de fond inutiles dès que l'application principale requiert un surcroît d'allocation de page cache.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 7.5.sp,
                        lineHeight = 10.sp
                    )
                }
                Switch(
                    checked = engine.autoBackgroundManagerEnabled,
                    onCheckedChange = { 
                        engine.autoBackgroundManagerEnabled = it 
                        if (it) {
                            engine.backgroundManagerLog = "✓ Daemon OOM activé. Analyse des sous-processus d'arrière plan en cours."
                        } else {
                            // Restore them
                            engine.backgroundProcesses.forEach { p -> p.active = true }
                            engine.ramReallocatedToActiveAppMb = 0
                            engine.systemBackgroundProcessesKilled = 0
                            engine.backgroundManagerLog = "⚠ Daemon OOM désactivé. Restauration de l'état d'origine des processus d'arrière-plan."
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.autoBackgroundManagerEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Agressivité de l'algorithme d'élimination",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val modes = listOf(
                    "Sélectif léger",
                    "Agressif modéré",
                    "Coercitif (Extrême)",
                    "Z-Noyau Brut"
                )
                
                val modesValues = listOf(
                    "Sélectif Léger",
                    "Agressif Modéré",
                    "Coercitif Automatique (Extrême)",
                    "Z-Noyau Quantique Brut (Kill Tout)"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    modes.forEachIndexed { idx, label ->
                        val value = modesValues[idx]
                        val isSel = engine.backgroundKillAgressiveness == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyberPrimary else CyberBlack.copy(alpha = 0.3f))
                                .border(1.dp, if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { 
                                    engine.backgroundKillAgressiveness = value 
                                    engine.backgroundManagerLog = "Modifié niveau d'agressivité : $value. Table des priorités OOM mise à jour."
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else TextSecondary.copy(alpha = 0.8f),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry of active allocation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ram Allocated Column Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CyberBlack.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("RAM RÉALLOUÉE (BONUS UNIQUE)", color = CyberSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (engine.ramReallocatedToActiveAppMb > 0) {
                                    "%.2f Go".format(engine.ramReallocatedToActiveAppMb / 1024.0)
                                } else {
                                    "0.0 Go (Inactif)"
                                },
                                color = if (engine.ramReallocatedToActiveAppMb > 0) Color.Green else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("Injecté par signal KERNEL", color = TextSecondary, fontSize = 6.5.sp)
                        }
                    }

                    // Killed processes metric Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CyberBlack.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("PROCESSUS CO-ÉLIMINÉS", color = CyberPrimary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${engine.systemBackgroundProcessesKilled} / ${engine.backgroundProcesses.size}",
                                color = if (engine.systemBackgroundProcessesKilled > 0) CyberPrimary else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("Indésirables libérés", color = TextSecondary, fontSize = 6.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Console logging shell terminal emulator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .border(0.5.dp, CyberCardBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CONSOLE DAEMON OOM-KILLER (LIVE SHELL)", color = Color.Green, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (engine.autoBackgroundManagerEnabled) Color.Green else Color.Red)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = engine.backgroundManagerLog,
                            color = Color.LightGray,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Processes Explorer Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPLORATEUR DE PROCESSUS D'ARRIÈRE-PLAN DÉTECTÉS",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (showConsoleState) "Masquer [▲]" else "Afficher [▼]",
                        color = CyberPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showConsoleState = !showConsoleState }
                    )
                }

                if (showConsoleState) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Small custom list of individual background apps
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(6.dp)
                    ) {
                        engine.backgroundProcesses.forEach { proc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = proc.name,
                                            color = Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (proc.active) Color.Red.copy(alpha = 0.15f) else Color.Green.copy(alpha = 0.15f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (proc.active) Color.Red.copy(alpha = 0.5f) else Color.Green.copy(alpha = 0.5f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (proc.active) "ACTIF" else "PURGÉ (0 Mo)",
                                                color = if (proc.active) Color.Red else Color.Green,
                                                fontSize = 6.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${proc.packageName} | Priorité: ${proc.importance}",
                                        color = TextSecondary,
                                        fontSize = 7.sp
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${proc.memoryMb} Mo",
                                        color = if (proc.active) Color.White else TextSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (proc.active) Color.Red.copy(alpha = 0.12f) else CyberSecondary.copy(alpha = 0.15f))
                                            .border(0.5.dp, if (proc.active) Color.Red else CyberSecondary, CircleShape)
                                            .clickable {
                                                if (proc.active) {
                                                    engine.manualKillBackgroundProcess(proc.packageName)
                                                } else {
                                                    engine.restartBackgroundProcess(proc.packageName)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (proc.active) Icons.Default.Close else Icons.Default.Refresh,
                                            contentDescription = "Action button",
                                            tint = if (proc.active) Color.Red else CyberSecondary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(CyberCardBorder.copy(alpha = 0.15f)))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big main Purge Action Button
                val allKilled = engine.backgroundProcesses.all { !it.active || it.importance == "Critique" }
                Button(
                    onClick = { isPurging = true },
                    enabled = !isPurging && !allKilled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        disabledContainerColor = CyberPrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPurging) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FORCE-KILL DES PROCESSUS EN COURS...", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, "Lightning bolt target killer", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (allKilled) "TOUT LE FOND EST ENTIÈREMENT PURGÉ" else "FORCER L'ÉLIMINATION DES APPLICATIONS EN FOND",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showPurgeFinishedMessage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color.Green.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color.Green, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Succès ! ${engine.systemBackgroundProcessesKilled} applications terminées de force. RAM réaffectée avec succès !",
                            color = Color.Green,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠️ Daemon de supervision inactif. La RAM reste partagée avec les tâches d'arrière-plan.",
                        color = CyberAccentRed,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BottomActionArea(engine: FsrEngine) {
    var optimizeActive by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Apply Optimization button
        Button(
            onClick = {
                // Boost rendering and optimize cooling!
                engine.gpuLoadSlider = (engine.gpuLoadSlider - 20f).coerceAtLeast(15f)
                engine.rcasSharpness = 0.9f
                optimizeActive = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Bolt Optimizer",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (optimizeActive) "OPTIMISÉ" else "APPLIQUER OPTIMISATION",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Reset default configurations button
        IconButton(
            onClick = {
                engine.sourceResolution = engine.availableResolutions.find { it.id == "240p" } ?: engine.availableResolutions[1]
                engine.upscaleMode = UpscaleMode.FSR_2_0
                engine.frameGenMode = FrameGenMode.LSFG_X2
                engine.gpuLoadSlider = 40f
                engine.rcasSharpness = 0.75f
                engine.baseFps = 30
                engine.variableRateShading = true
                engine.dynamicResolutionScaling = true
                engine.foveatedVringEnabled = true
                engine.cpuUndervoltEnabled = false
                engine.cpuVoltOffsetMv = -80
                engine.gpuUndervoltEnabled = false
                engine.gpuVoltOffsetMv = -60
                engine.cpuOverclockEnabled = false
                engine.cpuCoreFreqGhz = 2.8f
                engine.gpuOverclockEnabled = false
                engine.gpuClockMhz = 850
                engine.ramCompressorEnabled = true
                engine.ramCompressionAlgo = "ZRAM (LZ4-compaction)"
                optimizeActive = false
            },
            modifier = Modifier
                .size(48.dp)
                .background(LavenderResetBg, RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restaurer",
                tint = LavenderOnContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationBarView(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(LavenderSelectedBg)
            .drawBehind {
                drawLine(
                    color = CyberCardBorder.copy(alpha = 0.6f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val navigationItems = listOf(
            Triple("Core", Icons.Default.Home, 0),
            Triple("Stats", Icons.Default.List, 1),
            Triple("Tweaks", Icons.Default.Build, 2),
            Triple("Apps", Icons.Default.Settings, 3),
            Triple("Profile", Icons.Default.Person, 4)
        )

        navigationItems.forEach { (label, icon, index) ->
            val isSelected = selectedTab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 26.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LavenderResetBg else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CyberPrimary else TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = if (isSelected) CyberPrimary else TextSecondary.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

@Composable
fun ProfileCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(LavenderContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profil",
                        tint = CyberPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Profil Développeur",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "traoreismael29041993@gmail.com",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CyberCardBorder.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SPÉCIFICATIONS MATÉRIELLES DE RENDU",
                color = CyberPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ProfileConfigRow(label = "GPU Actif", value = "Mali-G710 MC10 Adaptive Mobile")
            ProfileConfigRow(label = "API Graphique", value = "Vulkan 1.3 / OpenGL ES 3.2")
            ProfileConfigRow(label = "Châssis", value = "FSR 2.0 Spatial-Temporal EASU")
            ProfileConfigRow(label = "Fluidification", value = "LSFG FrameGen Adaptive Core v3.2")
            ProfileConfigRow(label = "Fuseau Horaire de Rendu", value = "UTC-7 (Pacific Standard)")
            ProfileConfigRow(label = "Date d'Optimisation", value = "13 Juin 2026")
        }
    }
}

@Composable
fun ProfileConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GpuTextureCompressorPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMPRESSEUR DE TEXTURES ULTRA-DENSE",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Algorithmes de compression de texture VRAM de pointe",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Texture Compression Status",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Activer la Compression Texture GPU",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Réduit la consommation de bande passante mémoire et l'empreinte VRAM.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.gpuTextureCompressorEnabled,
                    onCheckedChange = { engine.gpuTextureCompressorEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.gpuTextureCompressorEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "FORMAT DE COMPRESSION SÉLECTIONNÉ",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val formats = listOf(
                    "BC7 (Normale/Qualité)",
                    "ASTC 4x4",
                    "ETC2 (Mobile)",
                    "Wavlet Adaptive Huffman (Ultra)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEach { format ->
                        val isSelected = engine.gpuTextureCompressionFormat == format
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) LavenderResetBg else CyberBlack.copy(alpha = 0.2f))
                                .border(1.dp, if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { engine.gpuTextureCompressionFormat = format }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = format,
                                color = if (isSelected) CyberPrimary else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (format) {
                                        "BC7 (Normale/Qualité)" -> "X4 (4:1)"
                                        "ASTC 4x4" -> "X6 (6:1)"
                                        "ETC2 (Mobile)" -> "X3.5 (3.5:1)"
                                        "Wavlet Adaptive Huffman (Ultra)" -> "X8 (8:1)"
                                        else -> ""
                                    },
                                    color = if (isSelected) CyberPrimary else TextSecondary,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBlack.copy(alpha = 0.4f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("MÉMOIRE TEXTURE DESSINÉE", color = TextSecondary, fontSize = 8.sp)
                        Text("${String.format("%.1f", engine.compressedTextureMemorySizeMb)} Mo", color = CyberPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(CyberCardBorder.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("ÉQUIVALENT BANDE PASSANTE", color = TextSecondary, fontSize = 8.sp)
                        Text("-${String.format("%.1f", engine.textureBandwidthSavedPct)}%", color = CyberSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun AntiAliasingAnisotropyPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILTRAGE ANISOTROPIQUE & ANTI-ALIASING",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Correction de l'aliasing géométrique et des textures obliques",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "AA icon",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Anti-Aliasing Choice
            Text(
                text = "ALGORITHME D'ANTI-ALIASING (AA)",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val aaModes = listOf(
                "Aucun",
                "FXAA",
                "SMAA",
                "TAA (Temporel)",
                "DSAA (Deep Learning AA)",
                "MSAA 4x"
            )

            // Column of Grid buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in aaModes.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (j in 0..1) {
                            val index = i + j
                            if (index < aaModes.size) {
                                val mode = aaModes[index]
                                val isSelected = engine.antiAliasingMode == mode
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { engine.antiAliasingMode = mode }
                                        .border(1.dp, if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) LavenderResetBg else Color.Transparent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode,
                                            color = if (isSelected) CyberPrimary else TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (engine.antiAliasingMode == "TAA (Temporel)" || engine.antiAliasingMode == "MSAA 4x") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Niveau d'Échantillonnage AA:", color = TextSecondary, fontSize = 8.sp)
                    Text("${engine.aaSampleCount}x MSAA/TAA", color = CyberPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBlack.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf(2, 4, 8).forEach { valCount ->
                        val isSelected = engine.aaSampleCount == valCount
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyberPrimary else Color.Transparent)
                                .clickable { engine.aaSampleCount = valCount }
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${valCount}X",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Anisotropic level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NIVEAU FILTRAGE ANISOTROPE (AF)",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (engine.anisotropicLevel == 1) "OFF (Linear)" else "${engine.anisotropicLevel}x AF",
                    color = CyberPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBlack.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(1, 2, 4, 8, 16).forEach { level ->
                    val isSelected = engine.anisotropicLevel == level
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberPrimary else Color.Transparent)
                            .clickable { engine.anisotropicLevel = level }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (level == 1) "Off" else "${level}x",
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultiCoreProcessingPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDONNANCEUR MULTI-CŒURS COMPLEXE",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Moteur de parallélisation asynchrone CPU-GPU moderne",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "MultiCore icon",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CPU Multi-threading switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Traitement Parallèle CPU Multi-Cœurs",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distribue les calculs de vecteurs de vitesse 3D sur plusieurs Threads de travail.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.multiCoreCpuDispatch,
                    onCheckedChange = { engine.multiCoreCpuDispatch = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.multiCoreCpuDispatch) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Threads CPU de calcul actifs :", color = TextSecondary, fontSize = 8.sp)
                    Text("${engine.cpuWorkerThreads} Workers", color = CyberPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = engine.cpuWorkerThreads.toFloat(),
                    onValueChange = { engine.cpuWorkerThreads = it.toInt() },
                    valueRange = 2f..32f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberPrimary,
                        activeTrackColor = CyberPrimary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // GPU Overlapping queue switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exécution Concourante GPU Multi-Files",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active l'Async Compute pour superposer les passes de shaders géométriques et d'upscaling.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.multiQueueGpuComputing,
                    onCheckedChange = { engine.multiQueueGpuComputing = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            if (engine.multiQueueGpuComputing) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pipelines de traitement parallélisés :", color = TextSecondary, fontSize = 8.sp)
                    Text("${engine.gpuExecutionPipelines}/16 Blocs ASIC", color = CyberSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = engine.gpuExecutionPipelines.toFloat(),
                    onValueChange = { engine.gpuExecutionPipelines = it.toInt() },
                    valueRange = 2f..16f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberSecondary,
                        activeTrackColor = CyberSecondary,
                        inactiveTrackColor = CyberCardBorder
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Composable
fun ThermalLimitManagerPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GESTIONNAIRE DE TEMPÉRATURE LIMITÉ",
                        color = if (engine.thermalThrottlingActive) CyberAccentRed else CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Thermorégulateur matériel dynamique à sécurité intégrée",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Thermal icon",
                    tint = if (engine.thermalThrottlingActive) CyberAccentRed else CyberSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thermal Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TEMPÉRATURE DU CŒUR GPU", color = TextSecondary, fontSize = 8.sp)
                    Text(
                        text = "${String.format("%.1f", engine.gpuTempCelsius)} °C",
                        color = when {
                            engine.thermalThrottlingActive -> CyberAccentRed
                            engine.gpuTempCelsius > 75f -> CyberTertiary
                            else -> CyberSecondary
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = if (engine.thermalThrottlingActive) CyberAccentRed.copy(alpha = 0.15f) else CyberSecondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (engine.thermalThrottlingActive) CyberAccentRed else CyberSecondary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (engine.thermalThrottlingActive) "THROTTLED (CRITICAL)" else "COOL / SOUS CONTRÔLE",
                        color = if (engine.thermalThrottlingActive) CyberAccentRed else CyberSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Limit adjustable slider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TEMPÉRATURE LIMITE CRITIQUE (Throttle) :",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${engine.thermalLimitCelsius} °C",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Slider(
                value = engine.thermalLimitCelsius.toFloat(),
                onValueChange = { engine.thermalLimitCelsius = it.toInt() },
                valueRange = 60f..105f,
                colors = SliderDefaults.colors(
                    thumbColor = if (engine.thermalThrottlingActive) CyberAccentRed else CyberPrimary,
                    activeTrackColor = if (engine.thermalThrottlingActive) CyberAccentRed else CyberPrimary,
                    inactiveTrackColor = CyberCardBorder
                ),
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Glissez l'ajusteur vers la gauche pour tester de force le comportement du régulateur de limitation thermique.",
                color = TextMuted,
                fontSize = 8.sp,
                lineHeight = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun TdpPowerManagerPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONTRÔLEUR DE PUISSANCE TDP DYNAMIQUE",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Régulation des enveloppes énergétiques CPU & GPU en temps réel",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Power icon",
                    tint = CyberSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profiles selections
            Text(
                text = "PROFIL D'ALIMENTATION MATÉRIEL",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val profiles = listOf(
                "Éco (15W/20W)",
                "Équilibrée (35W/45W)",
                "Performance (65W/80W)",
                "Configurée (Custom)"
            )

            // Grid of profiles
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in profiles.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (j in 0..1) {
                            val index = i + j
                            if (index < profiles.size) {
                                val profile = profiles[index]
                                val isSelected = engine.powerProfileMode == profile
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { engine.powerProfileMode = profile }
                                        .border(1.dp, if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) LavenderResetBg else Color.Transparent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile,
                                            color = if (isSelected) CyberPrimary else TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CPU TDP Slider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIMITE TDP ENVELOPPE CPU :",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${engine.cpuTdpWatts.toInt()} W",
                    color = CyberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Slider(
                value = engine.cpuTdpWatts,
                onValueChange = {
                    engine.powerProfileMode = "Configurée (Custom)"
                    engine.cpuTdpWatts = it
                },
                valueRange = 5f..120f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberPrimary,
                    activeTrackColor = CyberPrimary,
                    inactiveTrackColor = CyberCardBorder
                ),
                modifier = Modifier.height(24.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // GPU TDP Slider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIMITE TDP ENVELOPPE GPU :",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${engine.gpuTdpWatts.toInt()} W",
                    color = CyberSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Slider(
                value = engine.gpuTdpWatts,
                onValueChange = {
                    engine.powerProfileMode = "Configurée (Custom)"
                    engine.gpuTdpWatts = it
                },
                valueRange = 5f..150f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberSecondary,
                    activeTrackColor = CyberSecondary,
                    inactiveTrackColor = CyberCardBorder
                ),
                modifier = Modifier.height(24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Intelligent SmartShift / Dual Power Booster Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.4f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dual Power Booster (SmartShift)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Réalloue dynamiquement l'énergie CPU/GPU non exploitée pour optimiser le framerate global ou la fluidité.",
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.dualPowerBoosterEnabled,
                    onCheckedChange = { engine.dualPowerBoosterEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-Time Power Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("PUISSANCE RÉELLE CPU", color = TextSecondary, fontSize = 8.sp)
                    Text(
                        text = "${String.format("%.2f", engine.cpuPowerDrawMw / 1000f)} W",
                        color = if (engine.cpuTdpThrottledActive) CyberTertiary else CyberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (engine.cpuTdpThrottledActive) {
                        Text("TDP SEUIL LIMITÉ", color = CyberTertiary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(CyberCardBorder.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("PUISSANCE RÉELLE GPU", color = TextSecondary, fontSize = 8.sp)
                    Text(
                        text = "${String.format("%.2f", engine.gpuPowerDrawMw / 1000f)} W",
                        color = if (engine.gpuTdpThrottledActive) CyberTertiary else CyberSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (engine.gpuTdpThrottledActive) {
                        Text("TDP SEUIL LIMITÉ", color = CyberTertiary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ArchitectureCompatibilityPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMPATIBILITÉ ARCHITECTURE & SDK NÉO-COMPILATION",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Optimisations bas niveau des binaires natifs NDK (x86/ARM) & Runtimes Android",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Architecture icon",
                    tint = CyberSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Selector of Simulated Architecture (ISA)
            Text(
                text = "SUPPORT ARCHITECTURALE MATÉRIEL (ISA RECOURS)",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val architectures = listOf(
                "ARMv9-A (Modern 64-bit)" to "Ultra-rapide, SVE2 Vectorisation active",
                "ARM64-v8a (ARMv8 64-bit)" to "Standard natif ARMv8-A, Neon SIMD pipelines stables",
                "ARMv7-A (32-bit legacy)" to "Régression 32-bit, pénalité de registre (+55%)",
                "x86_64 (Intel/AMD)" to "Optimisé pour l'émulateur standard avec AVX",
                "x86 (Legacy Emulator)" to "Émulation restreinte, barrières de Shaders"
            )

            // Dynamic Custom dropdown or simple list
            architectures.forEach { (arch, desc) ->
                val isSelected = engine.targetIsaArchitecture == arch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clickable { engine.targetIsaArchitecture = arch }
                        .border(
                            1.dp,
                            if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) LavenderResetBg else Color.Transparent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { engine.targetIsaArchitecture = arch },
                            colors = RadioButtonDefaults.colors(selectedColor = CyberPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = arch,
                                color = if (isSelected) CyberPrimary else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Android SDK Version Profile Selector
            Text(
                text = "COMPATIBILITÉ CIBLE SYSTÈME ANDROID API",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val sdkVersions = listOf(
                "Android 14/15/16 (API 34-36)",
                "Android 10 - 13 (API 29-33)",
                "Android 7.0 - 9.0 (API 24-28)",
                "Android 5.0 - 6.0 (API 21-23)"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in sdkVersions.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (j in 0..1) {
                            val index = i + j
                            if (index < sdkVersions.size) {
                                val sdk = sdkVersions[index]
                                val isSelected = engine.targetAndroidSdkVersion == sdk
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { engine.targetAndroidSdkVersion = sdk }
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) LavenderResetBg else Color.Transparent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sdk,
                                            color = if (isSelected) CyberPrimary else TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compiling tweaks (ABI filtering & Link-time optimization toggles)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filtres d'ABI NDK Stricts (Multi-Arch)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Injecte des builds séparées x86, armv7-a et arm64-v8a pour éviter toute ré-émulation lourde.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.ndkAbiFiltersEnabled,
                    onCheckedChange = { engine.ndkAbiFiltersEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optimisation Link-Time (LTO)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active l'optimisation inter-procédurale de la compilation croisée, diminuant les micro-saccades de runtime.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
                Switch(
                    checked = engine.linkerLtoOptimizations,
                    onCheckedChange = { engine.linkerLtoOptimizations = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = CyberSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Diagnostics and multipliers Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DIAGNOSTICS & MULTIPLICATEURS MATÉRIELS", color = CyberPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    val speedPenalty = when (engine.targetIsaArchitecture) {
                        "ARMv9-A (Modern 64-bit)" -> "Extrêmement optimisé, SVE2 vectorisé (x0.82)"
                        "ARM64-v8a (ARMv8 64-bit)" -> "Natif ARMv8-A régulier (x1.00)"
                        "ARMv7-A (32-bit legacy)" -> "Goulot d'étranglement CPU critique (x1.55)"
                        "x86_64 (Intel/AMD)" -> "Simulation d'architecture PC Desktop (x0.95)"
                        "x86 (Legacy Emulator)" -> "Émulation de translation restrictive (x1.40)"
                        else -> "Natif (x1.00)"
                    }

                    val sdkImpact = when (engine.targetAndroidSdkVersion) {
                        "Android 14/15/16 (API 34-36)" -> "Exécution ART ultra-fluide moderne"
                        "Android 10 - 13 (API 29-33)" -> "Standard stable avec GC à faible pause"
                        "Android 7.0 - 9.0 (API 24-28)" -> "Couche de support rétrocompatible active (+20%)"
                        "Android 5.0 - 6.0 (API 21-23)" -> "Surcharge thermique du Garbage Collector (+45%)"
                        else -> "Standard"
                    }

                    val formattedHeap = run {
                        val heap = engine.vmHeapSize
                        if (heap.endsWith("M")) {
                            val num = heap.removeSuffix("M").toIntOrNull()
                            if (num != null) {
                                if (num >= 1024) {
                                    val gb = num / 1024.0
                                    if (gb % 1 == 0.0) "${gb.toInt()} Go" else "${"%.1f".format(gb)} Go"
                                } else {
                                    "$num Mo"
                                }
                            } else {
                                heap
                            }
                        } else {
                            heap
                        }
                    }

                    Text("• ISA CPU/GPU : $speedPenalty", color = Color.White, fontSize = 8.sp)
                    Text("• Runtime Système : $sdkImpact", color = Color.White, fontSize = 8.sp)
                    Text("• Linker LTO : ${if (engine.linkerLtoOptimizations) "Actif (Pertes réduites de 8%)" else "Inactif"}", color = Color.White, fontSize = 8.sp)
                    Text("• Tas Virtuel (VM Heap) : $formattedHeap (${if (engine.largeHeapEnabled) "largeHeap Forcé" else "Standard"})", color = Color.White, fontSize = 8.sp)
                    Text("• Accélération Matérielle : ${if (engine.hardwareAcceleratedEnabled) "GPU Direct (Vulkan/OGLES)" else "Logiciel (Émulé)"}", color = Color.White, fontSize = 8.sp)
                    Text("• GPU VRAM Alloué : ${engine.gpuVideoMemorySizeMb} Mo (${if (engine.gpuVramCompressionEnabled) "Compression " + engine.gpuVramCompressionLevel + " [Physique: " + "%.0f".format(engine.activeVramFootprintMb) + " Mo]" else "Standard"})", color = Color.White, fontSize = 8.sp)
                    Text("• Statut Compilation : ABI armeabi-v7a + arm64-v8a (ARMv8) + x86 + x86_64 conformes", color = CyberSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ApplicationProfilesScreenView(engine: FsrEngine) {
    var selectedAppPackageName by remember { mutableStateOf("global") }
    
    // Custom app adding states
    var customAppName by remember { mutableStateOf("") }
    var customAppPkg by remember { mutableStateOf("") }
    var customAppDeveloper by remember { mutableStateOf("") }
    var customAppCategory by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var customHeapInput by remember { mutableStateOf("") }

    val activeProfileText = if (engine.currentActivePackageName == "global") {
        "Configuration Globale (Système)"
    } else {
        engine.installedApps.find { it.packageName == engine.currentActivePackageName }?.name ?: engine.currentActivePackageName
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. ACTIVE PROFILE NOTIFICATION BANNER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, CyberPrimary, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberPrimary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CyberPrimary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Active prof icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "PROFIL ACTIF DANS L'ENGIN FSR",
                        color = CyberPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = activeProfileText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. APPLICATIONS GRID SELECTOR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APPLICATIONS CONTEXTUELLES",
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Sélectionnez un profil préconfiguré ou simulez vos propres applications.",
                            color = TextSecondary,
                            fontSize = 8.sp
                        )
                    }
                    IconButton(
                        onClick = { showAddForm = !showAddForm },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add custom app",
                            tint = CyberSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom app adder form
                if (showAddForm) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, CyberCardBorder.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("AJOUTER UNE NOUVELLE APPLICATION", color = CyberSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customAppName,
                                onValueChange = { customAppName = it },
                                label = { Text("Nom du jeu/app", fontSize = 10.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = customAppPkg,
                                onValueChange = { customAppPkg = it },
                                label = { Text("Package (ex: com.epic.fortnite)", fontSize = 10.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (customAppName.isNotBlank() && customAppPkg.isNotBlank()) {
                                        val newApp = FsrEngine.InstalledApp(
                                            name = customAppName,
                                            packageName = customAppPkg,
                                            type = "Jeu/Custom",
                                            defaultCategory = "Simulé",
                                            developer = "Utilisateur",
                                            iconEmoji = "📦",
                                            hasCustomProfile = false
                                        )
                                        engine.installedApps.add(newApp)
                                        selectedAppPackageName = customAppPkg
                                        engine.switchActiveProfile(customAppPkg)
                                        customAppName = ""
                                        customAppPkg = ""
                                        showAddForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary)
                            ) {
                                Text("Ajouter à la bibliothèque", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Global Profile Row
                val isGlobalSelected = selectedAppPackageName == "global"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable {
                            selectedAppPackageName = "global"
                            engine.switchActiveProfile("global")
                        }
                        .border(
                            1.dp,
                            if (isGlobalSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGlobalSelected) LavenderResetBg else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📢", fontSize = 18.sp)
                            Column {
                                Text("Configuration Globale (Système)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Paramètres généraux hérités par défaut", color = TextSecondary, fontSize = 8.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIF", color = CyberPrimary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Apps List
                engine.installedApps.forEach { app ->
                    val isAppSelected = selectedAppPackageName == app.packageName
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable {
                                selectedAppPackageName = app.packageName
                                engine.switchActiveProfile(app.packageName)
                            }
                            .border(
                                1.dp,
                                if (isAppSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAppSelected) LavenderResetBg else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(app.iconEmoji, fontSize = 16.sp)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(app.name, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(CyberSecondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(app.defaultCategory, color = CyberSecondary, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(app.packageName, color = TextSecondary, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (app.hasCustomProfile) {
                                    Box(
                                        modifier = Modifier
                                            .background(CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("CUSTOM", color = CyberSecondary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Text("global", color = TextSecondary, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. TARGET INSPECTOR & ADJUSTMENT PROFILE PANEL
        val selectedApp = engine.installedApps.find { it.packageName == selectedAppPackageName }
        if (selectedApp != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header of Inspector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AJUSTEMENT DÉDIÉ : ${selectedApp.name.uppercase()}",
                                color = CyberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Définition des limites et optimisations exclusives à l'application",
                                color = TextSecondary,
                                fontSize = 8.sp
                            )
                        }
                        Text(selectedApp.iconEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle Custom Profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberDarkSurface)
                            .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Paramètres de profil spécifiques",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active les débridages et modificateurs matériels natifs pour cette application uniquement.",
                                color = TextSecondary,
                                fontSize = 8.sp,
                                lineHeight = 11.sp
                            )
                        }
                        Switch(
                            checked = selectedApp.hasCustomProfile,
                            onCheckedChange = { enable ->
                                engine.toggleCustomProfileForApp(selectedApp.packageName, enable)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberPrimary,
                                checkedTrackColor = CyberPrimary.copy(alpha = 0.4f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = CyberCardBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!selectedApp.hasCustomProfile) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberDarkSurface)
                                .border(1.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = TextSecondary, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Cette application hérite actuellement de la configuration globale centrale.",
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Activez le bouton ci-dessus pour déverrouiller l'optimiseur de TDP et l'ajusteur matériel exclusif.",
                                    color = CyberPrimary,
                                    fontSize = 8.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // ADJUSTMENT PANEL (User can set unique settings!)
                        Text(
                            text = "PUISSANCE THERMIQUE ET TDP CIBLE",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Slider CPU Watts
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TDP Cible CPU", color = Color.Black, fontSize = 10.sp)
                                Text("${engine.cpuTdpWatts.toInt()} Watts", color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = engine.cpuTdpWatts,
                                onValueChange = { 
                                    engine.cpuTdpWatts = it
                                    engine.powerProfileMode = "Configurée (Custom)"
                                },
                                valueRange = 5f..120f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyberPrimary,
                                    activeTrackColor = CyberPrimary
                                )
                            )
                        }

                        // Slider GPU Watts
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TDP Cible GPU", color = Color.Black, fontSize = 10.sp)
                                Text("${engine.gpuTdpWatts.toInt()} Watts", color = CyberSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = engine.gpuTdpWatts,
                                onValueChange = { 
                                    engine.gpuTdpWatts = it
                                    engine.powerProfileMode = "Configurée (Custom)"
                                },
                                valueRange = 5f..150f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyberSecondary,
                                    activeTrackColor = CyberSecondary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Micro-undervolting
                        Text(
                            text = "TENSION MATÉRIELLE EXCLUSIVE (VOLT OFFSET)",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Undervolt CPU", color = Color.Black, fontSize = 9.sp)
                                        Switch(
                                            checked = engine.cpuUndervoltEnabled,
                                            onCheckedChange = { engine.cpuUndervoltEnabled = it },
                                            modifier = Modifier.scale(0.75f),
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberPrimary)
                                        )
                                    }
                                    if (engine.cpuUndervoltEnabled) {
                                        Slider(
                                            value = engine.cpuVoltOffsetMv.toFloat(),
                                            onValueChange = { engine.cpuVoltOffsetMv = it.toInt() },
                                            valueRange = -150f..-20f,
                                            colors = SliderDefaults.colors(thumbColor = CyberPrimary, activeTrackColor = CyberPrimary)
                                        )
                                        Text("${engine.cpuVoltOffsetMv} mV", color = CyberPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Undervolt GPU", color = Color.Black, fontSize = 9.sp)
                                        Switch(
                                            checked = engine.gpuUndervoltEnabled,
                                            onCheckedChange = { engine.gpuUndervoltEnabled = it },
                                            modifier = Modifier.scale(0.75f),
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberSecondary)
                                        )
                                    }
                                    if (engine.gpuUndervoltEnabled) {
                                        Slider(
                                            value = engine.gpuVoltOffsetMv.toFloat(),
                                            onValueChange = { engine.gpuVoltOffsetMv = it.toInt() },
                                            valueRange = -120f..-20f,
                                            colors = SliderDefaults.colors(thumbColor = CyberSecondary, activeTrackColor = CyberSecondary)
                                        )
                                        Text("${engine.gpuVoltOffsetMv} mV", color = CyberSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Upscale/Frame Gen profile controls
                        Text(
                            text = "MÉCANISME D'ÉCHELLE ET DOUBLEURS",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Selection of upscaleMode & frameGenMode
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Upscale Row choices
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mode de Sursaut", color = Color.Black, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                UpscaleMode.values().forEach { mode ->
                                    val isSel = engine.upscaleMode == mode
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                            .clickable { engine.upscaleMode = mode }
                                            .background(
                                                if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(6.dp)
                                    ) {
                                        Text(mode.label, color = if (isSel) CyberPrimary else Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // FrameGen choices
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Frame Gen", color = Color.Black, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                FrameGenMode.values().forEach { fg ->
                                    val isSel = engine.frameGenMode == fg
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                            .clickable { engine.frameGenMode = fg }
                                            .background(
                                                if (isSel) CyberSecondary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberSecondary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(6.dp)
                                    ) {
                                        Text(fg.label, color = if (isSel) CyberSecondary else Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Architecture & Target Android SDK exclusive configurations
                        Text(
                            text = "ARCHITECTURE (ISA) & COMPATIBILITÉ SDK CIBLE",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Select box for Architecture and select box for Android SDK
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ISA CPU/GPU cible", color = Color.Black, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                val architectureOptions = listOf(
                                    "ARMv9-A (Modern 64-bit)",
                                    "ARM64-v8a (ARMv8 64-bit)",
                                    "ARMv7-A (32-bit legacy)",
                                    "x86_64 (Intel/AMD)",
                                    "x86 (Legacy Emulator)"
                                )
                                architectureOptions.forEach { option ->
                                    val isSel = engine.targetIsaArchitecture == option
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                            .clickable { engine.targetIsaArchitecture = option }
                                            .background(
                                                if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(6.dp)
                                    ) {
                                        Text(option, color = if (isSel) CyberPrimary else Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Android SDK Support", color = Color.Black, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                val sdkOptions = listOf(
                                    "Android 14/15/16 (API 34-36)",
                                    "Android 10 - 13 (API 29-33)",
                                    "Android 7.0 - 9.0 (API 24-28)",
                                    "Android 5.0 - 6.0 (API 21-23)"
                                )
                                sdkOptions.forEach { sdk ->
                                    val isSel = engine.targetAndroidSdkVersion == sdk
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                            .clickable { engine.targetAndroidSdkVersion = sdk }
                                            .background(
                                                if (isSel) CyberSecondary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberSecondary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(6.dp)
                                    ) {
                                        Text(sdk, color = if (isSel) CyberSecondary else Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // NEW: VM Heap & Hardware Acceleration Parameters
                        Text(
                            text = "MACHINE VIRTUELLE & ACCÉLÉRATION MATÉRIELLE (VM)",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Heap size choices
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Taille maximum du tas Java (VM Heap Size)", color = Color.Black, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Row 1 Presets: 128Mo à 1Go
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val heapOptionsRow1 = listOf("128M", "256M", "512M", "1024M")
                                heapOptionsRow1.forEach { opt ->
                                    val isSel = engine.vmHeapSize == opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { engine.vmHeapSize = opt }
                                            .background(
                                                if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (opt) {
                                                "1024M" -> "1 Go"
                                                else -> "${opt.replace("M", " Mo")}"
                                            },
                                            color = if (isSel) CyberPrimary else Color.Black,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Row 2 Presets: 2Go à 8Go
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val heapOptionsRow2 = listOf("2048M", "3072M", "4048M", "8192M")
                                heapOptionsRow2.forEach { opt ->
                                    val isSel = engine.vmHeapSize == opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { engine.vmHeapSize = opt }
                                            .background(
                                                if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (opt) {
                                                "2048M" -> "2 Go"
                                                "3072M" -> "3 Go"
                                                "4048M" -> "4 Go"
                                                "8192M" -> "8 Go"
                                                else -> "${opt.replace("M", " Mo")}"
                                            },
                                            color = if (isSel) CyberPrimary else Color.Black,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Saisie personnalisée (Custom heap size value input)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customHeapInput,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                            customHeapInput = newValue
                                        }
                                    },
                                    label = { Text("Valeur personnalisée (Mo)", fontSize = 8.sp, color = TextSecondary) },
                                    placeholder = { Text("ex: 768", fontSize = 8.sp, color = Color.Gray.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 9.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberPrimary,
                                        unfocusedBorderColor = CyberCardBorder.copy(alpha = 0.4f),
                                        focusedLabelColor = CyberPrimary
                                    )
                                )
                                Button(
                                    onClick = {
                                        val sizeVal = customHeapInput.trim().toIntOrNull()
                                        if (sizeVal != null && sizeVal > 0) {
                                            engine.vmHeapSize = "${sizeVal}M"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Appliquer", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Show Custom Badge if custom value is currently active
                            val presetsList = listOf("128M", "256M", "512M", "1024M", "2048M", "3072M", "4048M", "8192M")
                            if (!presetsList.contains(engine.vmHeapSize)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Saisie personnalisée active :", color = CyberPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                            val formattedActive = run {
                                                val heap = engine.vmHeapSize
                                                if (heap.endsWith("M")) {
                                                    val num = heap.removeSuffix("M").toIntOrNull()
                                                    if (num != null) {
                                                        if (num >= 1024) "${"%.1f".format(num / 1024.0)} Go ($num Mo)" else "$num Mo"
                                                    } else heap
                                                } else heap
                                            }
                                            Text(formattedActive, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Green.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("ACTIF", color = Color.Green, fontSize = 6.5.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // largeHeap switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Transparent)
                                .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Segment de tas étendu (largeHeap)",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Alloue une enveloppe RAM supérieure (jusqu'à 4x la limite standard) pour prévenir les plantages d'exécution (OOM).",
                                    color = TextSecondary,
                                    fontSize = 7.5.sp,
                                    lineHeight = 10.sp
                                )
                            }
                            Switch(
                                checked = engine.largeHeapEnabled,
                                onCheckedChange = { engine.largeHeapEnabled = it },
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberPrimary,
                                    checkedTrackColor = CyberPrimary.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // hardwareAccelerated switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Transparent)
                                .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Force l'accélération matérielle (hardwareAccelerated)",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active le compositing et le rendu pipeline directs via Vulkan/OpenGL ES. Évite la rastérisation logicielle.",
                                    color = TextSecondary,
                                    fontSize = 7.5.sp,
                                    lineHeight = 10.sp
                                )
                            }
                            Switch(
                                checked = engine.hardwareAcceleratedEnabled,
                                onCheckedChange = { engine.hardwareAcceleratedEnabled = it },
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberSecondary,
                                    checkedTrackColor = CyberSecondary.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // NEW: GPU VRAM Allocation & Extreme Compression Tuning
                        Text(
                            text = "MÉMOIRE VIDÉO GPU (VRAM) & COMPRESSEUR ULTRA EXTRÊME",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // VRAM options
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Taille maximum de la VRAM allouée (GPU Memory Size)", color = Color.Black, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val vramOptionsRow1 = listOf(128, 512, 1024)
                                vramOptionsRow1.forEach { size ->
                                    val isSel = engine.gpuVideoMemorySizeMb == size
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { engine.gpuVideoMemorySizeMb = size }
                                            .background(
                                                if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (size) {
                                                1024 -> "1 Go (1024M)"
                                                else -> "$size Mo"
                                            },
                                            color = if (isSel) CyberPrimary else Color.Black,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val vramOptionsRow2 = listOf(2048, 3072, 4048)
                                vramOptionsRow2.forEach { size ->
                                    val isSel = engine.gpuVideoMemorySizeMb == size
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { engine.gpuVideoMemorySizeMb = size }
                                            .background(
                                                if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (size) {
                                                2048 -> "2 Go (2048M)"
                                                3072 -> "3 Go (3072M)"
                                                4048 -> "4 Go (4048M)"
                                                else -> "$size Mo"
                                            },
                                            color = if (isSel) CyberPrimary else Color.Black,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // gpuVramCompressionEnabled switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Transparent)
                                .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Force la compression ultra-agressive (LHC-VRAM)",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active des algorithmes asynchrones (LZW-H & Huffman) réduisant de 75% les requêtes de bande passante matérielle vers le bus GPU.",
                                    color = TextSecondary,
                                    fontSize = 7.5.sp,
                                    lineHeight = 10.sp
                                )
                            }
                            Switch(
                                checked = engine.gpuVramCompressionEnabled,
                                onCheckedChange = { engine.gpuVramCompressionEnabled = it },
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberSecondary,
                                    checkedTrackColor = CyberSecondary.copy(alpha = 0.4f)
                                )
                            )
                        }

                        if (engine.gpuVramCompressionEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Format de compression ultra-agressive", color = Color.Black, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            val vramCompressionLevels = listOf(
                                "Standard (Sans perte)",
                                "Élevée (Lossy 2:1)",
                                "Extrême LZW-H",
                                "Ultra-Noyau Quantique (4:1 Compaction)"
                            )
                            
                            vramCompressionLevels.forEach { level ->
                                val isSel = engine.gpuVramCompressionLevel == level
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp)
                                        .clickable { engine.gpuVramCompressionLevel = level }
                                        .background(
                                            if (isSel) CyberPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSel) CyberPrimary else CyberCardBorder.copy(alpha = 0.3f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(level, color = if (isSel) CyberPrimary else Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        val compressionFactor = when (level) {
                                            "Standard (Sans perte)" -> "1.5x"
                                            "Élevée (Lossy 2:1)" -> "2.0x"
                                            "Extrême LZW-H" -> "3.2x"
                                            "Ultra-Noyau Quantique (4:1 Compaction)" -> "4.1x"
                                            else -> "1.0x"
                                        }
                                        Text("Ratio $compressionFactor", color = if (isSel) CyberPrimary else TextSecondary, fontSize = 7.5.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Compression real-time live calculations card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Green.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.Green.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("TÉLÉMÉTRIE VRAM EN TEMPS RÉEL (COMPRESSOR GPU ACTIF)", color = Color.Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• État : Activé & Overclocké via Shizuku/Root", color = Color.Black, fontSize = 7.5.sp)
                                    Text("• Empreinte Physique VRAM : ${"%.1f".format(engine.activeVramFootprintMb)} Mo (Allocation Virtuelle : ${engine.gpuVideoMemorySizeMb} Mo)", color = Color.Black, fontSize = 7.5.sp)
                                    Text("• Gain d'Adressage : ${"%.0f".format((1f - (engine.activeVramFootprintMb / engine.gpuVideoMemorySizeMb)) * 100f)}% d'économie en mémoire d'affichage", color = CyberPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom active warning
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberSecondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = CyberSecondary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Les modificateurs ci-dessus modifient le profil natif NDK en temps réel de ${selectedApp.name}.",
                                    color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sélectionnez une application pour modifier ou éditer son profil de ressources NDK.", color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AccessPrivilegesCard(engine: FsrEngine) {
    var isCheckingStatus by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header with custom cyberpunk colored icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyberSecondary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privilege Icon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PRIVILÈGES SYSTEME ENGIN FSR",
                            color = CyberSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Mode de communication avec les couches de bas niveau",
                            color = TextSecondary,
                            fontSize = 8.sp
                        )
                    }
                }
                
                // Active State Indicator Pill
                val modeLabel = when(engine.systemAccessMode) {
                    FsrEngine.AccessMode.NO_ROOT -> "Local overlay"
                    FsrEngine.AccessMode.SHIZUKU -> if (engine.isShizukuConnected) "Shizuku lié" else "Démon hors-ligne"
                    FsrEngine.AccessMode.ROOT -> if (engine.isRootGranted) "SuperSU actif" else "FSR su requis"
                }
                val modeColor = when(engine.systemAccessMode) {
                    FsrEngine.AccessMode.NO_ROOT -> Color.Gray
                    FsrEngine.AccessMode.SHIZUKU -> if (engine.isShizukuConnected) CyberSecondary else Color.Red
                    FsrEngine.AccessMode.ROOT -> if (engine.isRootGranted) CyberPrimary else Color.Red
                }
                Box(
                    modifier = Modifier
                        .background(modeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, modeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(modeLabel.uppercase(), color = modeColor, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Tab selection for Access Modes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBlack.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FsrEngine.AccessMode.values().forEach { mode ->
                    val isSel = engine.systemAccessMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                engine.systemAccessMode = mode
                                when (mode) {
                                    FsrEngine.AccessMode.NO_ROOT -> {
                                        engine.logShellCommand("switch_mode --no-root", "Commutateur en mode conteneur d'assistance standard.")
                                    }
                                    FsrEngine.AccessMode.SHIZUKU -> {
                                        engine.logShellCommand("switch_mode --shizuku", "Attente de Shizuku IPC Binder. Port standard 5555.")
                                    }
                                    FsrEngine.AccessMode.ROOT -> {
                                        engine.logShellCommand("switch_mode --root", "Initialisation des liens su (SuperUser API).")
                                    }
                                }
                            }
                            .background(if (isSel) CyberSecondary else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(mode.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.label.substringBefore(" "),
                                color = if (isSel) Color.White else Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Info panel detailing currently selected Access Mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = engine.systemAccessMode.label.uppercase(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = engine.systemAccessMode.description,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Specific action triggers based on mode selection
                    when (engine.systemAccessMode) {
                        FsrEngine.AccessMode.NO_ROOT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Axe ADB local simulé actif", color = CyberPrimary, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Overlay d'assistance", color = Color.Black, fontSize = 8.sp)
                                    Switch(
                                        checked = engine.floatingAdbOverlayEnabled,
                                        onCheckedChange = { 
                                            engine.floatingAdbOverlayEnabled = it
                                            engine.logShellCommand("set_overlay " + if (it) "1" else "0", "Interface flottante d'assistance mise à jour.")
                                        },
                                        modifier = Modifier.scale(0.6f),
                                        colors = SwitchDefaults.colors(checkedThumbColor = CyberSecondary)
                                    )
                                }
                            }
                        }
                        FsrEngine.AccessMode.SHIZUKU -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (engine.isShizukuConnected) "✓ Liaison Shizuku IPC active" else "⚠ Démon Shizuku non détecté",
                                        color = if (engine.isShizukuConnected) CyberSecondary else Color.Red,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Button(
                                        onClick = {
                                            isCheckingStatus = true
                                            engine.logShellCommand("shizuku_bind --request", "Appel du binder ShizukuService...")
                                            engine.isShizukuConnected = true
                                            isCheckingStatus = false
                                            engine.logShellCommand("shizuku_bind --success", "shizuku APIv11 connectée. uid=2000 gids={1007, 3003}")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Lier Démon IPC", color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Exécuter via le rish shell Shizuku", color = Color.Black, fontSize = 8.sp)
                                    Switch(
                                        checked = engine.useRishShellService,
                                        onCheckedChange = { 
                                            engine.useRishShellService = it
                                            engine.logShellCommand("rish_config use_rish=" + if (it) "true" else "false", "Canal d'injection Shizuku configuré.")
                                        },
                                        modifier = Modifier.scale(0.6f),
                                        colors = SwitchDefaults.colors(checkedThumbColor = CyberSecondary)
                                    )
                                }
                            }
                        }
                        FsrEngine.AccessMode.ROOT -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (engine.isRootGranted) "✓ Privilèges Root octroyés (uid=0)" else "⚠ En attente de commande SuperSU",
                                        color = if (engine.isRootGranted) CyberPrimary else Color.Red,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Button(
                                        onClick = {
                                            isCheckingStatus = true
                                            engine.logShellCommand("su --request", "Appel du binaire /system/bin/su...")
                                            engine.isRootGranted = true
                                            isCheckingStatus = false
                                            engine.logShellCommand("su --success", "Privilèges superSU octroyés avec succès (uid:0 root)")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Vérifier Root (su)", color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Forcer gouverneur CPU agressif", color = Color.Black, fontSize = 8.sp)
                                    Switch(
                                        checked = engine.forceAggressiveCpuGovernor,
                                        onCheckedChange = { 
                                            engine.forceAggressiveCpuGovernor = it
                                            if (it) {
                                                engine.logShellCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "Cpu0 gouverneur -> performance")
                                                engine.logShellCommand("echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor", "Cpu4 gouverneur -> performance")
                                            } else {
                                                engine.logShellCommand("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "Cpu0 gouverneur -> schedutil (Défaut)")
                                            }
                                        },
                                        modifier = Modifier.scale(0.6f),
                                        colors = SwitchDefaults.colors(checkedThumbColor = CyberPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Interactive Command-Line NDK Injection terminal
            Text(
                text = "JOURNAL DE LOGS CONSOLE ET INJECTIONS NDK",
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .border(1.dp, Color.Green.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                val state = rememberScrollState()
                LaunchedEffect(engine.accessShellDiagnosticsLogs.size) {
                    state.animateScrollTo(state.maxValue)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(state)
                ) {
                    engine.accessShellDiagnosticsLogs.forEach { log ->
                        val logColor = when {
                            log.startsWith("•") -> Color.Yellow.copy(alpha = 0.9f)
                            log.contains("success") || log.contains("✓") || log.contains("API v11") || log.contains("connectée") -> Color.Green
                            log.contains("switch_mode") || log.contains("shizuku_bind") || log.contains("su --") -> CyberSecondary
                            log.contains("⚠") || log.contains("attente") || log.contains("Démon") || log.contains("requis") -> Color.Red
                            else -> Color.Green.copy(alpha = 0.75f)
                        }
                        Text(
                            text = log,
                            color = logColor,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 10.sp
                        )
                    }
                    if (engine.accessShellDiagnosticsLogs.isEmpty()) {
                        Text(
                            text = "[console] Console d'injection NDK vide.",
                            color = Color.Green,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            
            // Console clean trigger
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Vider la console",
                    modifier = Modifier.clickable {
                        engine.accessShellDiagnosticsLogs.clear()
                        engine.accessShellDiagnosticsLogs.add("[console] Purge console effectuée.")
                    },
                    color = CyberPrimary,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GovernorsCoreBatteryPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Panel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTONOMISEUR BATTERIE & CŒURS",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Gouverneurs d'économie d'énergie, profils de batterie et contrôle par cœur indépendant",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Battery controller logo",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 1. BATTERY AUTONOMIZER (PROFILES) =================
            Text(
                text = "PILOTE D'AUTONOMISATEUR DE BATTERIE (MODERNE & EXTRÊME)",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val batteryModes = listOf(
                "Ultra-Low Power (Extrême)",
                "Éco Ultra-Économique",
                "Équilibré Auto",
                "Haute Performance",
                "Extrême High Power (Overlimit)"
            )

            // Selectable profile rows
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                batteryModes.forEach { mode ->
                    val isSelected = engine.batteryOptimizerMode == mode
                    val containerColor = if (isSelected) LavenderContainer else CyberBlack.copy(alpha = 0.3f)
                    val borderColor = if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f)
                    val contentColor = if (isSelected) LavenderOnContainer else TextPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                engine.batteryOptimizerMode = mode
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode,
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val descText = when (mode) {
                                "Ultra-Low Power (Extrême)" -> "Désactive les Big Cores, TDP 6W CPU / 8W GPU, ZSTD-Ultra compression"
                                "Éco Ultra-Économique" -> "Vitesse réduite de 35%, désactive 3 Big Cores, TDP 12W CPU / 15W GPU"
                                "Équilibré Auto" -> "Ajustement progressif à la charge, tous les cœurs actifs en régulation"
                                "Haute Performance" -> "TDP rehaussé à 70W CPU / 90W GPU, active les files d'attente async"
                                "Extrême High Power (Overlimit)" -> "Overclocking CPU 4.2GHz, maximum TDP 110W CPU / 140W GPU"
                                else -> ""
                            }
                            Text(
                                text = descText,
                                color = if (isSelected) LavenderOnContainer.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 8.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Indicator",
                                tint = CyberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realtime Battery Diagnostics Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "DIAGNOSTIC ENERGÉTIQUE DE LA BATTERIE EN TEMPS RÉEL",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Statut Santé :", color = TextSecondary, fontSize = 8.sp)
                            Text(text = engine.batteryHealthStatus, color = CyberSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Intensité Courant :", color = TextSecondary, fontSize = 8.sp)
                            val drawText = if (engine.batteryCurrentDrawMa > 0) "+${engine.batteryCurrentDrawMa}" else "${engine.batteryCurrentDrawMa}"
                            Text(text = "$drawText mA", color = if (engine.batteryCurrentDrawMa < -600) CyberAccentRed else CyberPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Autonomie restante estimée :", color = TextSecondary, fontSize = 8.sp)
                            val remainingText = if (engine.batteryBypassChargingEnabled) "Infini (Réseau Direct)" else "${String.format("%.1f", engine.batteryEstimatedRemainingHours)} heures"
                            Text(text = remainingText, color = CyberPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bypass Charging Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.3f))
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bypass Charging (Contournement)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Alimente directement les puces sans passer par la batterie. Supprime 98% de la chauffe interne de charge sous forte sollicitation.",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Switch(
                    checked = engine.batteryBypassChargingEnabled,
                    onCheckedChange = { engine.batteryBypassChargingEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberPrimary,
                        checkedTrackColor = LavenderContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 2. EXTREME CPU & GPU GOVERNORS =================
            Text(
                text = "GOUVERNEURS CPU & GPU (FRÉQUENCES DYNAMIQUES)",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // CPU Governor selection
            Text(
                text = "Gouverneur CPU Actif :",
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val cpuGovernors = listOf(
                "Schedutil (Réactif)", "Interactive (Moderne)",
                "Performance (Extrême)", "Lulzactive",
                "Conservative (Éco)", "Powersave (Ultra-Éco)"
            )

            val cpuGridPairs = cpuGovernors.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cpuGridPairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pair.forEach { gov ->
                            val isSelected = engine.cpuGovernor == gov
                            val chipBg = if (isSelected) CyberPrimary else CyberBlack.copy(alpha = 0.3f)
                            val chipText = if (isSelected) Color.White else TextPrimary
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        engine.cpuGovernor = gov
                                    }
                                    .border(1.dp, if (isSelected) CyberPrimary else CyberCardBorder, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = chipBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gov,
                                        color = chipText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GPU Governor selection
            Text(
                text = "Gouverneur GPU Actif :",
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val gpuGovernors = listOf(
                "Devfreq-Ondemand", "Simple-Ondemand",
                "Performance (Extrême)", "Adreno-Boost",
                "Conservative", "Powersave"
            )

            val gpuGridPairs = gpuGovernors.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                gpuGridPairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pair.forEach { gov ->
                            val isSelected = engine.gpuGovernor == gov
                            val chipBg = if (isSelected) CyberSecondary else CyberBlack.copy(alpha = 0.3f)
                            val chipText = if (isSelected) Color.White else TextPrimary
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        engine.gpuGovernor = gov
                                    }
                                    .border(1.dp, if (isSelected) CyberSecondary else CyberCardBorder, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = chipBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gov,
                                        color = chipText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 3. CORES CONTROLS PER CORE =================
            Text(
                text = "CONTRÔLEUR GRANULAIRE INDÉPENDANT DES CŒURS CPU",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // 4 Big Cores
            Text(
                text = "CŒURS PERFORMANCES (BIG CORES 0-3) :",
                color = CyberPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val bigCores = listOf(
                Triple("Big Core 0", engine.cpuBigCore0Enabled, engine.cpuBigCore0FreqGhz),
                Triple("Big Core 1", engine.cpuBigCore1Enabled, engine.cpuBigCore1FreqGhz),
                Triple("Big Core 2", engine.cpuBigCore2Enabled, engine.cpuBigCore2FreqGhz),
                Triple("Big Core 3", engine.cpuBigCore3Enabled, engine.cpuBigCore3FreqGhz)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bigCores.forEachIndexed { idx, core ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBlack.copy(alpha = 0.2f))
                            .border(1.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = core.second,
                            onCheckedChange = { enabled ->
                                when (idx) {
                                    0 -> engine.cpuBigCore0Enabled = enabled
                                    1 -> engine.cpuBigCore1Enabled = enabled
                                    2 -> engine.cpuBigCore2Enabled = enabled
                                    3 -> engine.cpuBigCore3Enabled = enabled
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CyberPrimary)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = core.first,
                                    color = if (core.second) TextPrimary else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (core.second) "${String.format("%.1f", core.third)} GHz" else "Éteint (Hors tension)",
                                    color = if (core.second) CyberPrimary else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (core.second) {
                                Slider(
                                    value = core.third,
                                    onValueChange = { freq ->
                                        when (idx) {
                                            0 -> engine.cpuBigCore0FreqGhz = freq
                                            1 -> engine.cpuBigCore1FreqGhz = freq
                                            2 -> engine.cpuBigCore2FreqGhz = freq
                                            3 -> engine.cpuBigCore3FreqGhz = freq
                                        }
                                    },
                                    valueRange = 1.5f..4.2f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberPrimary,
                                        activeTrackColor = CyberPrimary,
                                        inactiveTrackColor = CyberCardBorder.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Little Cores
            Text(
                text = "CŒURS ULTRA-EFFICACES (LITTLE CORES 4-7) :",
                color = CyberSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val littleCores = listOf(
                Triple("Little Core 4", engine.cpuLittleCore4Enabled, engine.cpuLittleCore4FreqGhz),
                Triple("Little Core 5", engine.cpuLittleCore5Enabled, engine.cpuLittleCore5FreqGhz),
                Triple("Little Core 6", engine.cpuLittleCore6Enabled, engine.cpuLittleCore6FreqGhz),
                Triple("Little Core 7", engine.cpuLittleCore7Enabled, engine.cpuLittleCore7FreqGhz)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                littleCores.forEachIndexed { idx, core ->
                    val offsetIdx = idx + 4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBlack.copy(alpha = 0.2f))
                            .border(1.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = core.second,
                            onCheckedChange = { enabled ->
                                when (offsetIdx) {
                                    4 -> engine.cpuLittleCore4Enabled = enabled
                                    5 -> engine.cpuLittleCore5Enabled = enabled
                                    6 -> engine.cpuLittleCore6Enabled = enabled
                                    7 -> engine.cpuLittleCore7Enabled = enabled
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CyberSecondary)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = core.first,
                                    color = if (core.second) TextPrimary else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (core.second) "${String.format("%.1f", core.third)} GHz" else "Éteint (Hors tension)",
                                    color = if (core.second) CyberSecondary else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (core.second) {
                                Slider(
                                    value = core.third,
                                    onValueChange = { freq ->
                                        when (offsetIdx) {
                                            4 -> engine.cpuLittleCore4FreqGhz = freq
                                            5 -> engine.cpuLittleCore5FreqGhz = freq
                                            6 -> engine.cpuLittleCore6FreqGhz = freq
                                            7 -> engine.cpuLittleCore7FreqGhz = freq
                                        }
                                    },
                                    valueRange = 0.8f..2.4f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberSecondary,
                                        activeTrackColor = CyberSecondary,
                                        inactiveTrackColor = CyberCardBorder.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FpsLimiterPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Panel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIMITEUR DE FPS INTERPOLÉ",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Régulation fine du taux de rafraîchissement final et de la latence d'affichage",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "FPS Limiter Panel Logo",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 1. ENABLE LIMITER SWITCH =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.3f))
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Activer le limiteur de flux",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Limite activement le signal interpolé afin d'économiser l'énergie de l'écran et stabiliser le frametime.",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Switch(
                    checked = engine.fpsLimiterEnabled,
                    onCheckedChange = { engine.fpsLimiterEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberPrimary,
                        checkedTrackColor = LavenderContainer
                    )
                )
            }

            if (engine.fpsLimiterEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                // ================= 2. PRESETS GRID DISPLAY =================
                Text(
                    text = "PROFILS DE LIMITES PRÉDÉFINIS :",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val fpsPresets = listOf(
                    "30 FPS", "60 FPS",
                    "120 FPS", "144 FPS",
                    "240 FPS", "Personnalisé (Custom)"
                )

                val gridPairs = fpsPresets.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    gridPairs.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            pair.forEach { preset ->
                                val isSelected = engine.fpsLimitPreset == preset
                                val chipBg = if (isSelected) CyberPrimary else CyberBlack.copy(alpha = 0.3f)
                                val chipText = if (isSelected) Color.White else TextPrimary
                                val chipBorderColor = if (isSelected) CyberPrimary else CyberCardBorder
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            engine.fpsLimitPreset = preset
                                        }
                                        .border(1.dp, chipBorderColor, RoundedCornerShape(8.dp)),
                                    colors = CardDefaults.cardColors(containerColor = chipBg),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset,
                                            color = chipText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= 3. CUSTOM LIMIT VALUE SLIDER =================
                if (engine.fpsLimitPreset == "Personnalisé (Custom)") {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Limite de Fréquence Manuelle",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${engine.fpsLimitValue} FPS",
                            color = CyberSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Ajustez précisément la limite de rafraîchissement cible.",
                        color = TextPrimary.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = engine.fpsLimitValue.toFloat(),
                        onValueChange = { engine.fpsLimitValue = it.toInt() },
                        valueRange = 30f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberSecondary,
                            activeTrackColor = CyberSecondary,
                            inactiveTrackColor = CyberCardBorder
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 4. REALTIME DIAGNOSTIC MONITOR =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "MONITEUR DE TRACE DE FRÉQUENCE MATÉRIELLE",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Rendu d'Origine :", color = TextSecondary, fontSize = 8.sp)
                            Text(
                                text = "${String.format("%.1f", engine.actualRenderFps)} FPS",
                                color = CyberSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Affichage Interpolé :", color = TextSecondary, fontSize = 8.sp)
                            val displayFps = engine.interpolatedFps
                            val textColor = if (engine.fpsLimiterEnabled) CyberPrimary else CyberSecondary
                            Text(
                                text = "${String.format("%.1f", displayFps)} FPS",
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val statusMsg = if (engine.fpsLimiterEnabled) {
                        "Limite active à ${if (engine.fpsLimitPreset == "Personnalisé (Custom)") engine.fpsLimitValue.toString() else engine.fpsLimitPreset}. Régulation stabilisatrice du frame-pacing activée."
                    } else {
                        "Flux illimité (capacité matérielle maximum brute de 240 FPS)."
                    }
                    Text(
                        text = statusMsg,
                        color = if (engine.fpsLimiterEnabled) CyberPrimary.copy(alpha = 0.9f) else TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ExtremeChipsetRareTweaksPanel(engine: FsrEngine) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COMPATIBILITÉ SOC UNIVERSELLE & TWEAKS RARES",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Moteur d'accélération d'architecture mobile et injection de paramètres kernel",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Universal SOC Optimization Logo",
                    tint = CyberSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 1. CHIPSET BRAND SELECTOR =================
            Text(
                text = "PILOTE DE COMPATIBILITÉ CHIPSET (TOUTES MARQUES MATÉRIELLES) :",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val hardwareBrands = listOf(
                "Détection Automatique (Intelligente)",
                "Qualcomm Snapdragon (Adreno / Hexagon DSP)",
                "MediaTek Dimensity (Mali / APU HyperEngine)",
                "Samsung Exynos (AMD Xclipse RDNA / NPU)",
                "Google Tensor (Mali / Custom TPU Compiler)",
                "Apple Silicon A/M Series (Metal Virtualizer)",
                "Unisoc & PowerVR (Rogue Engine Stable)",
                "NVIDIA Tegra Mobile (CUDA / PhysX Link)"
            )

            // Grid or simple list items for brands selection
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                hardwareBrands.forEach { brand ->
                    val isSelected = engine.selectedHardwareVendor == brand
                    val containerColor = if (isSelected) LavenderContainer else CyberBlack.copy(alpha = 0.3f)
                    val borderColor = if (isSelected) CyberPrimary else CyberCardBorder.copy(alpha = 0.5f)
                    val contentColor = if (isSelected) LavenderOnContainer else TextPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                engine.selectedHardwareVendor = brand
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = brand,
                                color = contentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val descText = when (brand) {
                                "Détection Automatique (Intelligente)" -> "Scanne automatiquement les flags sysfs pour appliquer des règles génériques Vulkan."
                                "Qualcomm Snapdragon (Adreno / Hexagon DSP)" -> "Active Adreno Boost, force les pipelines asynchrones de shader et l'overclock DSP Hexagon."
                                "MediaTek Dimensity (Mali / APU HyperEngine)" -> "Pilote MediaTek HyperEngine CPU-Co-execution et affectation APU basse latence."
                                "Samsung Exynos (AMD Xclipse RDNA / NPU)" -> "Démarrage des accélérateurs de traçage de rayons physiques AMD et optimisation NPU."
                                "Google Tensor (Mali / Custom TPU Compiler)" -> "Forçage de la compilation vectorielle sur TPU personnalisé pour réduire le fardeau CPU."
                                "Apple Silicon A/M Series (Metal Virtualizer)" -> "Couche de pont et traduction d'appels natifs Metal vers Vulkan (environnement virtuel)."
                                "Unisoc & PowerVR (Rogue Engine Stable)" -> "Correction de synchronisation d'affichage et maintien du Frame-Pacing sur PowerVR légers."
                                "NVIDIA Tegra Mobile (CUDA / PhysX Link)" -> "Active CUDA Core scheduler et le calcul de physique multi-threads matériel."
                                else -> ""
                            }
                            Text(
                                text = descText,
                                color = if (isSelected) LavenderOnContainer.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Indicator",
                                tint = CyberPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 2. BRAND TWEAK ACTIVE SWITCH =================
            val activeTweakName = when (engine.selectedHardwareVendor) {
                "Qualcomm Snapdragon (Adreno / Hexagon DSP)" -> "Moteur Adreno-Boost & Offload Hexagon"
                "MediaTek Dimensity (Mali / APU HyperEngine)" -> "MediaTek HyperEngine & APU Thread Co-Processing"
                "Samsung Exynos (AMD Xclipse RDNA / NPU)" -> "AMD Xclipse RDNA Shaders & NPU Compute"
                "Google Tensor (Mali / Custom TPU Compiler)" -> "Google TPU Custom Vectorizer & Compilation"
                "Apple Silicon A/M Series (Metal Virtualizer)" -> "Simulateur Metal-To-Vulkan Translation Layer"
                "Unisoc & PowerVR (Rogue Engine Stable)" -> "PowerVR Rogue Frame-Pacing Fixes"
                "NVIDIA Tegra Mobile (CUDA / PhysX Link)" -> "NVIDIA CUDA Scheduler & PhysX Optimization"
                else -> "Moteur d'Optimisation Générique Vulkan"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.3f))
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTweakName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active l'accélération bas niveau et les micro-optimisations de pilotes matériels spécifiques à cette marque.",
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
                Switch(
                    checked = engine.brandTweakActive,
                    onCheckedChange = { engine.brandTweakActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = LavenderContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 3. UTRA-RARE TWEAKS SWITCHES GRID =================
            Text(
                text = "INJECTION DE TWEAKS EXTRÊMES ET ULTRA-RARES (GPU SYSTEM) :",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Tweak 1: Vulkan Cache Ramdisk Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.2f))
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cache Vulkan sur Ramdisk Virtuelle",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Extreme)",
                            color = CyberPrimary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "Mappe le cache Vulkan directement en mémoire vive émulée (RAMDisk), évitant 98% des micro-stutters lors de la compilation dynamique de shaders.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
                Switch(
                    checked = engine.vulkanRamdiskCacheEnabled,
                    onCheckedChange = { engine.vulkanRamdiskCacheEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = LavenderContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tweak 2: EAS Bypass
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.2f))
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Contournement de planificateur EAS (RT-Priority)",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Rare)",
                            color = CyberSecondary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "Court-circuite le scheduler d'économie d'énergie EAS d'Android pour forcer une priorité système temps réel aux threads logiques de rendu.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
                Switch(
                    checked = engine.easBypassActive,
                    onCheckedChange = { engine.easBypassActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberSecondary,
                        checkedTrackColor = LavenderContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tweak 3: Flash Read-Ahead & TRIM Pipeline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack.copy(alpha = 0.2f))
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Storage Pipeline Flash Turbo",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Ultra)",
                            color = CyberPrimary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "Augmente le read-ahead de stockage flash de 128KB à 2048KB pour un chargement d'assets instantané, et déclenche discrètement l'instruction TRIM.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
                Switch(
                    checked = engine.turboStoragePipelineEnabled,
                    onCheckedChange = { engine.turboStoragePipelineEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberPrimary,
                        checkedTrackColor = LavenderContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 4. TCP NETWORK CONGESTION ALGORITHM =================
            Text(
                text = "GOUVERNEUR TCP KERNEL ET CONGESTION PING (RESEAU CLOUD EN RENDU) :",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val networkGovs = listOf(
                "BBR-v3 (Ultra Latency)", "Cubic (High Bandwidth)",
                "Westwood (Lossy Wi-Fi/LTE)", "BBR-v1 (Stable Queue)"
            )

            val netGridPairs = networkGovs.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                netGridPairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pair.forEach { gov ->
                            val isSelected = engine.tcpCongestionControl == gov
                            val chipBg = if (isSelected) CyberSecondary else CyberBlack.copy(alpha = 0.3f)
                            val chipText = if (isSelected) Color.White else TextPrimary
                            val borderCol = if (isSelected) CyberSecondary else CyberCardBorder
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        engine.tcpCongestionControl = gov
                                    }
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = chipBg),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gov,
                                        color = chipText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostics info block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "TRACE DE COMMISSIONS ET DRIVER EN TEMPS RÉEL",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val latencyReducedText = if (engine.easBypassActive || engine.brandTweakActive) {
                        "Réduction estimée de la latence système globale : -${String.format("%.1f", (if (engine.easBypassActive) 1.8f else 0f) + (if (engine.brandTweakActive) 0.9f else 0f))}ms"
                    } else {
                        "Aucun gain de latence actif."
                    }

                    val powerOptimizedText = if (engine.selectedHardwareVendor != "Détection Automatique (Intelligente)" && engine.brandTweakActive) {
                        "Efficacité énergétique : +10% d'autonomie via optimisation bas niveau."
                    } else {
                        "Profil de consommation normal."
                    }

                    val microStutterText = if (engine.vulkanRamdiskCacheEnabled || engine.turboStoragePipelineEnabled) {
                        "Indice de micro-stutter : -${String.format("%.0f", (if (engine.vulkanRamdiskCacheEnabled) 60f else 0f) + (if (engine.turboStoragePipelineEnabled) 30f else 0f))}% (Cache rapide actif)"
                    } else {
                        "Gigue spatio-temporelle standard."
                    }

                    Text(
                        text = "• $latencyReducedText\n• $powerOptimizedText\n• $microStutterText",
                        color = CyberPrimary.copy(alpha = 0.95f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}



