package com.boolder.boolder.utils

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<T : ViewBinding>(
    val binder: (View) -> T
) : ReadOnlyProperty<Fragment, T>,
    DefaultLifecycleObserver {

    private var binding: T? = null

    override fun onDestroy(owner: LifecycleOwner) {
        binding = null // Clear reference.
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) {
            return binding
        }

        val lifecycle = thisRef.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            error("Should not attempt to get bindings when Fragment views are destroyed.")
        }

        return binder(thisRef.requireView()).also {
            setValue(thisRef, it)
        }
    }

    private fun setValue(fragment: Fragment, value: T) {
        fragment.lifecycle.removeObserver(this)
        this.binding = value
        fragment.lifecycle.addObserver(this)
    }
}

// Use this to injet view in fragment
fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T) =
    FragmentViewBindingDelegate(viewBindingFactory)

// Use this to inject view in Activity
inline fun <T : ViewBinding> AppCompatActivity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(NONE) {
        bindingInflater.invoke(layoutInflater)
    }
