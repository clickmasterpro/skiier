package com.example.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainGameApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Home -> HomeScreen(viewModel)
                is Screen.Game -> ActiveGameScreen(viewModel)
                is Screen.Setting -> SettingScreen(viewModel)
                is Screen.Rankings -> RankingsScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: GameViewModel) {
    val nameInput by viewModel.playerName.collectAsStateWithLifecycle()
    var showEmptyAlert by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        // Aesthetic snow background elements
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            // Draw a few abstract hills with code
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.72f)
                    quadraticTo(w * 0.35f, h * 0.62f, w * 0.75f, h * 0.78f)
                    lineTo(w, h * 0.7f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                color = Color(0x35E0F7FA)
            )
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.82f)
                    quadraticTo(w * 0.5f, h * 0.88f, w, h * 0.8f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                color = Color(0x55FFFFFF)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Brand Header Area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 12.dp)
                )
                Text(
                    text = "Go Skiing",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    modifier = Modifier.testTag("app_title")
                )
                Text(
                    text = "The Ultimate Retro Ski Challenge",
                    fontSize = 14.sp,
                    color = Color(0xFFE0F2F1),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Input card section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xAA1E3541)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Rider Name",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { viewModel.setPlayerName(it) },
                        placeholder = { Text("Player name", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFF647C),
                            focusedBorderColor = Color(0xFFFF647C),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_name_input")
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (nameInput.isBlank()) {
                            showEmptyAlert = true
                        } else {
                            viewModel.navigateTo(Screen.Game)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_game_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF647C)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Start Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Rankings) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("rankings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26C6DA)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rankings", fontSize = 15.sp)
                    }

                    Button(
                        onClick = { viewModel.navigateTo(Screen.Setting) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("setting_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78909C)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings", fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (showEmptyAlert) {
        AlertDialog(
            onDismissRequest = { showEmptyAlert = false },
            title = { Text("Invalid Name") },
            text = { Text("Please enter a player name before hitting slope!") },
            confirmButton = {
                TextButton(onClick = { showEmptyAlert = false }) {
                    Text("Invalid", color = Color(0xFFFF647C))
                }
            }
        )
    }
}

@Composable
fun SettingScreen(viewModel: GameViewModel) {
    val selectedColor by viewModel.selectedJacketColor.collectAsStateWithLifecycle()
    var sliderHue by remember { mutableStateOf(selectedColor.toHue()) }

    // Standard preset options
    val colorsList = listOf(
        Color(0xFFFF647C), // Coral Pink (Default)
        Color(0xFFFF9100), // Neon Orange
        Color(0xFF2979FF), // Electrical Blue
        Color(0xFF00E676), // Radiant Green
        Color(0xFFFFD600)  // Golden Yellow
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101C24))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Styling Setup",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Skier Live Preview Box
            Card(
                modifier = Modifier
                    .size(240.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F3543)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        // Draw skier preview center matching active selection jacket
                        val scale = size.minDimension / 100f
                        withTransform({
                            scale(scale, scale)
                            translate(10f, 15f)
                        }) {
                            // Draw nice background slope line
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, 75f),
                                end = Offset(80f, 65f),
                                strokeWidth = 3f
                            )

                            // Skis
                            drawLine(
                                color = Color.Black,
                                start = Offset(15f, 66f),
                                end = Offset(65f, 60f),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )

                            // Legs & Boots
                            drawLine(
                                color = Color.Black,
                                start = Offset(40f, 63f),
                                end = Offset(36f, 52f),
                                strokeWidth = 4f
                            )

                            // Ski Jacket (Body) - Programmatic Custom Color
                            drawPath(
                                path = Path().apply {
                                    moveTo(36f, 52f)
                                    lineTo(42f, 32f)
                                    lineTo(28f, 38f)
                                    close()
                                },
                                color = selectedColor
                            )

                            // Arms & Poles
                            drawLine(
                                color = Color.Black,
                                start = Offset(32f, 36f),
                                end = Offset(40f, 48f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.Gray,
                                start = Offset(20f, 60f),
                                end = Offset(46f, 34f),
                                strokeWidth = 2f
                            )

                            // Helmet (Head)
                            drawCircle(
                                color = Color.DarkGray,
                                radius = 7f,
                                center = Offset(28f, 32f)
                            )

                            // Goggles
                            drawArc(
                                color = Color(0xFF00E5FF),
                                startAngle = -45f,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = 3f),
                                size = Size(10f, 10f),
                                topLeft = Offset(22f, 27f)
                            )
                        }
                    }
                }
            }

            // Controllers Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Skier Jacket Color",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Presets Palette List Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorsList.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.selectJacketColor(color)
                                    sliderHue = color.toHue()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Slider Controller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hue", color = Color.Gray, modifier = Modifier.width(42.dp))
                    Slider(
                        value = sliderHue,
                        onValueChange = {
                            sliderHue = it
                            viewModel.selectJacketColor(Color.fromHue(it))
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedColor,
                            activeTrackColor = selectedColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Save Action
            Button(
                onClick = { viewModel.navigateTo(Screen.Home) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("done_setting_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF647C)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RankingsScreen(viewModel: GameViewModel) {
    val rankingsList by viewModel.allRankings.collectAsStateWithLifecycle()
    val highlightId by viewModel.highlightedId.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101C24))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "Rankings List",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Placeholder to balance row alignment
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Table Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF203543), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pos", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Player", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3.5f))
                Text("Coins", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f), textAlign = TextAlign.End)
                Text("Duration", color = Color.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            if (rankingsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Ranking",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.testTag("no_ranking_display")
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(rankingsList) { index, item ->
                        val rankNum = index + 1
                        val isHighlighted = highlightId == item.id

                        // Rank Color modifiers
                        val rankColor = when (rankNum) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> Color(0xFF90A4AE)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isHighlighted) 2.dp else 0.dp,
                                    color = if (isHighlighted) Color.Yellow else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighlighted) Color(0xFF2A3D34) else Color(0x7F1F3543)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position indicator with background circle for top-3
                                Box(
                                    modifier = Modifier
                                        .weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "$rankNum",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = rankColor
                                    )
                                }

                                // Player Info
                                Text(
                                    text = item.playerName,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isHighlighted) Color.Yellow else Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(3.5f)
                                )

                                // Coins collected
                                Row(
                                    modifier = Modifier.weight(1.8f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${item.coins}",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }

                                // Game Duration
                                Text(
                                    text = "${item.duration} s",
                                    color = Color(0xFFA0FFE6),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.8f)
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
fun ActiveGameScreen(viewModel: GameViewModel) {
    // Listeners for tilt sensors
    TiltSensorHandler(viewModel)

    val playerName by viewModel.playerName.collectAsStateWithLifecycle()
    val coinsCollected by viewModel.coinsCollected.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()

    val invincible by viewModel.isInvincible.collectAsStateWithLifecycle()
    val skaterJumpY by viewModel.skierJumpY.collectAsStateWithLifecycle()
    val jacketColor by viewModel.selectedJacketColor.collectAsStateWithLifecycle()

    val obstacles by viewModel.obstacles.collectAsStateWithLifecycle()
    val coins by viewModel.coins.collectAsStateWithLifecycle()
    val slopeAngleValue by viewModel.slopeAngle.collectAsStateWithLifecycle()
    val treesOffsetValue by viewModel.treesOffset.collectAsStateWithLifecycle()

    val showGameOverDialog by viewModel.showGameOverDialog.collectAsStateWithLifecycle()
    val showSwipeQuitDialog by viewModel.showSwipeQuitDialog.collectAsStateWithLifecycle()

    // Interactive Drag Gesture Tracker (detection of swipes)
    var dragStartX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Combine Drag details for Swipe left-to-right (Quite flow) & Swipe Down (Velocity boost)
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // 1. Check Horizontal swipe from left to right:
                        if (change.position.x - dragStartX > 130f && dragAmount.x > 8f) {
                            viewModel.requestSwipeQuit()
                        }
                        // 2. Check Vertical swipe Down:
                        if (dragAmount.y > 15f) {
                            viewModel.swipeDownVelocityBoost()
                        }
                    }
                )
            }
            .pointerInput(isPaused) {
                // Jump on tapping and Invincibility on long-press
                if (!isPaused) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    // Start tracking duration for long-press
                                    val startTime = System.currentTimeMillis()
                                    var isLong = false
                                    while (true) {
                                        val subEvent = awaitPointerEvent()
                                        val released = subEvent.changes.none { it.pressed }
                                        if (released) {
                                            if (!isLong) {
                                                // It was a short press -> Trigger jump!
                                                viewModel.makeSkierJump()
                                            }
                                            viewModel.cancelLongPress()
                                            break
                                        } else {
                                            if (System.currentTimeMillis() - startTime >= 450) {
                                                if (!isLong) {
                                                    isLong = true
                                                    // Long Press recognized! Activate Invinvibility mode.
                                                    viewModel.startLongPressInvincibility()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // LAYER 1: GRADIENT SKY AND HIGH PEAKS MOUNTAINS RENDER
        MountainBackgroundCanvas()

        // LAYER 2: SCROLLING BACKDROP SEAMLESS PINE TREES RENDER
        TreesScrollingLayer(treesOffsetValue)

        // LAYER 3: DYNAMIC WHITE SLOPE & ACTIVE GAME ITEMS
        GameDynamicsSlopeCanvas(
            slopeAngle = slopeAngleValue,
            jumpY = skaterJumpY,
            selectedColor = if (invincible) Color.Black else jacketColor,
            isInvincibleMode = invincible,
            obstacles = obstacles,
            coins = coins
        )

        // SCREEN RECORDING HUD INDICATOR (Flashing RED REC button)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .safeDrawingPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "flash")
            val alphaAnimation by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = alphaAnimation))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "● REC",
                color = Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // INVINCIBILITY STATE INDICATOR ACCENT
        if (invincible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Yellow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Invincibility Mode",
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // TACTILE ACTIONS HEADER HUD (PAUSE / PLAYER STATS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Pause Toggle button
            FilledIconButton(
                onClick = { viewModel.togglePause() },
                modifier = Modifier
                    .size(50.dp)
                    .testTag("pause_resume_button"),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0x99203543))
            ) {
                if (isPaused) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(width = 5.dp, height = 16.dp).background(Color.White))
                        Box(modifier = Modifier.size(width = 5.dp, height = 16.dp).background(Color.White))
                    }
                }
            }

            // Right: Interactive score indicators card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x99203543)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Rider Name
                    Column {
                        Text("RIDER", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(playerName.ifBlank { "Player" }, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(24.dp))

                    // Coins
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COINS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("$coinsCollected", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(24.dp))

                    // Duration
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TIME", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("${durationSeconds}s", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // FULLSCREEN LAYOVER FOR PAUSED GAME STATUS
        if (isPaused && !showSwipeQuitDialog && !showGameOverDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF647C))
                            .padding(12.dp)
                            .clickable { viewModel.togglePause() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Game suspended...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tap Play button to drop back in",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // SWIPE-QUIT HORIZONTAL VALIDATION PANEL DIALOG
        if (showSwipeQuitDialog) {
            Dialog(onDismissRequest = { viewModel.confirmQuit(false) }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F3543)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFFFF647C),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Quit Skiing?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The game is in progress. Are you sure to quit?",
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.confirmQuit(false) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.Gray)
                            ) {
                                Text("No", color = Color.White)
                            }
                            Button(
                                onClick = { viewModel.confirmQuit(true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF647C))
                            ) {
                                Text("Yes")
                            }
                        }
                    }
                }
            }
        }

        // COLLISION GAME OVER PANEL DIALOG
        if (showGameOverDialog) {
            Dialog(onDismissRequest = { }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3541)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(2.dp, Color(0xFFFF647C), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Game Over",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF647C),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score metrics
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33000000), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rider:", color = Color.Gray, fontSize = 15.sp)
                                Text(playerName.ifBlank { "Player" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Coins collected:", color = Color.Gray, fontSize = 15.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$coinsCollected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Time on slope:", color = Color.Gray, fontSize = 15.sp)
                                Text("${durationSeconds}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Actions
                        Button(
                            onClick = { viewModel.restartGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("restart_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF647C)),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.confirmQuit(true) // Stops the session fully
                                viewModel.navigateTo(Screen.Rankings) // jumps straight to highlights context
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("go_to_rankings_button"),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Go To Rankings", fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// GYROSCOPE/ACCELEROMETER PORTRAIT SIDETILT SCANNER
@Composable
fun TiltSensorHandler(viewModel: GameViewModel) {
    val context = LocalContext.current
    DisposableEffect(viewModel) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val triggerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val tiltX = event.values[0]
                    viewModel.updateTilt(tiltX)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(triggerListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(triggerListener)
        }
    }
}

@Composable
fun MountainBackgroundCanvas() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00B0FF), // Sky blue
                        Color(0xFF80DEEA), // Soft cyan
                        Color(0xFFE0F7FA)  // Clear winter base
                    )
                )
            )
    ) {
        val w = size.width
        val h = size.height

        // Background mountains far layer (softer, lighter)
        drawPath(
            path = Path().apply {
                moveTo(w * -0.2f, h * 0.5f)
                lineTo(w * 0.2f, h * 0.28f)
                lineTo(w * 0.45f, h * 0.45f)
                lineTo(w * 0.82f, h * 0.25f)
                lineTo(w * 1.2f, h * 0.52f)
                lineTo(w * 1.2f, h)
                lineTo(w * -0.2f, h)
                close()
            },
            color = Color(0xFFB2EBF2).copy(alpha = 0.65f)
        )

        // Mid mountains snowy peaks (sharper)
        drawPath(
            path = Path().apply {
                moveTo(0f, h * 0.55f)
                lineTo(w * 0.35f, h * 0.32f) // Peak 1
                lineTo(w * 0.55f, h * 0.48f)
                lineTo(w * 0.72f, h * 0.35f) // Peak 2
                lineTo(w, h * 0.56f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            color = Color(0xFFE0F7FA)
        )

        // Draw snowcaps overlay lines
        drawPath(
            path = Path().apply {
                moveTo(w * 0.35f, h * 0.32f)
                lineTo(w * 0.3f, h * 0.4f)
                lineTo(w * 0.35f, h * 0.38f)
                lineTo(w * 0.39f, h * 0.42f)
                lineTo(w * 0.35f, h * 0.32f)
                close()
            },
            color = Color.White
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.72f, h * 0.35f)
                lineTo(w * 0.68f, h * 0.42f)
                lineTo(w * 0.72f, h * 0.4f)
                lineTo(w * 0.75f, h * 0.43f)
                lineTo(w * 0.72f, h * 0.35f)
                close()
            },
            color = Color.White
        )
    }
}

