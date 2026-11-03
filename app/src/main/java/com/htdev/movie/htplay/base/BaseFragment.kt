package com.htdev.movie.htplay.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = checkNotNull(_binding) { "Binding is only available during the fragment view lifecycle." }
    abstract fun createViewBinding() : VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createViewBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        bindViewModel()
        handleClickEvent()
    }

    open fun initView(){}
    open fun bindViewModel(){}
    open fun handleClickEvent(){}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
