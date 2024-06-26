package com.nr.myclock.games.game128

import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

var GAMEENDPOINTS = -1

class GameViewModel : ViewModel() {
    private var _game = MutableStateFlow(Game())
    val game: StateFlow<Game> = _game.asStateFlow()

    private val lastMoveTilePosotions = MutableStateFlow(Game())
    private var canUseUndo: Boolean = true

    private var moveScore = 0
    private var previousMoveScore = 0

    private var _gameOver = MutableStateFlow(false)
    val gameOver: StateFlow<Boolean> = _gameOver

    private val sleepTime : Long = 2500

    private val _changeActivityEvent = MutableLiveData<Unit>()

    val changeActivityEvent: LiveData<Unit> = _changeActivityEvent


    init {
        newGame()
    }

    fun setMoveScore(a: Int) {
        moveScore += a
    }

    fun makeSwipe(direction: Directions): Game {
        val temporalArray = _game.value.matrix.array.map { it }.toMutableList()
        swipeToDirection(direction, temporalArray)
        usualMove(temporalArray)
        return _game.value
    }

    private fun swipeToDirection(
        direction: Directions,
        array: MutableList<Int?>
    ): MutableList<Int?> {
        when (direction) {
            Directions.RIGHT -> {
                Swipes(this).swipeToRight(array)
                Log.d("swipes", "Arena: RIGHT")
            }

            Directions.LEFT -> {
                Swipes(this).swipeToLeft(array)
                Log.d("swipes", "Arena: LEFT")
            }

            Directions.UP -> {
                Swipes(this).swipeToUp(array)
                Log.d("swipes", "Arena: UP")
            }

            Directions.DOWN -> {
                Swipes(this).swipeToDown(array)
                Log.d("swipes", "Arena: DOWN")
            }
        }
        return array
    }

    private fun usualMove(array: MutableList<Int?>) {
        if (noTilesChange(array)) {
            return
        }
        lastMoveTilePosotions.value = _game.value
        addNewDigit(array)
        _game.update { game ->
            game.copy(
                matrix = _game.value.matrix.matrixCopy(array),
                score = _game.value.score.plus(moveScore),
                move = _game.value.move.plus(1),
            )
        }
        canUseUndo = true
        previousMoveScore = moveScore
        moveScore = 0
        canContinue(_game.value.matrix.array)
        Log.d("moves", "move =  ${_game.value.move}")
        Log.d("moves", "_gameScore = ${_game.value.score}")
    }

    private fun noTilesChange(temporalArray: MutableList<Int?>): Boolean {
        val array = _game.value.matrix.array
        if (temporalArray == array) {
            Log.d("moves", "noTilesChange: NO CHANGE")
            return true
        }
        return false
    }

    private fun addNewDigit(array: MutableList<Int?>): MutableList<Int?>? {
        val tempArr = mutableListOf<Int>()
        repeat(array.size) {
            if (array[it] == null) {
                tempArr.add(it)
            }
        }
        if (tempArr.isNotEmpty()) {
            val pos = tempArr.random()
            array[pos] = selectRandomDigit()
            Log.d("new digit", "New digit at $pos position")
        } else return null
        return array
    }


    fun newGame(): Game {
        _gameOver.update {
            false
        }
        val position = (0 until _game.value.matrix.array.size).random()
        val value = selectRandomDigit()
        _game.value.matrix.array.replaceAll {
            null
        }
        val temporalArray = _game.value.matrix.array.map { it }.toMutableList()
        temporalArray[position] = value
        _game.update { game ->
            game.copy(
                matrix = _game.value.matrix.matrixCopy(temporalArray),
                score = 0,
                move = 0
            )
        }
        previousMoveScore = 0
        return _game.value
    }

    private fun selectRandomDigit(): Int {
        val a = (0..100).random()
        return if (a < 85) 2 else 4
    }

    private fun canContinue(array: MutableList<Int?>) {
        var isHorizontalSum = false
        var isVerticalSum = false
        val hasNulls = _game.value.matrix.array.contains(null)
        var maxNum = 0
        for (i in 0 until  array.size)
            if (array[i] != null && array[i]!! > maxNum)
                maxNum = array[i]!!
        if (!hasNulls) {
            // проверка по горизонталм
            for (line in 0 until array.size / ROWCOUNT) {
                val startIndex = line * ROWCOUNT
                val endIndex = startIndex + ROWCOUNT - 1

                for (digit1 in startIndex until endIndex) {
                    val digit2 = digit1 + 1
                    if (array[digit1] == array[digit2]) {
                        isHorizontalSum = true
                    }
                }
            }
            // проверка по вертикали
            for (digit1 in 0 until array.size - ROWCOUNT) {
                val digit2 = digit1 + ROWCOUNT
                if (array[digit1] == array[digit2]) {
                    isVerticalSum = true
                }
            }
        }
        if ((!isHorizontalSum && !isVerticalSum && !hasNulls) || maxNum == GAMEENDPOINTS) {
            Log.d("moves", "canContinue: GAMEOVER!")
            _gameOver.update {
                true
            }
        }
    }
}