package com.example.uniride.ui.passenger.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivitySearchFlowBinding


class SearchFlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
