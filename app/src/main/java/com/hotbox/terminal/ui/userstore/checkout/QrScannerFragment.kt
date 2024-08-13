package com.hotbox.terminal.ui.userstore.checkout

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.hotbox.terminal.BuildConfig
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.hotbox.terminal.R
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.FragmentQrScannerBinding
import com.hotbox.terminal.utils.Constants.isDebugMode
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class QrScannerFragment : BaseDialogFragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var codeScanner: CodeScanner

    private val qrDataSubject: PublishSubject<String> = PublishSubject.create()
    val qrData: Observable<String> = qrDataSubject.hide()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
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

    @SuppressLint("SuspiciousIndentation")
    private fun listenToViewEvent() {
        val activity = requireActivity()
        codeScanner = CodeScanner(activity, binding.scannerView)
            codeScanner.apply {
                camera = if (!isDebugMode()) CodeScanner.CAMERA_FRONT else CodeScanner.CAMERA_BACK
                isAutoFocusEnabled = true
                autoFocusMode = AutoFocusMode.SAFE
            }
        codeScanner.startPreview()
        codeScanner.decodeCallback = DecodeCallback {
            qrDataSubject.onNext(it.text)
        }

        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            dismiss()
        }.autoDispose()
    }

}