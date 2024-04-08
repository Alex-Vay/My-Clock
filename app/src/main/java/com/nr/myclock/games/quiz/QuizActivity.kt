package com.nr.myclock.games.quiz

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.R
import kotlin.random.Random

class QuizActivity : AppCompatActivity() {
    private lateinit var helper: DBHelper
    private lateinit var db : SQLiteDatabase
    private lateinit var question : String
    private lateinit var correctAns : String
    private lateinit var otherAns : Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_world_time)
        helper = DBHelper.getInstance(this)
        helper.writableDatabase
        db = helper.readableDatabase
        updateQuestion()
        val text: TextView = findViewById(R.id.ttt)
        text.text = question
    }

    private fun updateQuestion() {
        val randomNum = Random.nextInt(1, 2927)
        val cursor = db.rawQuery("SELECT * FROM Questions WHERE id = $randomNum", null)
        if (cursor.moveToFirst()) {
            question = cursor.getString(1)
            correctAns = cursor.getString(2)
            otherAns = cursor.getString(3).split(",").toTypedArray()
        }
        cursor.close()
    }

    fun checkAnswer(ans : String): Boolean {
        if (ans == correctAns) return true
        return false
    }

    fun getQuestionInfo(): Any {
        return object {
            val Question = question
            val CorrectAns = correctAns
            val OtherAns = otherAns
        }
    }
}