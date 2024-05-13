package com.nr.myclock.games.memory

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.MainActivity
import com.nr.myclock.databinding.MemoryActivityBinding
import kotlin.random.Random

class MemoryGameActivity : AppCompatActivity() {
    private lateinit var binding : MemoryActivityBinding
    private var numCount = -1
    private var minNum = -1
    private var maxNum = -1
    private var sleepTime = -1L
    private lateinit var placesNum : List<TextView>
    private lateinit var showedNum : List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MemoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        placesNum = listOf(binding.numPlace1, binding.numPlace2, binding.numPlace3,
            binding.numPlace4, binding.numPlace5, binding.numPlace6,
            binding.numPlace7, binding.numPlace8, binding.numPlace9,
            binding.numPlace10, binding.numPlace11, binding.numPlace12,
            binding.numPlace13, binding.numPlace14, binding.numPlace15).shuffled()
        val level = getSharedPreferences("clock_settings", MODE_PRIVATE).getInt("memoryLevel", 3)
        if (level == 1) {
            numCount = 5
            minNum = 1
            maxNum = 10
            sleepTime = 7000L
        }
        else if (level == 2) {
            numCount = 9
            minNum = 1
            maxNum = 30
            sleepTime = 10000L
        }
        else {
            numCount = 15
            minNum = 30
            maxNum = 100
            sleepTime = 12000L
        }
        showedNum = (minNum..maxNum).shuffled()
        placeNumOnScreen()
        if (temp < 3)
            Handler().postDelayed({
                temp += 1
                allNums = showedNum
                numCountFin = numCount
                val finish = Intent(this, FinishActivity::class.java)
                startActivity(finish)
            }, sleepTime)
        else
            Handler().postDelayed({
                val m = Intent(this, MainActivity::class.java)
                startActivity(m)
            }, sleepTime)
    }

    private fun placeNumOnScreen() {
        for (i in 0 until  numCount) {
            val par = placesNum[i].layoutParams as ViewGroup.MarginLayoutParams
            par.leftMargin = Random.nextInt(-(i * 8 + 1), (i * 8 + 1))
            par.topMargin = Random.nextInt(-(i * 8 + 1), (i * 8 + 1))
            placesNum[i].layoutParams = par
            placesNum[i].text = showedNum[i].toString()
        }
    }
}