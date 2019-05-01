package org.mozilla.rocket.content.view.ecommerce

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.banner.BannerAdapter
import org.mozilla.banner.BannerConfigViewModel
import org.mozilla.banner.TelemetryListener
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.content.ContentPortalListener
import org.mozilla.rocket.content.CouponAdapter
import org.mozilla.rocket.content.ShoppingLinkAdapter
import org.mozilla.rocket.content.TYPE_COUPON
import org.mozilla.rocket.content.TYPE_KEY
import org.mozilla.rocket.content.TYPE_TICKET
import org.mozilla.rocket.content.data.ShoppingLink

/**
 * Fragment that display the content for [ShoppingLink]s and [Coupon]s
 *
 */
class EcFragment : Fragment(), ContentPortalListener {

    companion object {
        fun newInstance(feature: Int): EcFragment {
            val args = Bundle().apply {
                putInt(TYPE_KEY, feature)
            }
            return EcFragment().apply { arguments = args }
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)

    }

    override fun onStatus(items: MutableList<out NewsItem>?) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        when (arguments?.getInt(TYPE_KEY)) {
            TYPE_COUPON -> {
                return inflater.inflate(R.layout.content_tab_coupon, container, false).apply {
                    val recyclerView = findViewById<RecyclerView>(R.id.content_coupons)
                    val couponAdapter = CouponAdapter(this@EcFragment)
                    recyclerView?.layoutManager = LinearLayoutManager(context)
                    recyclerView?.adapter = couponAdapter

                    val coupons = AppConfigWrapper.getEcommerceCoupons()
                    couponAdapter.submitList(coupons)

                    setupCouponBanner(this)
                }
            }
            TYPE_TICKET -> {
                return inflater.inflate(R.layout.content_tab_shoppinglink, container, false).apply {

                    val recyclerView = findViewById<RecyclerView>(R.id.ct_shoppinglink_list)
                    val shoppinglinkAdapter = ShoppingLinkAdapter(this@EcFragment)
                    recyclerView?.layoutManager = GridLayoutManager(context, 2)
                    recyclerView?.adapter = shoppinglinkAdapter

                    val ecommerceShoppingLinks = AppConfigWrapper.getEcommerceShoppingLinks()
                    ecommerceShoppingLinks.add(ShoppingLink("", "footer", "", ""))
                    shoppinglinkAdapter.submitList(ecommerceShoppingLinks)
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setupCouponBanner(view: View) {
        val banner = view.findViewById<RecyclerView>(R.id.content_banners)
        banner.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(banner)

        val vm = ViewModelProviders.of(activity!!).get(BannerConfigViewModel::class.java)
        vm.couponConfig.observe(viewLifecycleOwner, Observer {it?.apply {

            var bannerInnerTelemetryListener: TelemetryListener =
                object : TelemetryListener {
                    override fun sendClickItemTelemetry(
                        jsonString: String,
                        itemPosition: Int
                    ) {
                    }

                    override fun sendClickBackgroundTelemetry(jsonString: String) {
                        val jsonObject: JSONObject
                        try {
                            jsonObject = JSONObject(jsonString)
                            var pos: String? = jsonObject.optString("pos")
                            if (pos == null) {
                                pos = "-1"
                            }
                            val feed = jsonObject.optString("feed")
                            val id = jsonObject.optString("id")
                            val source = jsonObject.optString("source")
                            val category = jsonObject.optString("category")
                            val subCategory = jsonObject.optString("sub_category")

                            TelemetryWrapper.clickOnPromoItem(
                                pos,
                                id,
                                feed,
                                source,
                                category,
                                subCategory
                            )
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            val bannerAdapter = BannerAdapter(
                this,
                { arg ->
                    FragmentListener.notifyParent(
                        this@EcFragment,
                        FragmentListener.TYPE.OPEN_URL_IN_NEW_TAB,
                        arg
                    )
                },
                bannerInnerTelemetryListener
            )
            banner?.adapter = bannerAdapter
            banner?.visibility = View.VISIBLE
        }

        })
    }
}