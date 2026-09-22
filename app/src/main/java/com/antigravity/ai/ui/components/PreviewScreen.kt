package com.antigravity.ai.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.antigravity.ai.data.api.InspectorPayload
import com.antigravity.ai.data.api.PREVIEW_ORIGIN
import com.antigravity.ai.data.api.parseInspectorMessage
import com.antigravity.ai.ui.viewmodel.ChatViewModel

fun stableSelectorScript(): String = """
(() => { if (window.__previewInspector) return; window.__previewInspector = e => {
e.preventDefault(); e.stopPropagation(); const n=e.target; let p=[],x=n;
while(x&&x.nodeType===1&&x!==document.body){let q=x.tagName.toLowerCase();if(x.id){const esc=window.CSS&&CSS.escape?CSS.escape(x.id):x.id.replace(/[^a-zA-Z0-9_-]/g,'\\\\$&');q+='#'+esc;p.unshift(q);break}let i=1,s=x;while((s=s.previousElementSibling))if(s.tagName===x.tagName)i++;p.unshift(q+':nth-of-type('+i+')');x=x.parentElement}
const b=n.getBoundingClientRect(),c=getComputedStyle(n); n.style.outline='2px solid #ff3b30';
try { if (window.previewChannel) previewChannel.postMessage(JSON.stringify({selector:p.join('>'),outerHTML:n.outerHTML.slice(0,8192),bounds:JSON.stringify({x:b.x,y:b.y,w:b.width,h:b.height}),styles:JSON.stringify({color:c.color,backgroundColor:c.backgroundColor,fontSize:c.fontSize,fontWeight:c.fontWeight,display:c.display,margin:c.margin,padding:c.padding})})); } finally { document.removeEventListener('click',window.__previewInspector,true); window.__previewInspector=null; } }; document.addEventListener('click',window.__previewInspector,true); })()
""".trimIndent()

@Composable
fun PreviewScreen(url: String, sourcePath: String, onClose: () -> Unit, viewModel: ChatViewModel, onScreenshot: ((Bitmap) -> Unit)? = null) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<InspectorPayload?>(null) }
    var instruction by remember { mutableStateOf("") }
    var canInspect by remember { mutableStateOf(false) }
    BackHandler { onClose() }
    DisposableEffect(Unit) { onDispose { webView?.stopLoading(); webView?.destroy(); webView = null } }
    fun screenshot(): Bitmap? = webView?.takeIf { it.width > 0 && it.height > 0 }?.let { view -> Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) } }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "Geri") }
            IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, "Yenile") }
            Text(url.removePrefix("http://").removePrefix("https://"), maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
            IconButton(enabled = canInspect, onClick = { webView?.evaluateJavascript(stableSelectorScript(), null) }) { Icon(Icons.Default.Search, "Inspect") }
            IconButton(onClick = { screenshot()?.let { onScreenshot?.invoke(it) } ?: viewModel.setErrorMessage("Preview ekran görüntüsü alınamadı: WebView henüz hazır değil") }) { Icon(Icons.Default.Edit, "Ekran görüntüsü al / İşaretle") }
        }
        AndroidView(Modifier.weight(1f), factory = { context -> WebView(context).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; webView = this
            canInspect = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
            if (canInspect) WebViewCompat.addWebMessageListener(this, "previewChannel", setOf(PREVIEW_ORIGIN)) { _, message, _, _, _ -> parseInspectorMessage(message.data.orEmpty())?.let { selected = it } }
            loadUrl(url); loadedUrl = url
        } }, update = { view -> if (loadedUrl != url) { loadedUrl = url; view.loadUrl(url) } })
        selected?.let { item ->
            Text("${item.selector}\n${item.styles}", maxLines = 3, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            OutlinedTextField(instruction, { instruction = it }, Modifier.fillMaxWidth().padding(8.dp), maxLines = 2, label = { Text("Element talimatı") })
            Button(onClick = { screenshot()?.let { shot -> viewModel.sendPreviewInspection(shot, sourcePath, item.selector, item.outerHTML, item.bounds, item.styles, instruction); selected = null; instruction = "" } ?: viewModel.setErrorMessage("Preview ekran görüntüsü alınamadı: WebView henüz hazır değil") }, enabled = instruction.isNotBlank() && webView?.let { it.width > 0 && it.height > 0 } == true, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text("Seçimi sohbete gönder") }
        }
    }
}
