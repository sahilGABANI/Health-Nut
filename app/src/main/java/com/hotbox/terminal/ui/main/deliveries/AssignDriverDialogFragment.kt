package com.hotbox.terminal.ui.main.deliveries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.hotbox.terminal.R
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.AssignDriverDialogBinding

class AssignDriverDialogFragment : BaseDialogFragment() {

    private var _binding: AssignDriverDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AssignDriverDialogBinding.inflate(inflater, container, false)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.confirmMaterialButton.isEnabled = false
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dialog?.dismiss()
        }.autoDispose()
        val arrayAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.statusArray,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.assignDriverSpinner.setAdapter(arrayAdapter)
        binding.assignDriverSpinner.onItemClickListener = AdapterView.OnItemClickListener { adapter, v, position, id ->
            binding.confirmMaterialButton.isEnabled = true
        }

        binding.confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.confirmMaterialButton.isEnabled) {
                dialog?.dismiss()
            }
        }.autoDispose()
    }

}