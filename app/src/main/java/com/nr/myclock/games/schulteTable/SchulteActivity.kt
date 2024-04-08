package com.nr.myclock.games.schulteTable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.MainActivity
import kotlin.random.Random


class SchulteActivity : AppCompatActivity() {
    private var nextNum = 1
    private var end = 25
    private var level = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        level = getSharedPreferences("clock_settings", Context.MODE_PRIVATE).getInt("schulteLevel", 1)
        setContentView(createTable())
    }

    private fun createButton(num : String): Button {
        val button = Button(this)
        button.text = num
        button.id = num.toInt()
        return button
    }

    private fun setListener(button : Button) {
        button.setOnClickListener {
            if (button.text == nextNum.toString()) {
                nextNum++
                if (nextNum == end + 1) {
                    Thread.sleep(2500)
                    val m = Intent(this, MainActivity::class.java)
                    startActivity(m)
                }
            }
        }
    }

    private fun createTable(): TableLayout {
        var sqrt = 3
        if (level == 1) {
            nextNum = 1; end = 9; sqrt = 3
        }
        else if (level == 2) {
            nextNum = 1; end = 16; sqrt = 4
        }
        else if (level == 3) {
            nextNum = Random.nextInt(1, 100 - 25); end = nextNum + 24; sqrt = 5
        }
        val table = TableLayout(this); sqrt -= 1
        val numbers = (nextNum..end).shuffled()
        for (i in 0 until sqrt) {
            val tableRow = TableRow(this)
            val params = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            tableRow.layoutParams = params
            for (j in 0 until sqrt) {
                val button = createButton((numbers[i * 5 + j]).toString())
                setListener(button)
                params.setMargins(8, 8, 8, 8)
//                button.layoutParams = params
//                button.layoutParams.width = 90
                tableRow.addView(button)
            }
            table.addView(tableRow)
        }
        return table
    }
}