package com.nct.xmusicstation.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import com.nct.xmusicstation.R
import com.nct.xmusicstation.binding.FragmentDataBindingComponent
import com.nct.xmusicstation.databinding.LoginFragmentBinding
import com.nct.xmusicstation.event.CodeEvent
import com.nct.xmusicstation.event.ErrorEvent
import com.nct.xmusicstation.event.FragmentEvent
import com.nct.xmusicstation.event.ProgressTimerEvent
import com.nct.xmusicstation.ui.base.BaseDataEventFragment
import com.nct.xmusicstation.ui.player.PlayerFragment
import com.orhanobut.logger.Logger
import com.toan_itc.core.architecture.autoCleared
import com.toan_itc.core.richutils._switchFragment
import com.toan_itc.core.richutils.hideKeyboard
import com.toan_itc.core.richutils.showKeyboard
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by Toan.IT on 12/10/18.
 * Email:Huynhvantoan.itc@gmail.com
 */


@Suppress("unused", "UNUSED_PARAMETER")
class LoginFragment : BaseDataEventFragment<LoginViewModel>(), LoginView {
    private var binding by autoCleared<LoginFragmentBinding>()
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)

    companion object {
        fun newInstance() = LoginFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val dataBinding = DataBindingUtil.inflate<LoginFragmentBinding>(
            inflater,
            setLayoutResourceID(),
            container,
            false,
            dataBindingComponent
        )
        binding = dataBinding
        return dataBinding.root
    }

    override fun getViewModel(): Class<LoginViewModel> = LoginViewModel::class.java

    override fun setLayoutResourceID(): Int = R.layout.login_fragment

    override fun initView() {
        with(binding) {
            loginBtnRenewCode.setOnClickListener { onRenewCodeClicked() }
            etEmail.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.showKeyboard()
                }
            }
            etPassword.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.showKeyboard()
                }
            }
            btnLogin.setOnClickListener {
                checkLogin()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {

    }

    override fun onRenewCodeClicked() {
        viewModel.renewCodeClick()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProgressTimerEvent(progressTimerEvent: ProgressTimerEvent) {
        progressTimerEvent.apply {
            with(binding) {
                loginBtnRenewCode.isEnabled = true
                loginBtnRenewCode.text = timeInterver
                loginBtnRenewCode.progress = progress
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFragmentEvent(fragmentEvent: FragmentEvent) {
        view?.hideKeyboard()
        activity?._switchFragment(null, PlayerFragment.newInstance(), R.id.container)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onErrorEvent(errorEvent: ErrorEvent) {
        errorEvent.errorMessage?.let {
            showSnackBar(it)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCodeEvent(codeEvent: CodeEvent) {
        codeEvent.code?.let {
            binding.loginCode.text = it
        }
    }

    private fun checkLogin() {
        with(binding) {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            var cancel = false
            var focusView: View? = null
            if (password.isEmpty()) {
                if (txtError.isGone)
                    txtError.isVisible = true
                imgEmailError.isGone = true
                imgPassError.isVisible = true
                txtError.text = getString(R.string.error_invalid_password)
                focusView = etPassword
                cancel = true
            } else if (password.length < 3) {
                if (txtError.isGone)
                    txtError.isVisible = true
                imgEmailError.isGone = true
                imgPassError.isVisible = true
                txtError.text = getString(R.string.error_incorrect_password)
                focusView = etPassword
                cancel = true
            }
            if (email.isEmpty()) {
                if (txtError.isGone)
                    txtError.isVisible = true
                imgPassError.isGone = true
                imgEmailError.isVisible = true
                txtError.text = getString(R.string.error_field_required)
                focusView = etEmail
                cancel = true
            }
            if (cancel) {
                focusView?.requestFocus()
            } else {
                txtError.isGone = true
                imgEmailError.isGone = true
                imgPassError.isGone = true
                this@LoginFragment.viewModel.login(email, password)
            }
        }
    }

    override fun onDestroyView() {
        Logger.v("onDestroyView:" + this.javaClass.simpleName)
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

}
