package com.htdev.movie.htplay.widget

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.htdev.movie.htplay.databinding.LayoutToastBinding

object CustomToast {
    fun showToast(context: Context, message: String) {
        if (message.isBlank()) return

        val binding = LayoutToastBinding.inflate(LayoutInflater.from(context))
        binding.tvMessage.text = message

        Toast(context.applicationContext).apply {
            duration = Toast.LENGTH_SHORT
            view = binding.root
            show()
        }
    }
}
