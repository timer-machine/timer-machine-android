package xyz.aprildown.timer.component.key

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import xyz.aprildown.timer.app.base.ui.BaseActivity

class ImagePreviewActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent?.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank()) {
            finish()
            return
        }
        setContent {
            Text(text = path)
        }
    }

    companion object {
        private const val EXTRA_PATH = "path"
        fun getIntent(context: Context, path: String): Intent {
            return Intent(context, ImagePreviewActivity::class.java)
                .putExtra(EXTRA_PATH, path)
        }
    }
}