@Composable
fun TreesScrollingLayer(treesOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.7f // Align with down-hill standard placement

        // Draw multiple repeating trees in sequence with offsets
        val treeSpacing = 220f
        // Let's cover screen width with enough overlapping trees
        val totalNeeded = (w / treeSpacing).toInt() + 3

        for (i in -1..totalNeeded) {
            val baseTreeX = (i * treeSpacing) - treesOffset
            // Only draw if within reasonable horizontal bounds
            if (baseTreeX > -100f && baseTreeX < w + 100f) {
                // Dynamic slight height variation for added rustic style
                val treeHeight = 70f
                val treeWidth = 40f
                val rootY = groundY + (i % 3) * 12f // slightly tiered

                // Draw standard Christmas stylized WSC green trees using vectors drawing
                val treeColor = if (i % 2 == 0) Color(0xFF00695C) else Color(0xFF00796B)
                drawPath(
                    path = Path().apply {
                        // Level 3 (Top tier)
                        moveTo(baseTreeX, rootY - treeHeight)
                        lineTo(baseTreeX - (treeWidth * 0.3f), rootY - (treeHeight * 0.6f))
                        lineTo(baseTreeX + (treeWidth * 0.3f), rootY - (treeHeight * 0.6f))
                        close()

                        // Level 2 (Mid tier)
                        moveTo(baseTreeX, rootY - (treeHeight * 0.7f))
                        lineTo(baseTreeX - (treeWidth * 0.45f), rootY - (treeHeight * 0.3f))
                        lineTo(baseTreeX + (treeWidth * 0.45f), rootY - (treeHeight * 0.3f))
                        close()

                        // Level 1 (Bottom tier)
                        moveTo(baseTreeX, rootY - (treeHeight * 0.4f))
                        lineTo(baseTreeX - (treeWidth * 0.6f), rootY)
                        lineTo(baseTreeX + (treeWidth * 0.6f), rootY)
                        close()
                    },
                    color = treeColor
                )

                // Trunk
                drawRect(
                    color = Color(0xFF3E2723),
                    topLeft = Offset(baseTreeX - 4f, rootY),
                    size = Size(8f, 12f)
                )
            }
        }
    }
}

