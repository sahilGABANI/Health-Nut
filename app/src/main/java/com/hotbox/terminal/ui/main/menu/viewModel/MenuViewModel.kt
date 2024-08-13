package com.hotbox.terminal.ui.main.menu.viewModel

import com.hotbox.terminal.api.menu.MenuRepository
import com.hotbox.terminal.api.menu.model.MenuListInfo
import com.hotbox.terminal.api.menu.model.MenusItem
import com.hotbox.terminal.api.menu.model.ProductStateRequest
import com.hotbox.terminal.api.menu.model.ProductsItem
import com.hotbox.terminal.base.BaseViewModel
import com.hotbox.terminal.base.extension.subscribeWithErrorParsing
import com.hotbox.terminal.base.network.model.ErrorResult
import com.hotbox.terminal.base.network.model.HotBoxError
import com.hotbox.terminal.utils.Constants
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MenuViewModel(private val menuRepository: MenuRepository) : BaseViewModel() {

    private val menuStateSubject: PublishSubject<MenuState> = PublishSubject.create()
    val menuState: Observable<MenuState> = menuStateSubject.hide()

    fun getMenuByLocation(isCheck: String, categorySelection: String) {
        menuRepository.getMenuByLocation().doOnSubscribe {
            menuStateSubject.onNext(MenuState.LoadingState(true))
        }.doAfterTerminate {
            menuStateSubject.onNext(MenuState.LoadingState(false))
        }.subscribeWithErrorParsing<MenuListInfo, HotBoxError>({ menuListInfo ->
            menuStateSubject.onNext(MenuState.LoadingState(false))
            menuStateSubject.onNext(MenuState.MenuItemInfo(menuListInfo.menus))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    menuStateSubject.onNext(MenuState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }

    fun updateMenuState(request: ProductStateRequest) {
        menuRepository.updateMenuState(request).subscribeWithErrorParsing<ProductsItem, HotBoxError>({
            menuStateSubject.onNext(MenuState.UpdatedProductItemResponse(it))
        }, {
            when (it) {
                is ErrorResult.ErrorMessage -> {
                    menuStateSubject.onNext(MenuState.ErrorMessage(it.errorResponse.safeErrorMessage))
                }
                is ErrorResult.ErrorThrowable -> {
                    Timber.e(it.throwable)
                }
            }
        }).autoDispose()
    }
}

sealed class MenuState {
    data class ErrorMessage(val errorMessage: String) : MenuState()
    data class SuccessMessage(val successMessage: String) : MenuState()
    data class LoadingState(val isLoading: Boolean) : MenuState()
    data class MenuItemInfo(val MenuItemInfo: List<MenusItem>?) : MenuState()
    data class ProductItemInfo(val ProductsItem: List<ProductsItem>?) : MenuState()
    data class UpdatedProductItemResponse(val MenuItemInfo: ProductsItem) : MenuState()

}