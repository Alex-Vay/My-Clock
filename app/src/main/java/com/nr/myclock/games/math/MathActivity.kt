package com.nr.myclock.games.math

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.notkamui.keval.keval
import com.nr.myclock.clock.activities.MainActivity
import com.nr.myclock.databinding.MathActivityBinding
import com.nr.myclock.games.riddle.DBHelper
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt

class MathActivity : AppCompatActivity() {
    private var numbersCount = 0
    private var maxNumber = 0
    private var operationsCount = 0
    private var powNumber = 6
    private var correctAns = 0.0
    private val operations = mapOf(1 to "+", 2 to "-", 3 to "/", 4 to "*", 5 to "%", 6 to "^")
    private lateinit var mathExp : String
    private lateinit var binding : MathActivityBinding
    private val sleepTime = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MathActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val level = Random.nextInt(1, 4)
        if (level == 1) {
            numbersCount = Random.nextInt(4, 10 + 1)
            maxNumber = 10
            operationsCount = 2
        }
        else if (level == 2) {
            numbersCount = Random.nextInt(3, 6 + 1)
            maxNumber = 100
            operationsCount = 4
        }
        else {
            numbersCount = Random.nextInt(4, 8 + 1)
            maxNumber = 128
            operationsCount = 6
        }
        updateQuestion()
        val quest: TextView = binding.quest
        quest.text = mathExp
        buttonsBind()
    }

    private fun updateQuestion() {
        val tempMathExp = StringBuilder()
        tempMathExp.append(Random.nextInt(-maxNumber, maxNumber + 1))
        for (i in 1 until numbersCount) {
            val operation = operations.get(Random.nextInt(1, operationsCount + 1))
            var curNum = Random.nextInt(-maxNumber, maxNumber + 1)
            if (!(operation == "+" && curNum < 0)) tempMathExp.append(operation)
            if (operation == "^") curNum = Random.nextInt(-powNumber, powNumber + 1)
            tempMathExp.append(curNum)
        }
        mathExp = tempMathExp.toString()
        correctAns = mathExp.keval()
    }

    private fun buttonsBind() {
        val writtenAns = binding.wtireAns
        val ansRes = binding.ansRes
        var tries = 0
        writtenAns.setOnEditorActionListener { _, actionId, _ ->
            var curAns = writtenAns.text.toString().replace(',', '.').toDouble()
            val isInt = correctAns % 1 == 0.0
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (tries >= 9 || (abs(curAns - correctAns) <= 1 && !(isInt)) ||
                    (curAns == correctAns && isInt)) {
                    ansRes.text = "ПРАВИЛЬНО"
                    Handler().postDelayed({
                        val m = Intent(this, MainActivity::class.java)
                        startActivity(m)
                    }, sleepTime)
                }
                else {
                    if (curAns > correctAns) ansRes.text = "НЕПРАВИЛЬНО. ОТВЕТ МЕНЬШЕ"
                    else ansRes.text = "НЕПРАВИЛЬНО. ОТВЕТ БОЛЬШЕ"
                    tries += 1
                }
                writtenAns.text.clear()
                true
            } else {
                false
            }
        }
    }
}