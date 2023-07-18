package org.bugm.borg

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ReminderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }
    }
}
