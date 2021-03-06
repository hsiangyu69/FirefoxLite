package org.mozilla.rocket.home.contenthub.data

import android.content.Context
import androidx.lifecycle.LiveData
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.GAMES
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.JSON_KEY_ID
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.JSON_KEY_ITEMS
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.JSON_KEY_TYPE
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.NEWS
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.SHOPPING
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo.Companion.TRAVEL
import org.mozilla.rocket.preference.booleanLiveData
import org.mozilla.rocket.preference.stringLiveData
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.getJsonArray
import org.mozilla.rocket.util.toJsonArray
import org.mozilla.strictmodeviolator.StrictModeViolation

class ContentHubRepo(private val appContext: Context) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    fun isContentHubEnabled(): LiveData<Boolean> =
            preference.booleanLiveData(SHARED_PREF_KEY_ENABLE_CONTENT_HUB, true)

    fun setContentHubEnabled(enabled: Boolean) {
        preference.edit().putBoolean(SHARED_PREF_KEY_ENABLE_CONTENT_HUB, enabled).apply()
    }

    fun getConfiguredContentHubItemGroupsLive(): LiveData<List<ContentHubItemGroup>?> {
        val readTypes = getReadTypesLive()
        val contentHubItemsJsonStr = FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_CONTENT_HUB_ITEMS_V2_5)
                .takeIf { it.isNotEmpty() }
        return readTypes.map {
            contentHubItemsJsonStr?.jsonStringToContentHubItemGroups(it)
        }
    }

    fun getDefaultContentHubItems(resId: Int): List<ContentHubItem>? =
            AssetsUtils.loadStringFromRawResource(appContext, resId)
                    ?.jsonStringToContentHubItems(readTypes = getReadTypes())

    private fun getReadTypes(): List<Int> =
            preference.getString(SHARED_PREF_KEY_READ_CONTENT_HUB, "")?.jsonStringToTypeList() ?: emptyList()

    private fun getReadTypesLive(): LiveData<List<Int>> =
            preference.stringLiveData(SHARED_PREF_KEY_READ_CONTENT_HUB, "")
                    .map { it.jsonStringToTypeList() ?: emptyList() }

    fun addReadType(type: Int) {
        setReadTypes(
            getReadTypes().toMutableSet().apply { add(type) }
        )
    }

    private fun setReadTypes(readTypes: Collection<Int>) {
        val jsonArray = JSONArray().apply {
            readTypes.map { type ->
                JSONObject().apply {
                    put(JSON_KEY_TYPE, type)
                }
            }.forEach { jsonObject -> put(jsonObject) }
        }
        preference.edit()
                .putString(SHARED_PREF_KEY_READ_CONTENT_HUB, jsonArray.toString())
                .apply()
    }

    companion object {
        const val TRAVEL = 1
        const val SHOPPING = 2
        const val NEWS = 3
        const val GAMES = 4

        private const val PREF_NAME = "content_hub"
        private const val SHARED_PREF_KEY_READ_CONTENT_HUB = "shared_pref_key_read_content_hub"
        private const val SHARED_PREF_KEY_ENABLE_CONTENT_HUB = "shared_pref_key_enable_content_hub"
        const val JSON_KEY_TYPE = "type"
        const val JSON_KEY_ID = "id"
        const val JSON_KEY_ITEMS = "items"
    }
}

private fun String.jsonStringToTypeList(): List<Int>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> jsonObject.getInt(JSON_KEY_TYPE) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun String.jsonStringToContentHubItemGroups(readTypes: List<Int>): List<ContentHubItemGroup>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .mapNotNull { jsonObject -> jsonObject.toContentHubItemGroup(readTypes) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun JSONObject.toContentHubItemGroup(readTypes: List<Int>): ContentHubItemGroup? {
    return try {
        val id = this.getInt(JSON_KEY_ID)
        val items = this.getJsonArray(JSON_KEY_ITEMS) { jsonObject ->
            val type = jsonObject.getInt(JSON_KEY_TYPE)
            val isUnread = !readTypes.contains(type)
            createContentHubItem(type, isUnread)
        }
        ContentHubItemGroup(id, items)
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

private fun String.jsonStringToContentHubItems(readTypes: List<Int>): List<ContentHubItem>? {
    return try {
        val jsonArray = this.toJsonArray()
        (0 until jsonArray.length())
                .map { index -> jsonArray.getJSONObject(index) }
                .map { jsonObject -> jsonObject.getInt(JSON_KEY_TYPE) }
                .map { type ->
                    val isUnread = !readTypes.contains(type)
                    createContentHubItem(type, isUnread)
                }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}

sealed class ContentHubItem(val iconResId: Int, val textResId: Int, open var isUnread: Boolean) {
    class Travel(override var isUnread: Boolean) : ContentHubItem(R.drawable.ic_travel, R.string.travel_vertical_title, isUnread)
    class Shopping(override var isUnread: Boolean) : ContentHubItem(R.drawable.ic_shopping, R.string.shopping_vertical_title, isUnread)
    class News(override var isUnread: Boolean) : ContentHubItem(R.drawable.ic_news, R.string.label_menu_news, isUnread)
    class Games(override var isUnread: Boolean) : ContentHubItem(R.drawable.ic_games, R.string.gaming_vertical_title, isUnread)
}

data class ContentHubItemGroup(
    val groupId: Int,
    val items: List<ContentHubItem>
)

private fun createContentHubItem(type: Int, isUnread: Boolean): ContentHubItem {
    return when (type) {
        TRAVEL -> ContentHubItem.Travel(isUnread)
        SHOPPING -> ContentHubItem.Shopping(isUnread)
        NEWS -> ContentHubItem.News(isUnread)
        GAMES -> ContentHubItem.Games(isUnread)
        else -> error("Unsupported content hub item type $type")
    }
}