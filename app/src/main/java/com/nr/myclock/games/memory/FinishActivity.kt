package com.nr.myclock.games.memory

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.clock.activities.MainActivity
import com.nr.myclock.databinding.MemoryFinishActivityBinding
import kotlin.random.Random

private var temp = 0

class FinishActivity : AppCompatActivity() {
    private lateinit var binding : MemoryFinishActivityBinding
    private var correctNum = -1
    private val sleepTime = 3000L
    var numCountFin = -1
    lateinit var allNums : List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MemoryFinishActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        numCountFin = getIntent().getIntExtra("numCount", -1)
        allNums = (getIntent().getSerializableExtra("showedNum") as IntArray).toList()
        temp += 1
        buttonsBind()
    }

    private fun buttonsBind() {
        val buttons = listOf(binding.ans1, binding.ans2, binding.ans3, binding.ans4)
        correctNum = allNums[Random.nextInt(numCountFin + 1, allNums.size + 1)]
        val tempAllNums = allNums.slice(0..2).plus(correctNum).shuffled()
        for (i in 0 until tempAllNums.size) {
            buttons[i].text = tempAllNums[i].toString()
            buttons[i].setOnClickListener {
                for (j in 0 until buttons.size){
                    if (j != i) buttons[j].isEnabled = false
                }
                if (buttons[i].text == correctNum.toString()) {
                    buttons[i].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7FFFD4"))
                    Handler().postDelayed({
                        val m = Intent(this, MainActivity::class.java)
                        startActivity(m)
                    }, sleepTime)
                }
                else {
                    buttons[i].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E52B50"))
                    if (temp < 3) {
                        Handler().postDelayed({
                            val memory = Intent(this, MemoryGameActivity::class.java)
                            startActivity(memory)
                        }, sleepTime)
                    }
                    else
                        Handler().postDelayed({
                            val m = Intent(this, MainActivity::class.java)
                            startActivity(m)
                        }, sleepTime)
                }
            }
        }
    }
}