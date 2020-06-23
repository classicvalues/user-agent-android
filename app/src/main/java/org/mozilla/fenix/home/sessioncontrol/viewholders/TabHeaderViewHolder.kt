/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.content.Context
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.tab_header.*
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.ViewHolder
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor

class TabHeaderViewHolder(
    view: View,
    private val interactor: SessionControlInteractor
) : ViewHolder(view) {
    private var isPrivate = false
    private var tabsMenu: TabHeaderMenu

    init {
        tabsMenu = TabHeaderMenu(view.context, isPrivate) {
            when (it) {
                is TabHeaderMenu.Item.Share -> interactor.onShareTabs()
                is TabHeaderMenu.Item.CloseAll -> interactor.onCloseAllTabs(isPrivate)
                is TabHeaderMenu.Item.SaveToCollection -> {
                    interactor.onSaveToCollection(null)
                    view.context.components.analytics.metrics
                        .track(Event.CollectionSaveButtonPressed(TELEMETRY_HOME_MENU_IDENITIFIER))
                }
            }
        }

        share_tabs_button.setOnClickListener {
            interactor.onShareTabs()
        }

        close_tabs_button.setOnClickListener {
            it.context.components.analytics.metrics.track(Event.PrivateBrowsingGarbageIconTapped)
            interactor.onCloseAllTabs(true)
        }

        tabs_overflow_button.run {
            var menu: PopupWindow? = null
            setOnClickListener {
                if (menu == null) {
                    menu = tabsMenu.menuBuilder
                        .build(it.context)
                        .show(
                            anchor = it,
                            orientation = BrowserMenu.Orientation.DOWN,
                            onDismiss = { menu = null }
                        )
                } else {
                    menu?.dismiss()
                }
            }
        }
    }

    fun bind(isPrivate: Boolean, hasTabs: Boolean) {
        this.isPrivate = isPrivate
        tabsMenu.isPrivate = isPrivate

        val headerTextResourceId =
            if (isPrivate) R.string.tabs_header_private_tabs_title else R.string.tab_header_label
        header_text.text = itemView.context.getString(headerTextResourceId)
        share_tabs_button.isInvisible = !isPrivate || !hasTabs
        close_tabs_button.isInvisible = !isPrivate || !hasTabs
        tabs_overflow_button.isVisible = !isPrivate && hasTabs
    }

    class TabHeaderMenu(
        private val context: Context,
        var isPrivate: Boolean,
        private val onItemTapped: (Item) -> Unit = {}
    ) {
        sealed class Item {
            object CloseAll : Item()
            object Share : Item()
            object SaveToCollection : Item()
        }

        val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

        private val menuItems by lazy {
            listOf(
                SimpleBrowserMenuItem(
                    context.getString(R.string.tabs_menu_close_all_tabs)
                ) {
                    onItemTapped.invoke(Item.CloseAll)
                },
                SimpleBrowserMenuItem(
                    context.getString(R.string.tabs_menu_share_tabs)
                ) {
                    onItemTapped.invoke(Item.Share)
                } // ,
                /* Ghostery Begin: removing collections +/
                SimpleBrowserMenuItem(
                    context.getString(R.string.tabs_menu_save_to_collection)
                ) {
                    onItemTapped.invoke(Item.SaveToCollection)
                }.apply { visible = { !isPrivate } }
                /+ Ghostery End */
            )
        }
    }

    companion object {
        const val TELEMETRY_HOME_MENU_IDENITIFIER = "homeMenu"
        const val LAYOUT_ID = R.layout.tab_header
    }
}
