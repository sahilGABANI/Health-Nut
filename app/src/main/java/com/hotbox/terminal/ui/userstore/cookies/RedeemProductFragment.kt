package com.hotbox.terminal.ui.userstore.cookies

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.hotbox.terminal.R
import com.hotbox.terminal.api.authentication.LoggedInUserCache
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.getViewModelFromFactory
import com.hotbox.terminal.base.extension.showToast
import com.hotbox.terminal.base.extension.subscribeAndObserveOnMainThread
import com.hotbox.terminal.databinding.FragmentCookiesBinding
import com.hotbox.terminal.databinding.FragmentRedeemProductBinding
import com.hotbox.terminal.ui.main.orderdetail.OrderDetailsFragment
import com.hotbox.terminal.ui.userstore.AddToCartDialogFragment
import com.hotbox.terminal.ui.userstore.cookies.view.UserStoreProductAdapter
import com.hotbox.terminal.ui.userstore.customize.CustomizeOrderActivity
import com.hotbox.terminal.ui.userstore.guest.TakeNBackDialogFragment
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreState
import com.hotbox.terminal.ui.userstore.viewmodel.UserStoreViewModel
import javax.inject.Inject

class RedeemProductFragment : BaseFragment() {


    private var redeemPoint: Int = 0
    private var menuId: Int? = 0

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserStoreViewModel>
    private lateinit var userStoreViewModel: UserStoreViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        private const val REDEEM_POINT = "redeemPoint"
        @SuppressLint("StaticFieldLeak")
        private lateinit var userStoreProductAdapter: UserStoreProductAdapter
        private var _binding: FragmentRedeemProductBinding? = null
        private val binding get() = _binding!!
        var listOfProduct: List<ProductsItem>? = null
            set(value) {
                field = value
                updateItems()
            }

        private fun updateItems() {
            if (this::userStoreProductAdapter.isInitialized) {
                val list = listOfProduct?.filter { it.menuActive == true && it.productLoyaltyTier?.tierValue != null && it.productLoyaltyTier?.tierValue != 0 }
                list?.forEach {
                    it.isRedeemProduct = true
                }
                if (list.isNullOrEmpty()) {
                    binding.emptyMessageAppCompatTextView.isVisible = true
                    binding.productDetailsRecycleView.isVisible = false
                } else {
                    binding.productDetailsRecycleView.isVisible = true
                    binding.emptyMessageAppCompatTextView.isVisible = false
                    userStoreProductAdapter.listOfProductDetails = list
                }
            }
        }

        @JvmStatic
        fun newInstance(orderId: Int?): RedeemProductFragment {
            val args = Bundle()
            orderId?.let { args.putInt(REDEEM_POINT, it) }
            val fragment = RedeemProductFragment()
            fragment.arguments = args
            return fragment
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        userStoreViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRedeemProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        redeemPoint = arguments?.getInt(REDEEM_POINT, 0) ?: throw IllegalStateException("No args provided")
        binding.tvLoyaltyPoint.text = redeemPoint.toString()
        binding.tvLoyaltyPoint.isVisible = false
        binding.tvPoint.isVisible = false
        initAdapter()
    }

    private fun initAdapter() {
        userStoreProductAdapter = UserStoreProductAdapter(requireContext()).apply {
            userStoreProductActionState.subscribeAndObserveOnMainThread {
                if (it.productLoyaltyTier?.tierValue!! <= redeemPoint) {
                    val addToCartDialogFragment = AddToCartDialogFragment()
                    AddToCartDialogFragment.listOfProduct = it
                    AddToCartDialogFragment.menuId = it.menuId
                    AddToCartDialogFragment.isRedeemProduct = it.productLoyaltyTier?.tierValue
                    addToCartDialogFragment.show(parentFragmentManager, "")
                } else {
                    showToast("You don't have enough leaves to redeem")
                }
            }.autoDispose()
        }
        binding.productDetailsRecycleView.apply {
            adapter = userStoreProductAdapter
        }
        updateItems()
    }

    private fun listenToViewModel() {
        userStoreViewModel.userStoreState.subscribeAndObserveOnMainThread {
            when (it) {
                is UserStoreState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is UserStoreState.SuccessMessage -> {

                }
                is UserStoreState.SubProductState -> {


                }
                else -> {

                }
            }
        }.autoDispose()
    }
}