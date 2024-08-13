package com.hotbox.terminal.ui.main.menu

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotbox.terminal.R
import com.hotbox.terminal.api.menu.model.MenuSectionInfo
import com.hotbox.terminal.api.menu.model.MenusItem
import com.hotbox.terminal.api.menu.model.ProductStateRequest
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.application.HotBoxApplication
import com.hotbox.terminal.base.BaseFragment
import com.hotbox.terminal.base.RxBus
import com.hotbox.terminal.base.RxEvent
import com.hotbox.terminal.base.ViewModelFactory
import com.hotbox.terminal.base.extension.*
import com.hotbox.terminal.databinding.FragmentMenuBinding
import com.hotbox.terminal.ui.main.menu.view.MenuAdapter
import com.hotbox.terminal.ui.main.menu.viewModel.MenuState
import com.hotbox.terminal.ui.main.menu.viewModel.MenuViewModel
import com.hotbox.terminal.utils.Constants
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MenuFragment : BaseFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = MenuFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MenuViewModel>
    private lateinit var menuViewModel: MenuViewModel

    private var category: String? = null
    private var selectedCategory: String? = null
    private lateinit var menuAdapter: MenuAdapter
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private var isCheck: String = Constants.CHECK_ALL
    private var categorySelection: String = Constants.CATEGORY_FILTER_ALL
    private var spinnerArrayList = listOf<String>()
    private var listOfMenu : List<ProductsItem> ? = null
    private var listOfSelectCategoryMenuItem: List<MenusItem>? = null
    private var listOfMenuWithCategory: List<MenusItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HotBoxApplication.component.inject(this)
        menuViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenToViewModel()
        listenToViewEvent()
    }

    private fun listenToViewModel() {
        menuViewModel.menuState.subscribeAndObserveOnMainThread {
            when (it) {
                is MenuState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is MenuState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is MenuState.LoadingState -> {
                    binding.progressBar.isVisible = it.isLoading
                }
                is MenuState.ProductItemInfo -> {
                    setData(it.ProductsItem)
                }
                is MenuState.UpdatedProductItemResponse -> {
                    val list = listOfMenuWithCategory
                    list?.forEach { item ->
                        item.products?.find { item1 -> item1.menuId == it.MenuItemInfo.menuId }?.apply {
                            this.menuActive = it.MenuItemInfo.menuActive
                        }
                    }
                    setMenuByCategoryData(listOfMenuWithCategory)
                }
                is MenuState.MenuItemInfo -> {
                    listOfMenuWithCategory = it.MenuItemInfo
                    setMenuByCategoryData(it.MenuItemInfo)
                    spinnerArrayList = listOf(Constants.CATEGORY_FILTER_ALL)
                    it.MenuItemInfo?.forEach { category ->
                        spinnerArrayList = spinnerArrayList + category.categoryName.toString()
                    }
                    val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, spinnerArrayList)
                    binding.autoCompleteStatus.setAdapter(arrayAdapter)
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun setData(productsInfo: List<ProductsItem>?) {
        listOfMenu = productsInfo
        menuAdapter.listOfMenu = productsInfo
    }

    private fun setMenuByCategoryData(menuInfo: List<MenusItem>?) {
        when (isCheck) {
            Constants.CHECK_ALL -> {
                if (categorySelection != Constants.CATEGORY_FILTER_ALL) {
                    listOfSelectCategoryMenuItem = menuInfo?.filter { it.categoryName == categorySelection }
                    if (listOfSelectCategoryMenuItem?.size != 0) {
                        listOfMenu =   listOfSelectCategoryMenuItem?.get(0)?.products
                    }
                } else {
                    val products = mutableListOf<ProductsItem>()
                    menuInfo?.forEach {
                        products.addAll(it.products ?: listOf())
                    }
                    listOfMenu = products
                }
            }
            Constants.CHECK_AVAILABLE -> {
                if (categorySelection != Constants.CATEGORY_FILTER_ALL) {
                    listOfSelectCategoryMenuItem = menuInfo?.filter { it.categoryName == categorySelection }
                    if (listOfSelectCategoryMenuItem?.size != 0) {
                        listOfMenu =   listOfSelectCategoryMenuItem?.get(0)?.products?.filter { it.menuActive == true }
                    }
                } else {
                    val products = mutableListOf<ProductsItem>()
                    menuInfo?.forEach {
                        products.addAll(it.products?.filter { it.menuActive == true } ?: listOf())
                    }
                    listOfMenu = products
                }
            }
            Constants.CHECK_UNAVAILABLE -> {
                if (categorySelection != Constants.CATEGORY_FILTER_ALL) {
                    listOfSelectCategoryMenuItem = menuInfo?.filter { it.categoryName == categorySelection }
                    if (listOfSelectCategoryMenuItem?.size != 0) {
                        listOfMenu =   listOfSelectCategoryMenuItem?.get(0)?.products?.filter { it.menuActive == false }
                    }
                } else {
                    val products = mutableListOf<ProductsItem>()
                    menuInfo?.forEach {
                        products.addAll(it.products?.filter { it.menuActive == false } ?: listOf())
                    }
                    listOfMenu = products
                }
            }
        }

        menuAdapter.listOfMenu = listOfMenu

    }

    @SuppressLint("SetTextI18n")
    private fun emptyMessageVisibility() {
        binding.emptyMessageAppCompatTextView.isVisible = true
        binding.emptyMessageAppCompatTextView.text = getString(R.string.no_added_any_one_menu)
    }

    @SuppressLint("ResourceType")
    private fun listenToViewEvent() {
        initAdapter()
        binding.allCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = true
            binding.availableCheckBox.isChecked = false
            binding.unavailableCheckBox.isChecked = false
            binding.snoozedItemCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.availableCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.availableCheckBox.isChecked = true
            binding.unavailableCheckBox.isChecked = false
            binding.snoozedItemCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.unavailableCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.availableCheckBox.isChecked = false
            binding.unavailableCheckBox.isChecked = true
            binding.snoozedItemCheckBox.isChecked = false
            onCheckboxClicked()
        }.autoDispose()
        binding.snoozedItemCheckBox.throttleClicks().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.allCheckBox.isChecked = false
            binding.availableCheckBox.isChecked = false
            binding.unavailableCheckBox.isChecked = false
            binding.snoozedItemCheckBox.isChecked = true
            onCheckboxClicked()
        }.autoDispose()

        category = getColoredSpanned(resources.getString(R.string.category), getColor(requireContext(), R.color.grey))
        selectedCategory = getColoredSpanned(resources.getString(R.string.all), getColor(requireContext(), R.color.black))
        binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
        binding.autoCompleteStatus.throttleClicks().subscribeAndObserveOnMainThread {
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, spinnerArrayList)
            binding.autoCompleteStatus.setAdapter(arrayAdapter)
            requireActivity().hideKeyboard()
            binding.autoCompleteStatus.showDropDown()
        }.autoDispose()
        binding.autoCompleteStatus.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            selectedCategory = getColoredSpanned(parent.getItemAtPosition(position).toString(), getColor(requireContext(), R.color.black))
            binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
            categorySelection = parent.getItemAtPosition(position).toString()
            setMenuByCategoryData(listOfMenuWithCategory)
        }
        binding.swipeRefreshLayout.refreshes().subscribeAndObserveOnMainThread {
            requireActivity().hideKeyboard()
            binding.swipeRefreshLayout.isRefreshing = true
            binding.relativeLayout.isVisible = false
            Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                onResume()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.relativeLayout.isVisible = true
            }.autoDispose()
        }.autoDispose()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initAdapter() {
        menuAdapter = MenuAdapter(requireContext()).apply {
            menuActionState.subscribeAndObserveOnMainThread {
                requireActivity().hideKeyboard()
                if (it.menuActive == false) {
                    it.menuId?.let { item -> menuViewModel.updateMenuState(ProductStateRequest(false,item, it.productBasePrice?.toInt())) }
                } else {
                    it.menuId?.let { item -> menuViewModel.updateMenuState(ProductStateRequest(true,item, it.productBasePrice?.toInt())) }
                }
            }.autoDispose()
        }
        binding.rvMenuView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.rvMenuView.apply {
            adapter = menuAdapter
        }
        menuAdapter.headerInfo = MenuSectionInfo(
            getString(R.string.product), getString(R.string.description), getString(R.string.price), getString(R.string.state)
        )
    }

    private fun onCheckboxClicked() {
        isCheck = when {
            binding.allCheckBox.isChecked -> {
                Constants.CHECK_ALL
            }
            binding.availableCheckBox.isChecked -> {
                Constants.CHECK_AVAILABLE
            }
            binding.unavailableCheckBox.isChecked -> {
                Constants.CHECK_UNAVAILABLE
            }
            else -> {
                Constants.CHECK_ALL
            }
        }
        setMenuByCategoryData(listOfMenuWithCategory)
    }

    private fun onRefresh() {
        category = getColoredSpanned(resources.getString(R.string.category), getColor(requireContext(), R.color.grey))
        selectedCategory = getColoredSpanned(resources.getString(R.string.all), getColor(requireContext(), R.color.black))
        binding.autoCompleteStatus.setText(Html.fromHtml("$category $selectedCategory"))
        binding.allCheckBox.isChecked = true
        binding.availableCheckBox.isChecked = false
        binding.unavailableCheckBox.isChecked = false
        menuViewModel.getMenuByLocation(Constants.CHECK_ALL, Constants.CATEGORY_FILTER_ALL)
    }

    override fun onResume() {
        super.onResume()
        RxBus.publish(RxEvent.HideShowEditTextMainActivity(false))
        onRefresh()
    }

    private fun getColoredSpanned(text: String, color: Int): String {
        return "<font color=$color>$text</font>"
    }

    override fun onStart() {
        super.onStart()
    }
}