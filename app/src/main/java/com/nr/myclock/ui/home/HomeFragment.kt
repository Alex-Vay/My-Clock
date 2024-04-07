package com.nr.myclock.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nr.myclock.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val root: View = binding.root
//        Кнопка для добавления будильника
        val addAlarm: Button = binding.alarmButton
//      не получается добавить фрагмент(alarmsettingsfragment.xml) который будет отображаться после нажатия кнопки
//        addAlarm.setOnClickListener {
//            // Создать и открыть новый фрагмент для настройки будильника
//            val newFragment = AlarmSettingsFragment() // здесь AlarmSettingsFragment - это ваш новый фрагмент для настройки будильника
//            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, newFragment).commit()
//
//        }

        val textView: TextView = binding.alarmButton
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it

        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}