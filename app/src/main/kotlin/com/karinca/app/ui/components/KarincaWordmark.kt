package com.karinca.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karinca.app.R

// The app's small brand header — mascot on the left, "karınca" wordmark on the right —
// shared by every screen's top bar so it can't drift into a per-screen variant (e.g. the
// mascot sitting inside a colored circle on one screen but not another).
@Composable
fun KarincaWordmark(modifier: Modifier = Modifier, logoSize: Dp = 32.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Image(painterResource(R.drawable.ic_karinca_logo), contentDescription = null, modifier = Modifier.size(logoSize))
        Spacer(Modifier.width(8.dp))
        Text("karınca", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
    }
}

// The full top bar every main screen shares: KarincaWordmark on the left, the profile
// icon aligned on the right — one call instead of every screen re-declaring the same
// TopAppBar(title = { KarincaWordmark() }, actions = { ... AccountCircle ... }).
// onProfileClick null means "already on Profilim" — the icon still shows (so the bar
// looks consistent everywhere) but isn't itself clickable.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarincaTopBar(onProfileClick: (() -> Unit)? = null) {
    TopAppBar(
        title = { KarincaWordmark() },
        actions = {
            if (onProfileClick != null) {
                IconButton(onClick = onProfileClick) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Profilim",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).padding(end = 4.dp),
                )
            }
        },
    )
}