@Composable
fun GameDynamicsSlopeCanvas(
    slopeAngle: Float,
    jumpY: Float,
    selectedColor: Color,
    isInvincibleMode: Boolean,
    obstacles: List<Obstacle>,
    coins: List<Coin>
) {
    val context = LocalContext.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val groundY = h * 0.72f
        val skierXpx = 100.dp.toPx()

        // Draw the white slope canvas area with matching incline angle rotating
        withTransform({
            // Draw rotated to simulate sloping angle center around coordinates of skier (at x=100dp, groundY)
            val skierYpx = groundY
            rotate(slopeAngle, pivot = Offset(skierXpx, skierYpx))
        }) {
            // Slope path representing the fast snowy ground sliding
            drawPath(
                path = Path().apply {
                    moveTo(-w, groundY)
                    lineTo(w * 2, groundY)
                    lineTo(w * 2, h * 1.5f)
                    lineTo(-w, h * 1.5f)
                    close()
                },
                color = Color.White
            )

            // Draw active rolling Coins along slope
            coins.forEach { coin ->
                val cxPx = coin.x.dp.toPx()
                val cyPx = groundY - 14.dp.toPx() // roll right on snow

                // outer shiny gold ring
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = coin.radiusDp.dp.toPx(),
                    center = Offset(cxPx, cyPx)
                )

                // Inner core with retro star visual symbol
                drawCircle(
                    color = Color(0xFFFFA000),
                    radius = (coin.radiusDp * 0.65f).dp.toPx(),
                    center = Offset(cxPx, cyPx)
                )

                // High-visibility dot shine
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(cxPx - 3.dp.toPx(), cyPx - 3.dp.toPx())
                )
            }

            // Draw active obstacles (red Hurdles barriers)
            obstacles.forEach { obs ->
                val oxPx = obs.x.dp.toPx()
                val oyPx = groundY - obs.heightDp.dp.toPx()

                // Draw hurdle feet
                drawLine(
                    color = Color(0xFFFF3D00), // hot orange red
                    start = Offset(oxPx + 4.dp.toPx(), groundY),
                    end = Offset(oxPx + 4.dp.toPx(), oyPx),
                    strokeWidth = 4.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFFF3D00),
                    start = Offset(oxPx + obs.widthDp.dp.toPx() - 4.dp.toPx(), groundY),
                    end = Offset(oxPx + obs.widthDp.dp.toPx() - 4.dp.toPx(), oyPx),
                    strokeWidth = 4.dp.toPx()
                )

                // Draw horizontal top crossbar of hurdle
                drawRoundRect(
                    color = Color(0xFFFF5722),
                    topLeft = Offset(oxPx, oyPx),
                    size = Size(obs.widthDp.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )

                // Inner yellow indicator bar stripe
                drawRect(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(oxPx + 5.dp.toPx(), oyPx + 2.dp.toPx()),
                    size = Size(obs.widthDp.dp.toPx() - 10.dp.toPx(), 4.dp.toPx())
                )
            }

            // Draw skier programmatically on top of current slope
            // Skier is raised by jumpY vertical units
            val skierYpx = groundY - jumpY.dp.toPx()

            withTransform({
                translate(skierXpx, skierYpx)
            }) {
                // Skis (thick black angled strokes)
                drawLine(
                    color = Color.Black,
                    start = Offset(-25f, -1f),
                    end = Offset(25f, -5f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )

                // Legs & Boots
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, -4f),
                    end = Offset(-4f, -15f),
                    strokeWidth = 5f
                )

                // Ski Jacket Body silhouette - programmatically toggled colors!
                drawPath(
                    path = Path().apply {
                        moveTo(-4f, -15f)
                        lineTo(4f, -35f)
                        lineTo(-12f, -28f)
                        close()
                    },
                    color = selectedColor
                )

                // Arms & Poles leaning
                drawLine(
                    color = Color.Black,
                    start = Offset(-8f, -30f),
                    end = Offset(0f, -18f),
                    strokeWidth = 4f
                )
                // Pole line hitting slope for cool physics feel
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(-22f, 6f),
                    end = Offset(6f, -32f),
                    strokeWidth = 2.5f
                )

                // Head Helmet (solid sphere)
                drawCircle(
                    color = Color.DarkGray,
                    radius = 8.5f,
                    center = Offset(-12f, -35f)
                )

                // Google details (changes to glowing gold if invincible, neon green by default)
                drawArc(
                    color = if (isInvincibleMode) Color.Yellow else Color(0xFF00E676),
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 3.5f),
                    size = Size(11f, 11f),
                    topLeft = Offset(-18f, -40f)
                )
            }
        }
    }
}

// Helpers extension to calculate hue for sliders
private fun Color.toHue(): Float {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    if (max == min) return 0f
    val d = max - min
    val h = when (max) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    }
    return h * 60f
}

// Fabricate a color using a precise HUE slider parameter
private fun Color.Companion.fromHue(hue: Float): Color {
    val h = hue / 60f
    val x = 1f - Math.abs((h % 2f) - 1f)
    val (r, g, b) = when {
        h < 1f -> Triple(1f, x, 0f)
        h < 2f -> Triple(x, 1f, 0f)
        h < 3f -> Triple(0f, 1f, x)
        h < 4f -> Triple(0f, x, 1f)
        h < 5f -> Triple(x, 0f, 1f)
        else -> Triple(1f, 0f, x)
    }
    return Color(r, g, b)
}
