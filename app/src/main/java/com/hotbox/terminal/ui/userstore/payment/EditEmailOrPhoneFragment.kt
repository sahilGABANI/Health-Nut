package com.hotbox.terminal.ui.userstore.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.hotbox.terminal.R
import com.hotbox.terminal.api.stripe.model.EditType
import com.hotbox.terminal.base.BaseDialogFragment
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentEditEmailOrPhoneBinding
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class EditEmailOrPhoneFragment : BaseDialogFragment() {

    private var _binding: FragmentEditEmailOrPhoneBinding? = null
    private val binding get() = _binding!!
    private var editType = EditType.Email
    private var data = ""



    private val editSuccessSubject: PublishSubject<String> = PublishSubject.create()
    val editSuccess: Observable<String> = editSuccessSubject.hide()

    companion object {

        val EDIT_TYPE = "EDIT_TYPE"
        val DATA = "DATA"
        @JvmStatic
        fun newInstance(editType: EditType, data: String) : EditEmailOrPhoneFragment {
            val listOfExtrasFragment = EditEmailOrPhoneFragment()
            val bundle = Bundle()
            bundle.putEnum(EDIT_TYPE, editType)
            bundle.putString(DATA,data)
            listOfExtrasFragment.arguments = bundle
            return listOfExtrasFragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditEmailOrPhoneBinding.inflate(inflater, container, false)
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
        arguments?.let {
            editType = it.getEnum(EDIT_TYPE,EditType.Email)
            data = it.getString(DATA,"")
        }
        if (data.isNotEmpty() && data != "-") {
            binding.nameEditText.setText(data)
            binding.confirmMaterialButton.isEnabled = data.isNotEmpty()
        }else {
            if (editType.type == EditType.Email.type) {
                binding.nameEditText.hint = "enter Email"
            } else {
                binding.nameEditText.hint = "enter Phone"
            }
        }
        initUi()
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        if (editType.type == EditType.Email.type) {
            binding.nameEditText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            binding.editTypeTextView.text = editType.displayType
            binding.editTypeHeading.text = "Edit Email"
            if (data.isNotEmpty() && data != "-") {
                binding.nameEditText.hint = "enter email"
            }
        } else {
            binding.editTypeTextView.text = editType.displayType
            binding.nameEditText.inputType = InputType.TYPE_CLASS_NUMBER
            binding.editTypeHeading.text = "Edit Phone"
            if (data.isNotEmpty() && data != "-") {
                binding.nameEditText.hint = "enter phone"
            }
        }
        binding.nameEditText.textChanges().skipInitialValue().doOnNext {

        }.debounce(300, TimeUnit.MILLISECONDS, Schedulers.io()).subscribeOnIoAndObserveOnMainThread({
            binding.confirmMaterialButton.isEnabled = it.isNotEmpty()
        }, {
            Timber.e(it)
        }).autoDispose()

        binding.confirmMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            dismiss()
            editSuccessSubject.onNext(binding.nameEditText.text.toString())
        }.autoDispose()
        binding.closeButtonMaterialCardView.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            dismiss()
        }.autoDispose()
    }

}