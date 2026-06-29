package com.devpro.pizzatime.feature.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentSplashBinding
import com.devpro.pizzatime.feature.welcome.WelcomeFragment

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private val openWelcomeRunnable = Runnable {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, WelcomeFragment())
            .commitAllowingStateLoss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handler.postDelayed(openWelcomeRunnable, SPLASH_DELAY_MS)
    }

    override fun onDestroyView() {
        handler.removeCallbacks(openWelcomeRunnable)
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1800L
    }
}