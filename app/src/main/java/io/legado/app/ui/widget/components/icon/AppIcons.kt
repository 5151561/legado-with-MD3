package io.legado.app.ui.widget.components.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.legado.app.ui.main.MainDestination

object AppIcons {

    val Search: ImageVector
        @Composable
        get() = Icons.Default.Search

    val MoreVert: ImageVector
        @Composable
        get() = Icons.Default.MoreVert

    val Edit: ImageVector
        @Composable
        get() = Icons.Default.Edit

    val Delete: ImageVector
        @Composable
        get() = Icons.Default.Delete

    val Close: ImageVector
        @Composable
        get() = Icons.Default.Clear

    val Back: ImageVector
        @Composable
        get() = Icons.AutoMirrored.Filled.ArrowBack

    val Filter: ImageVector
        @Composable
        get() = Icons.Default.FilterList

    val Settings: ImageVector
        @Composable
        get() = Icons.Default.Settings

    val BugReport: ImageVector
        @Composable
        get() = Icons.Default.BugReport

    val PrecisionSearch: ImageVector
        @Composable
        get() = Icons.Default.MyLocation

    val UnPrecisionSearch: ImageVector
        @Composable
        get() = Icons.Default.LocationSearching

    val History: ImageVector
        @Composable
        get() = Icons.Default.History

    val Replay: ImageVector
        @Composable
        get() = Icons.Default.Replay

    val MoreCircle: ImageVector
        @Composable
        get() = Icons.Default.MoreHoriz

    val Check: ImageVector
        @Composable
        get() = Icons.Default.Check

    val Group: ImageVector
        @Composable
        get() = Icons.Outlined.Sell



    @Composable
    fun mainDestination(destination: MainDestination, selected: Boolean): ImageVector {
        return when (destination) {
            MainDestination.Home -> if (selected) Icons.Default.Home else Icons.Outlined.Home
            MainDestination.Bookshelf -> if (selected) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Outlined.LibraryBooks
            MainDestination.Explore -> if (selected) Icons.Default.Explore else Icons.Outlined.Explore
            MainDestination.Rss -> if (selected) Icons.Default.RssFeed else Icons.Outlined.RssFeed
            MainDestination.My -> if (selected) Icons.Default.Person else Icons.Outlined.Person
        }
    }
}
