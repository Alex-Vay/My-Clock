package com.nr.myclock.games.game128

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nr.myclock.MainActivity
import com.nr.myclock.R
import com.nr.myclock.games.game128.ui.Tile
import com.nr.myclock.games.game128.ui.theme.Game2048Theme
import com.nr.myclock.games.game128.ui.theme.GameColors
import kotlin.math.abs

class Game128Activity : ComponentActivity() {
    private val unrealTime : Long = 240000
    private val sleepTime : Long = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val level = getSharedPreferences("clock_settings", MODE_PRIVATE).getInt("game128settings", 1)
        if (level == 1) {
            ROWCOUNT = 4
            GAMEENDPOINTS = 128
        }
        else if (level == 2) {
            ROWCOUNT = 4
            GAMEENDPOINTS = 256
        }
        else if (level == 3) {
            ROWCOUNT = 3
            GAMEENDPOINTS = 128
        }
        else {
            ROWCOUNT = 3
            GAMEENDPOINTS = 256
        }
        setContent {
            Game2048Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    drawerBackgroundColor = MaterialTheme.colors.background
                ) {
                    GameScreen(
                        modifier = Modifier.padding(it)
                    )
                }
            }
        }
    }

    @Composable
    fun GameOverScreen(
        modifier: Modifier = Modifier,
        isGameOver: Boolean,
        viewModel: GameViewModel
    ) {
        if (isGameOver) {
            AlertDialog(
                modifier = modifier
                    .fillMaxWidth(),
                title = {
                    Text(
                        text = stringResource(id = R.string.game_over),
                        modifier.fillMaxWidth(),
                        fontSize = 40.sp,
                        textAlign = TextAlign.Center,
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.next_time),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp
                    )
                },
                backgroundColor = Color.Transparent,
                contentColor = GameColors.Yellow,
                buttons = {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 80.dp),
                        onClick = {
                            Handler().postDelayed({
                                changeActivity()
                            }, sleepTime)
                        }
                    ) {
                        Text(
                            text = "END GAME",
                            fontSize = 20.sp,
                            color = GameColors.Yellow
                        )
                    }
                },
                onDismissRequest = { },
            )
        }
    }

    @Composable
    fun GameScreen(
        modifier: Modifier = Modifier,
        viewModel: GameViewModel = viewModel()
    ) {
        val gameStats by viewModel.game.collectAsState()
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Statistics(
                modifier = Modifier,
                movesCount = gameStats.move,
                scores = gameStats.score
            )
            Buttons(
                modifier = Modifier,
                viewModel = viewModel
            )
            Arena(viewModel = viewModel)
        }
    }

    @Composable
    fun Statistics(
        modifier: Modifier = Modifier,
        movesCount: Int,
        scores: Int
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = (stringResource(id = R.string.moves) + movesCount.toString()),
                modifier = Modifier.padding(2.dp),
                fontSize = 20.sp,
            )
            Text(
                text = (stringResource(id = R.string.scores) + scores.toString()),
                modifier = Modifier.padding(2.dp),
                fontSize = 20.sp,
            )
        }
    }


    @Composable
    fun Buttons(
        modifier: Modifier = Modifier,
        viewModel: GameViewModel,
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    Handler().postDelayed({
                        changeActivity()
                    }, unrealTime)
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.baseline_autorenew_24),
                    contentDescription = "Start new game",
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .rotate(90f)
                )
                Text(text = stringResource(id = R.string.end_game))
            }
        }
    }

    @Composable
    fun Arena(
        modifier: Modifier = Modifier,
        viewModel: GameViewModel
    ) {
        var direction by remember { mutableStateOf(Directions.UP) }
        val gameOver by remember { mutableStateOf(viewModel.gameOver) }
        Box(
            modifier = modifier
                .padding(16.dp)
                .aspectRatio(1f)
                .background(GameColors.Grey)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val (x, y) = dragAmount
                            when {
                                x > 5 && abs(x) > abs(y) -> { direction = Directions.RIGHT } // RIGHT
                                x < -5 && abs(x) > abs(y) -> { direction = Directions.LEFT } // LEFT
                                y > 5 && abs(x) < abs(y) -> { direction = Directions.DOWN } // DOWN
                                y < -5 && abs(x) < abs(y) -> { direction = Directions.UP } // UP
                            }
                        },
                        onDragEnd = {
                            viewModel.makeSwipe(direction)
                        },
                        onDragStart = {},
                        onDragCancel = {}
                    )
                }
        ) {
            GameOverScreen(
                modifier = Modifier,
                isGameOver = gameOver.collectAsState().value,
                viewModel = viewModel
            )

            val vmMatrix by viewModel.game.collectAsState()
            Column {
                repeat(ROWCOUNT) { row ->
                    HorizontalFields(
                        modifier = Modifier,
                        value = vmMatrix.matrix.asMatrix()[row]
                    )
                }
            }
        }
    }

    @Composable
    fun HorizontalFields(
        modifier: Modifier = Modifier,
        value: List<Int?> = listOf()
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            Arrangement.SpaceAround
        ) {
            repeat(ROWCOUNT) { digit ->
                Tile(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(3.dp),
                    value = value[digit],
                )
            }
        }
    }

     fun changeActivity() {
         val m = Intent(this, MainActivity::class.java)
         startActivity(m)
     }
}