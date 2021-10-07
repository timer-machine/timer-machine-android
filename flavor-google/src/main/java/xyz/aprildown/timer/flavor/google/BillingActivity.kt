package xyz.aprildown.timer.flavor.google

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.flavor.google.databinding.ActivityBillingBinding

@AndroidEntryPoint
class BillingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, BillingActivity::class.java)
        }
    }
}
