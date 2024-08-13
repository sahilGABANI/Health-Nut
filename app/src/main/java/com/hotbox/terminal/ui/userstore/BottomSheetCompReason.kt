package com.hotbox.terminal.ui.userstore

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hotbox.terminal.R
import com.hotbox.terminal.api.userstore.model.CompReasonType
import com.hotbox.terminal.base.BaseBottomSheetDialogFragment
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.base.extension.throttleClicks
import com.hotbox.terminal.databinding.CompReasonBottomSheetBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BottomSheetCompReason : BaseBottomSheetDialogFragment() {

    private val compReasonClickSubject: PublishSubject<CompReasonType> = PublishSubject.create()
    val compReasonClick: Observable<CompReasonType> = compReasonClickSubject.hide()

    private var _binding: CompReasonBottomSheetBinding? = null
    private val binding get() = _binding!!


    companion object {

        fun newInstance() = BottomSheetCompReason()
    }

    private var userId :Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogThemeRegular)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CompReasonBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        listenToViewEvent()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

    private fun listenToViewEvent() {
        binding.tvLongTicketTime.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.LongTicketTime)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvWrongToGoFood.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.WrongToGoFood)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvDidNotLike.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.DidNotLike)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvForeignObject.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.ForeignObject)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvSpill.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.Spill)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvManagement.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.Management)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvMissedItem.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.MissedItem)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvTraining.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.Training)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvMarketing.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.Marketing)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvEntryError.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.EntryError)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvEightySix.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.EightySix)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvGuestChangedMind.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.GuestChangedMind)
            dismissBottomSheet()
        }.autoDispose()
        binding.tvTest.throttleClicks().subscribeAndObserveOnMainThread {
            compReasonClickSubject.onNext(CompReasonType.Test)
            dismissBottomSheet()
        }.autoDispose()

    }

    fun dismissBottomSheet() {
        dismiss()
    }


}