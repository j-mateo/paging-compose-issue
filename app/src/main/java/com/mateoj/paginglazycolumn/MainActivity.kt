package com.mateoj.paginglazycolumn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.mateoj.paginglazycolumn.ui.theme.PagingLazyColumnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // https://developer.android.com/reference/kotlin/androidx/paging/compose/package-summary#collectaslazypagingitems
            PagingLazyColumnTheme {

                val myBackend = remember { MyBackend() }

                val pager = remember {
                    Pager(
                        config = PagingConfig(
                            pageSize = MyBackend.DataBatchSize,
                            enablePlaceholders = true,
                            maxSize = 200
                        ),
                        initialKey = 20
                    ) { myBackend.getAllData() }
                }

                val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

                LazyColumn(state = rememberLazyListState()) {
                    if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                        item {
                            Text(
                                text = "Waiting for items to load from the backend",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    itemsIndexed(lazyPagingItems) { index, item ->
                        Text("Index=$index: $item", fontSize = 20.sp)
                    }

                    if (lazyPagingItems.loadState.append == LoadState.Loading) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

            }
        }
    }
}

class MyBackend {
    // https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data#pagingsource
    fun getAllData(): PagingSource<Int, String> = object : PagingSource<Int, String>() {
        override fun getRefreshKey(state: PagingState<Int, String>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            return try {
                // Start refresh at page 1 if undefined.
                val nextPageNumber = params.key ?: 1
                val response = searchUsers(nextPageNumber)
                LoadResult.Page(
                    data = response.users,
//                    prevKey = null, // Only paging forward.
                    prevKey = response.prevPageNumber, // Paging backwards
                    nextKey = response.nextPageNumber
                )
            } catch (e: Exception) {
                // Handle errors in this block and return LoadResult.Error if it is an
                // expected error (such as a network failure).
                LoadResult.Error(e)
            }
        }

    }

    private fun searchUsers(pageNumber: Int): SearchResponse {
        val offset = pageNumber.dec() * DataBatchSize
        val users =
            (offset until offset.plus(DataBatchSize)).map { "User $it (Page $pageNumber)" }

        return SearchResponse(
            users = users,
            nextPageNumber = pageNumber.inc(),
            prevPageNumber = if (pageNumber <= 1) null else pageNumber.dec()
        )
    }

    companion object {
        const val DataBatchSize = 20
    }
}

data class SearchResponse(
    val nextPageNumber: Int,
    val users: List<String>,
    val prevPageNumber: Int?
)
