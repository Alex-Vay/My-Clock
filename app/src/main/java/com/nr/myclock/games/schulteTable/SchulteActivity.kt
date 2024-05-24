package com.nr.myclock.games.schulteTable

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.clock.activities.MainActivity
import com.nr.myclock.R
import com.nr.myclock.databinding.Schulte16ActivityBinding
import com.nr.myclock.databinding.Schulte25ActivityBinding
import com.nr.myclock.databinding.Schulte9ActivityBinding
import kotlin.random.Random


class SchulteActivity : AppCompatActivity() {
    private var nextNum = 1
    private var end = 25
    private var level = 1
    private val sleepTime : Long = 3500
    private val level1Start = 1
    private val level1End = 9
    private val level2Start = 1
    private val level2End = 16
    private val level3Start = Random.nextInt(1, 100 - 25)
    private val level3End = level3Start + 24


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        level = Random.nextInt(1, 4)
        if (level == 1) {
            nextNum = level1Start; end = level1End
            val binding = Schulte9ActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
            val cells = listOf(binding.viewS01, binding.viewS02, binding.viewS03,
                binding.viewS04, binding.viewS05, binding.viewS06,
                binding.viewS07, binding.viewS08, binding.viewS09)
            createTable(cells)
        }
        else if (level == 2) {
            nextNum = level2Start; end = level2End
            val binding = Schulte16ActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
            val cells = listOf(binding.viewS01, binding.viewS02, binding.viewS03, binding.viewS04,
                binding.viewS05, binding.viewS06, binding.viewS07, binding.viewS08,
                binding.viewS09, binding.viewS10, binding.viewS11, binding.viewS12,
                binding.viewS13, binding.viewS14, binding.viewS15, binding.viewS16)
            createTable(cells)
        }
        else {
            nextNum = level3Start; end = level3End
            val binding = Schulte25ActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
            val cells = listOf(binding.viewS01, binding.viewS02, binding.viewS03, binding.viewS04, binding.viewS05,
                binding.viewS06, binding.viewS07, binding.viewS08, binding.viewS09, binding.viewS10,
                binding.viewS11, binding.viewS12, binding.viewS13, binding.viewS14, binding.viewS15,
                binding.viewS16, binding.viewS17, binding.viewS18, binding.viewS19, binding.viewS20,
                binding.viewS21, binding.viewS22, binding.viewS23, binding.viewS24, binding.viewS25)
            createTable(cells)
        }
    }

    private fun createTable(cells : List<TextView>) {
        val numbers = (nextNum..end).shuffled()
        val a = cells.size
        val currentNum : TextView = findViewById(R.id.currentNumberToFind)
        currentNum.text = nextNum.toString()
        for (i in 0 until a) {
            cells[i].text = numbers[i].toString()
            cells[i].setOnClickListener {
                if (nextNum == numbers[i]) {
                    cells[i].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7FFFD4"))
                    nextNum++
                    if (nextNum == end + 1) {
                        currentNum.text = "Вы закончили)"
                        Handler().postDelayed({
                            val m = Intent(this, MainActivity::class.java)
                            startActivity(m)
                        }, sleepTime)
                    }
                    else {
                        currentNum.text = nextNum.toString()
                    }
                }
            }
        }
    }
}