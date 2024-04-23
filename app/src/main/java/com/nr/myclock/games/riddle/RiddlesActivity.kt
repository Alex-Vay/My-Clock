package com.nr.myclock.games.riddle

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.MainActivity
import com.nr.myclock.databinding.RiddlesActivityBinding
import com.nr.myclock.games.riddle.DBHelper
import kotlin.math.abs
import kotlin.random.Random

class RiddlesActivity : AppCompatActivity() {
    private lateinit var helper: DBHelper
    private lateinit var db : SQLiteDatabase
    private lateinit var question : String
    private lateinit var correctAns : String
    private lateinit var binding : RiddlesActivityBinding
    private val sleepTime = 2000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RiddlesActivityBinding.inflate(layoutInflater)
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
        val randomNum = Random.nextInt(1, 157 + 1)
        val cursor = db.rawQuery("SELECT * FROM Riddles WHERE id = $randomNum", null)
        if (cursor.moveToFirst()) {
            question = cursor.getString(1)
            correctAns = cursor.getString(2).lowercase()
        }
        cursor.close()
    }

    private fun buttonsBind() {
        val writtenAns = binding.wtireAns
        val ansRes = binding.ansRes
        var tries : Double = 0.0
        writtenAns.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (tries >= 5 || writtenAns.text.toString().lowercase() == correctAns) {
                    ansRes.text = "ПРАВИЛЬНО"
                    Handler().postDelayed({
                        val m = Intent(this, MainActivity::class.java)
                        startActivity(m)
                    }, sleepTime)
                }
                else {
                    var resText = "НЕПРАВИЛЬНО"
                    if (abs(writtenAns.text.toString().length - correctAns.length) < ((correctAns.length / 2) + 3)) tries += 1
                    else {
                        resText = "ХОТЯ БЫ ПОПЫТАЙСЯ"
                        tries += 0.5
                    }
                    ansRes.text = resText
                }
                writtenAns.text.clear()
                true
            } else {
                false
            }
        }
    }
}