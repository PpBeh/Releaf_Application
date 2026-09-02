package com.example.releaf.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.releaf.ui.viewmodel.AppLanguage
import com.example.releaf.ui.viewmodel.ThemeViewModel

object AppStrings {
    val en = mapOf(
        "app_name" to "Releaf",
        "search_placeholder" to "Search...",
        "activity" to "Activity",
        "garden" to "Garden",
        "profile" to "Profile",
        "map" to "Map",
        "settings" to "Settings",
        "theme" to "Theme",
        "language" to "Additional Language",
        "logout" to "Log out",
        "favourite_toilets" to "Favourite Toilets",
        "nearest_toilet" to "Nearest Toilet",
        "nearest_trash_can" to "Nearest Trash Can",
        "my_location" to "My location",
        "select_language" to "Select Language",
        "points" to "Points",
        "gems" to "Gems",
        "harvest" to "Harvest",
        "growing" to "Growing...",
        "empty_pot" to "Empty Pot",
        "garden_plot" to "My Garden Plot",
        "rewards" to "Rewards",
        "unlocked" to "Unlocked",
        "locked" to "Locked",
        "reward" to "Reward",
        "found_nearest" to "Found nearest",
        "none_found" to "No toilets or trash cans found.",
        "poi_created" to "POI created!",
        "create_failed" to "Failed to create POI",
        "too_close" to "A POI already exists within 5m of this location.",
        "photo_uploaded" to "Photo uploaded!",
        "photo_failed" to "Photo upload failed",
        "poi_removed" to "POI has been removed due to reports."
    )

    val zh = mapOf(
        "app_name" to "Releaf",
        "search_placeholder" to "搜索...",
        "activity" to "活动",
        "garden" to "花园",
        "profile" to "个人资料",
        "map" to "地图",
        "settings" to "设置",
        "theme" to "主题",
        "language" to "附加语言",
        "logout" to "退出登录",
        "favourite_toilets" to "收藏的厕所",
        "nearest_toilet" to "最近的厕所",
        "nearest_trash_can" to "最近的垃圾桶",
        "my_location" to "我的位置",
        "select_language" to "选择语言",
        "points" to "积分",
        "gems" to "宝石",
        "harvest" to "收获",
        "growing" to "生长中...",
        "empty_pot" to "空盆",
        "garden_plot" to "我的花园",
        "rewards" to "奖励",
        "unlocked" to "已解锁",
        "locked" to "未解锁",
        "reward" to "奖励",
        "found_nearest" to "找到最近的",
        "none_found" to "未找到厕所或垃圾桶。",
        "poi_created" to "兴趣点创建成功！",
        "create_failed" to "创建失败",
        "too_close" to "5米内已存在兴趣点。",
        "photo_uploaded" to "照片上传成功！",
        "photo_failed" to "照片上传失败",
        "poi_removed" to "该地点已因举报而被移除。"
    )

    fun get(key: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.ENGLISH -> en[key] ?: key
            AppLanguage.CHINESE -> zh[key] ?: en[key] ?: key
        }
    }
}

@Composable
fun string(key: String, themeViewModel: ThemeViewModel): String {
    val lang by themeViewModel.language.collectAsState()
    return AppStrings.get(key, lang)
}
