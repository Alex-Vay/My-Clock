package com.nr.myclock.games.quiz

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

const val DATABASE_NAME = "Questions.db" /* the database name */
const val ASSET_NAME = "Questions.db" /* The name of the asset file which could be different if required */
const val DATABASE_VERSION = 1
const val ASSET_COPY_BUFFER_SIZE = 8 * 1024
class DBHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME,null, DATABASE_VERSION) {
    companion object {
        private var instance: DBHelper?=null
        fun getInstance(context: Context): DBHelper {
            if (this.instance==null) {
                getAndCopyAssetDatabase(context)
                instance = DBHelper(context);
            }
            return instance as DBHelper
        }

        private fun ifDatabaseExists(context: Context): Boolean {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (dbFile.exists()) return true
            else if (!dbFile.parentFile.exists()) {
                dbFile.parentFile.mkdirs()
            }
            return false
        }

        private fun getAndCopyAssetDatabase(context: Context) {
            if (ifDatabaseExists(context)) return
            context.assets.open(ASSET_NAME).copyTo(
                FileOutputStream(context.getDatabasePath(DATABASE_NAME)),
                ASSET_COPY_BUFFER_SIZE
            )
        }
    }

    override fun onCreate(p0: SQLiteDatabase?) {
        // should not do anything if using a pre-packaged database
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        // May or may not be used
    }
}