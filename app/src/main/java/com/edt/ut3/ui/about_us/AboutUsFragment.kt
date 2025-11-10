package com.edt.ut3.ui.about_us

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import com.edt.ut3.R


class AboutUsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AboutUsScreen()
                }
            }
        }
    }
}

@Composable
fun AboutUsScreen() {
    val aboutUsText = stringResource(id = R.string.about_us_text)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            // Use AndroidView to wrap the TextView with autoLink functionality
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        text = aboutUsText
                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        autoLinkMask = android.text.util.Linkify.EMAIL_ADDRESSES or
                                      android.text.util.Linkify.WEB_URLS
                        LinkifyCompat.addLinks(this, android.text.util.Linkify.ALL)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutUsScreenPreview() {
    MaterialTheme {
        AboutUsScreen()
    }
}