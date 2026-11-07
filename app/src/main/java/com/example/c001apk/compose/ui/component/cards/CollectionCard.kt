package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.Alignment
import com.example.c001apk.compose.ui.theme.isMiuix
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.c001apk.compose.ui.component.design.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.c001apk.compose.ui.component.design.contentCard
import com.example.c001apk.compose.ui.component.design.contentPress
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.ffflist.FFFListType

/**
 * Created by bggRGjQaUbCoE on 2024/6/15
 */
@Composable
fun CollectionCard(
    modifier: Modifier = Modifier,
    data: HomeFeedResponse.Data,
    onViewFFFList: ((String?, String, String?, String?) -> Unit)? = null,
    onDeleteCollection: ((HomeFeedResponse.Data) -> Unit)? = null,
) {
    val prefs = LocalUserPreferences.current
    val canDelete = onDeleteCollection != null && prefs.isLogin &&
        data.id?.toLongOrNull()?.let { it > 0 } == true &&
        (data.uid.isNullOrBlank() || data.uid == prefs.uid)
    val openCollection = rememberHapticClick {
        onViewFFFList?.invoke(null, FFFListType.COLLECTION_ITEM.name, data.id.orEmpty(), data.title.orEmpty())
    }
    val deleteCollection = rememberHapticClick(type = HapticFeedbackType.LongPress) {
        onDeleteCollection?.invoke(data)
    }
    if (!isMiuix()) {
        androidx.compose.material3.Surface(modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.combinedClickable(onClick = openCollection,
                onLongClick = if (canDelete) deleteCollection else null,
                onLongClickLabel = if (canDelete) "删除收藏夹" else null, hapticFeedbackEnabled = false).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!data.coverPic.isNullOrBlank()) CoilLoader(url = data.coverPic,
                    modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium))
                else androidx.compose.material3.Surface(Modifier.size(64.dp), shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Outlined.Folder, null, Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(data.title?.takeIf(String::isNotBlank) ?: "默认收藏夹", style = MaterialTheme.typography.titleMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(data.isOpenTitle?.takeIf(String::isNotBlank),
                        data.itemNum?.let { "$it 项内容" }, data.followNum?.takeIf { it != "0" }?.let { "$it 人关注" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .contentPress()
            .contentCard()
            .combinedClickable(onClick = openCollection,
                onLongClick = if (canDelete) deleteCollection else null,
                onLongClickLabel = if (canDelete) "删除收藏夹" else null,
                hapticFeedbackEnabled = false)
            .padding(10.dp)
    ) {
        val (cover, name, type, follow, size) = createRefs()

        CoilLoader(
            url = data.coverPic,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .aspectRatio(4f / 3f)
                .constrainAs(cover) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                })

        Text(
            text = data.title.orEmpty(),
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(name) {
                    start.linkTo(cover.end)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = data.isOpenTitle.orEmpty(),
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(type) {
                    start.linkTo(cover.end)
                    top.linkTo(name.bottom)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = data.followNum.toString() + "人关注",
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(follow) {
                    start.linkTo(type.end)
                    top.linkTo(name.bottom)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = data.itemNum.toString() + "个内容",
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(size) {
                    start.linkTo(follow.end)
                    top.linkTo(name.bottom)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

    }
}
