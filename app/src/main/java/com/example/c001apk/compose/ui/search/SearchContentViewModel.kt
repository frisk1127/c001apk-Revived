package com.example.c001apk.compose.ui.search

import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.ui.base.BaseViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Created by bggRGjQaUbCoE on 2024/6/9
 */
@HiltViewModel(assistedFactory = SearchContentViewModel.ViewModelFactory::class)
class SearchContentViewModel @AssistedInject constructor(
    @Assisted("type") val type: String,
    @Assisted("keyword") val keyword: String,
    @Assisted("pageType") val pageType: String?,
    @Assisted("pageParam") var pageParam: String?,
    @Assisted val searchFeedType: SearchFeedType,
    @Assisted val sortType: SearchOrderType,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
) : BaseViewModel(networkRepo, blackListRepo) {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(
            @Assisted("type") type: String,
            @Assisted("keyword") keyword: String,
            @Assisted("pageType") pageType: String?,
            @Assisted("pageParam") pageParam: String?,
            searchFeedType: SearchFeedType,
            sortType: SearchOrderType,
        ): SearchContentViewModel
    }

    val feedType: String = when (searchFeedType) {
        SearchFeedType.ALL -> "all"
        SearchFeedType.FEED -> "feed"
        SearchFeedType.ARTICLE -> "feedArticle"
        SearchFeedType.COOLPIC -> "picture"
        SearchFeedType.COMMENT -> "comment"
        SearchFeedType.RATING -> "rating"
        SearchFeedType.ANSWER -> "answer"
        SearchFeedType.QUESTION -> "question"
        SearchFeedType.VOTE -> "vote"
    }
    val sort: String = if (type == "feed") sortType.apiValue else "default"

    init {
        fetchData()
    }

    override suspend fun customFetchData() =
        networkRepo.getSearch(
            type, feedType, sort, keyword,
            pageType, pageParam, page, lastItem
        )

    override fun handleLoadMore(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data> {
        return response.distinctBy { it.entityId }
    }

}
