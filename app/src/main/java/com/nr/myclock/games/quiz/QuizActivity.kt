package com.nr.myclock.games.quiz

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.MainActivity
import com.nr.myclock.databinding.QuestsActivityBinding
import com.nr.myclock.games.quiz.DBHelper
import kotlin.random.Random

class QuizActivity : AppCompatActivity() {
    private lateinit var helper: DBHelper
    private lateinit var db : SQLiteDatabase
    private lateinit var question : String
    private lateinit var correctAns : String
    private lateinit var otherAns : List<String>
    private lateinit var binding : QuestsActivityBinding
    private val sleepTimeCorrectAns = 2000L
    private val sleepTimeWrongAns = 5000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = QuestsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        helper = DBHelper.getInstance(this)
        helper.writableDatabase
        db = helper.readableDatabase
        updateQuestion()
        val quest: TextView = binding.quest
        quest.text = question
        buttonsBind()
    }

    private fun updateQuestion() {
        val randomNum = Random.nextInt(1, 2927 + 1)
        val cursor = db.rawQuery("SELECT * FROM Questions WHERE id = $randomNum", null)
        if (cursor.moveToFirst()) {
            question = cursor.getString(1)
            correctAns = cursor.getString(2)
            otherAns = cursor.getString(3).split(",")
        }
        cursor.close()
    }

    private fun buttonsBind() {
        val buttons = listOf(binding.ans1, binding.ans2, binding.ans3, binding.ans4)
        val questArr = otherAns.plus(correctAns).shuffled()
        val ansRes = binding.ansRes
        for (i in 0 until buttons.size) {
            buttons[i].text = questArr[i]
            buttons[i].setOnClickListener {
                for (j in 0 until buttons.size){
                    if (j != i) buttons[j].isEnabled = false
                }
                if (buttons[i].text == correctAns) {
                    buttons[i].setBackgroundColor(Color.parseColor("#7FFFD4"))
                    ansRes.text = "ПРАВЛЬНЫЙ ОТВЕТ"
//                    Toast.makeText(applicationContext, "ПРАВЛЬНЫЙ ОТВЕТ!", Toast.LENGTH_SHORT).show()
                    Handler().postDelayed({
                        val m = Intent(this, MainActivity::class.java)
                        startActivity(m)
                        }, sleepTimeCorrectAns)
                }
                else {
                    buttons[i].setBackgroundColor(Color.parseColor("#E52B50"))
                    ansRes.text = "НЕПРАВЛЬНЫЙ ОТВЕТ"
                    Handler().postDelayed({
                        for (j in 0 until buttons.size){
                            buttons[j].isEnabled = true
                        }
                    }, sleepTimeWrongAns)
                }
            }
        }
    }
}