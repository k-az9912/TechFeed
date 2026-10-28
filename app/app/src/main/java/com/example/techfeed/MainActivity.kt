package com.example.techfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// 画面を検討するためのサンプル。実際のニュースではありません。
private data class Article(val id: Int, val source: String, val category: String, val title: String, val summary: String, val time: String)
private val samples = listOf(
    Article(1, "Android Developers", "Android", "Compose で始める、これからの Android アプリ開発", "宣言的 UI と画面の状態管理を、実践的な例から見ていきます。", "30分前"),
    Article(2, "Tech Journal", "AI", "生成 AI を活用した開発ツールの今を知る", "日々のコーディングを支える新しいツールと、その使い方を紹介します。", "2時間前"),
    Article(3, "Security Weekly", "セキュリティ", "アプリ開発者が押さえておきたいセキュリティの基本", "認証情報の管理から依存ライブラリの更新まで、基本を振り返ります。", "4時間前"),
    Article(4, "Dev Notes", "開発", "チーム開発を支えるテストと CI の考え方", "変更を安心して届けるためのワークフローを整理します。", "昨日"),
    Article(5, "Android Developers", "Android", "大きな画面にも対応する Android UI の設計", "スマートフォンとタブレットで使いやすいレイアウトを考えます。", "昨日")
)
private val categories = listOf("すべて", "Android", "AI", "セキュリティ", "開発")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme()) { TechFeedScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechFeedScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf("すべて") }
    var savedIds by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var selectedArticleId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedArticle = samples.firstOrNull { it.id == selectedArticleId }
    val visible = samples.filter { (selectedTab == 0 || it.id in savedIds) && (category == "すべて" || it.category == category) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Column { Text("TechFeed", fontWeight = FontWeight.Bold); Text("知りたい技術を、ひとつに。", style = MaterialTheme.typography.labelSmall) } },
                actions = { Text("MOCK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 20.dp)) })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; category = "すべて" }, icon = { Text("▤") }, label = { Text("ニュース") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; category = "すべて" }, icon = { Text("☆") }, label = { Text("保存済み") })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(if (selectedTab == 0) "最新ニュース" else "保存した記事", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp))
            Text(if (selectedTab == 0) "気になる話題をチェック" else "あとで読みたい記事をまとめて確認", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { label -> FilterChip(selected = category == label, onClick = { category = label }, label = { Text(label) }) }
            }
            if (visible.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("記事がありません", style = MaterialTheme.typography.titleMedium)
                    Text(if (selectedTab == 1) "ニュース一覧の「保存」で記事を追加できます。" else "別のカテゴリを選んでください。", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(visible, key = { it.id }) { article ->
                        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { selectedArticleId = article.id }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${article.source}  ·  ${article.time}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(article.category, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                                    TextButton(onClick = { savedIds = if (article.id in savedIds) savedIds - article.id else savedIds + article.id }) { Text(if (article.id in savedIds) "★ 保存済み" else "☆ 保存") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (selectedArticle != null) {
        AlertDialog(onDismissRequest = { selectedArticleId = null }, title = { Text(selectedArticle.title) }, text = { Text("${selectedArticle.source} · ${selectedArticle.time}\n\n${selectedArticle.summary}\n\nこれは画面確認用のサンプル記事です。") }, confirmButton = { TextButton(onClick = { selectedArticleId = null }) { Text("閉じる") } })
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TechFeedPreview() {
    MaterialTheme { TechFeedScreen() }
}
