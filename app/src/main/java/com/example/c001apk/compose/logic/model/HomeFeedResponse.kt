package com.example.c001apk.compose.logic.model


import android.os.Parcelable
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class HomeFeedResponse(
    val status: Int?,
    val error: Int?,
    val message: String?,
    val messageStatus: Int?,
    val data: List<Data>?,
) {

    @Parcelize
    data class Data(
        val isStickTop: Int?,
        val likeUserInfo: UserInfo?,
        val ukey: String?,
        val islast: Int?,
        val isnew: Int?,
        @SerializedName("message_pic")
        val messagePic: String?,
        val messageUid: String?,
        val messageUsername: String?,
        val messageUserAvatar: String?,
        var unreadNum: Int?,
        @SerializedName("is_top")
        var isTop: Int?,
        val shorttitle: String?,
        val packageName: String?,
        @SerializedName("pkg_bit_type")
        val pkgBitType: Int?,
        var localVersionName: String?,
        var localVersionCode: Long?,
        var expand: Boolean = false,
        val forwardid: String?,
        @SerializedName("source_id")
        val sourceId: String?,
        val istag: Int?,
        val likeUsername: String?,
        val likeUid: String?,
        val likeAvatar: String?,
        val fromUserAvatar: String?,
        val fromusername: String?,
        val fromuid: String?,
        val note: String?,
        @SerializedName("extra_key")
        val extraKey: String?,
        val feedUid: String?,
        val fuid: String?,
        val rid: Long?,
        val forwardSourceType: String?,
        val forwardSourceFeed: ForwardSourceFeed?,
        @SerializedName("comment_num")
        val commentNum: String?,
        @SerializedName("fans_num")
        val fansNum: String?,
        @SerializedName("target_id")
        val targetId: String?,
        @SerializedName("target_type")
        val targetType: String?,
        @SerializedName("target_type_title")
        val targetTypeTitle: String?,
        var replyMeRows: List<Data>?,
        @SerializedName("cover_pic")
        val coverPic: String?,
        @SerializedName("is_open")
        val isOpen: Int?,
        @SerializedName("is_open_title")
        val isOpenTitle: String?,
        @SerializedName("item_num")
        val itemNum: String?,
        @SerializedName("follow_num")
        val followNum: String?,
        var description: String?,
        val subTitle: String?,
        val likeTime: Long?,
        @SerializedName("extra_title")
        val extraTitle: String?,
        @SerializedName("extra_url")
        val extraUrl: String?,
        @SerializedName("extra_pic")
        val extraPic: String?,
        val feedTypeName: String?,
        val vote: Vote?,
        @SerializedName("message_cover")
        val messageCover: String?,
        @SerializedName("message_title")
        val messageTitle: String?,
        @SerializedName("message_raw_output")
        val messageRawOutput: String?,
        val relationRows: ArrayList<RelationRows>?,
        val targetRow: TargetRow?,
        @SerializedName("change_count")
        val changeCount: Int?,
        val isModified: Int?,
        @SerializedName("ip_location")
        val ipLocation: String?,
        val isFeedAuthor: Int?,
        var topReplyRows: List<Data>?,
        val extraDataArr: ExtraDataArr?,
        val intro: String?,
        @SerializedName("tag_pics")
        val tagPics: List<String>?,
        val tabList: List<TabList>?,
        val displayUsername: String?,
        val cover: String?,
        val selectedTab: String?,
        val homeTabCardRows: List<HomeTabCardRows>?,
        val configRows: List<ProductConfig>?,
        @SerializedName("be_like_num")
        val beLikeNum: String?,
        val version: String?,
        val apkversionname: String?,
        val apkversioncode: String?,
        val apksize: String?,
        val apkfile: String?,
        val lastupdate: Long?,
        val follow: String?,
        val level: String?,
        val fans: String?,
        val logintime: Long?,
        val experience: String?,
        val regdate: Long?,
        @SerializedName("next_level_experience")
        val nextLevelExperience: String?,
        val bio: String?,
        @JsonAdapter(FeedAdapterFactory::class)
        val feed: Feed?,
        val gender: Int?,
        val city: String?,
        val downnum: String?,
        val downCount: String?,
        val apkname: String?,
        val entityType: String?,
        val type: String?,
        val feedType: String?,
        val fetchType: String?,
        val entityTemplate: String?,
        var entities: MutableList<Entities>?,
        val id: String?,
        val fid: String?,
        val url: String?,
        val uid: String?,
        val ruid: String?,
        val changelog: String?,
        var username: String?,
        val rusername: String?,
        val tpic: String?,
        val message: String?,
        val pic: String?,
        val tags: String?,
        val ttitle: String?,
        var likenum: String?,
        val commentnum: String?,
        val replynum: String?,
        val forwardnum: String?,
        val favnum: String?,
        val dateline: Long?,
        @SerializedName("create_time")
        val createTime: String?,
        @SerializedName("device_title")
        val deviceTitle: String?,
        @SerializedName("device_name")
        val deviceName: String?,
        @SerializedName("recent_reply_ids")
        val recentReplyIds: String?,
        @SerializedName("recent_like_list")
        val recentLikeList: String?,
        val entityId: String?,
        val userAvatar: String?,
        val infoHtml: String?,
        val title: String?,
        val commentStatusText: String?,
        @SerializedName("comment_status")
        val commentStatus: Int?,
        val picArr: List<String>?,
        var replyRows: List<Data>?,
        val replyRowsMore: Int?,
        val logo: String?,
        @SerializedName("hot_num")
        val hotNum: String?,
        @SerializedName("feed_comment_num")
        val feedCommentNum: String?,
        @SerializedName("hot_num_txt")
        val hotNumTxt: String?,
        @SerializedName("feed_comment_num_txt")
        val feedCommentNumTxt: String?,
        @SerializedName("commentnum_txt")
        val commentnumTxt: String?,
        val commentCount: String?,
        @SerializedName("alias_title")
        val aliasTitle: String?,
        val userAction: UserAction?,
        val userInfo: UserInfo?,
        val fUserInfo: UserInfo?,
        var isFollow: Int?,
    ) : Parcelable

    @Parcelize
    data class Feed(
        val id: String? = null,
        val uid: String? = null,
        val username: String? = null,
        val message: String? = null,
        val pic: String? = null,
        val url: String? = null,
    ) : Parcelable

    @Parcelize
    data class ForwardSourceFeed(
        val entityType: String?,
        val feedType: String?,
        val id: String?,
        val username: String?,
        val uid: String?,
        val url: String?,
        val message: String?,
        @SerializedName("message_title")
        val messageTitle: String?,
        val pic: String?,
        val picArr: List<String>?,
    ) : Parcelable

    @Parcelize
    data class Vote(
        val id: String?,
        val type: Int?,
        @SerializedName("start_time")
        val startTime: Long?,
        @SerializedName("end_time")
        val endTime: Long?,
        @SerializedName("total_vote_num")
        val totalVoteNum: Int?,
        @SerializedName("total_comment_num")
        val totalCommentNum: Int?,
        @SerializedName("total_option_num")
        val totalOptionNum: Int?,
        @SerializedName("max_select_num")
        val maxSelectNum: Int?,
        @SerializedName("min_select_num")
        val minSelectNum: Int?,
        @SerializedName("message_title")
        val messageTitle: String?,
        val options: List<Option>?,
    ) : Parcelable

    @Parcelize
    data class Option(
        @SerializedName("total_select_num")
        val totalSelectNum: Long?,
        val id: String?,
        @SerializedName("vote_id")
        val voteId: String?,
        val title: String?,
        val status: Int?,
        val order: Int?,
        val color: String?,
    ) : Parcelable

    @Parcelize
    data class RelationRows(
        val id: String?,
        val logo: String?,
        val title: String?,
        val url: String?,
        val entityType: String?,
    ) : Parcelable

    @Parcelize
    data class TargetRow(
        val id: String?,
        val logo: String?,
        val title: String?,
        val url: String?,
        val entityType: String?,
        val targetType: String?,
    ) : Parcelable

    @Parcelize
    data class ExtraDataArr(
        val pageTitle: String?,
        val cardPageName: String?,
    ) : Parcelable

    @Parcelize
    data class UserInfo(
        val uid: String?,
        val username: String?,
        val level: Int?,
        val logintime: Long?,
        val regdate: String?,
        val entityType: String?,
        val displayUsername: String?,
        val userAvatar: String?,
        val cover: String?,
        val fans: String?,
        val follow: String?,
        val bio: String?,
    ) : Parcelable

    @Parcelize
    data class TabList(
        val title: String?,
        val url: String?,
        @SerializedName("page_name")
        val pageName: String?,
        val entityType: String?,
        val entityId: Int?,
    ) : Parcelable

    @Parcelize
    data class HomeTabCardRows(
        val entityType: String?,
        val entityTemplate: String?,
        val title: String?,
        val url: String?,
        val entities: List<Entities>?,
        val entityId: String?,
    ) : Parcelable

    @Parcelize
    data class ProductConfig(
        val id: String?,
        @SerializedName("product_id")
        val productId: String?,
        val title: String?,
        val price: String?,
        val url: String?,
        @SerializedName("is_add_compare")
        val isAddCompare: Int?,
    ) : Parcelable

    @Parcelize
    data class UserAction(
        var like: Int?,
        @com.google.gson.annotations.SerializedName("favorite") var favorite: Int? = 0,
        var follow: Int? = 0,
        //val collect: Int?,
        //var followAuthor: Int?,
        //val authorFollowYou: Int?
    ) : Parcelable

    data class ReplyRows(
        val id: String?,
        val uid: String?,
        val feedUid: String?,
        val username: String?,
        var message: String?,
        val ruid: String?,
        val rusername: String?,
        val picArr: List<String>?,
        val pic: String?,
        val userInfo: UserInfo?,
    )

    @Parcelize
    data class Entities(
        val uid: String?,
        val userAvatar: String?,
        @SerializedName("device_title")
        val deviceTitle: String?,
        val dateline: String?,
        val username: String?,
        val url: String?,
        val pic: String?,
        val title: String?,
        val message: String?,
        val logo: String?,
        val id: String?,
        val entityType: String?,
        @SerializedName(value = "config_id", alternate = ["product_config_id"])
        val configId: String?,
        @SerializedName("product_id")
        val productId: String?,
        @SerializedName("is_add_compare")
        val isAddCompare: Int?,
        @SerializedName("alias_title")
        val aliasTitle: String?,
        @SerializedName("config_name")
        val configName: String?,
        @SerializedName("price_min")
        val priceMin: String?,
        @SerializedName("price_max")
        val priceMax: String?,
        @SerializedName("price_currency")
        val priceCurrency: String?,
        @SerializedName("product_specs")
        val productSpecs: List<String>?,
        val userInfo: UserInfo?,
    ) : Parcelable

}

