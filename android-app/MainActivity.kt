package com.example.clienapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MenuItem(
    val title: String,
    val url: String,
    val description: String = ""
)

interface ClienApiService {
    @Headers(
        "User-Agent: Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36"
    )
    @GET("/")
    suspend fun getHomePage(): String
}

class ClienRepository {
    suspend fun fetchMenuItems(): List<MenuItem> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect("https://m.clien.net")
                .userAgent("Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36")
                .get()
            
            val menuItems = mutableListOf<MenuItem>()
            
            // 메인 메뉴 파싱
            doc.select("nav a, .menu-item a").forEach { element ->
                val title = element.text()
                val url = element.attr("href")
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    menuItems.add(MenuItem(
                        title = title,
                        url = if (url.startsWith("http")) url else "https://m.clien.net$url"
                    ))
                }
            }
            
            menuItems
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

@Composable
fun ClienApp() {
    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val repository = remember { ClienRepository() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            menuItems = repository.fetchMenuItems()
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clien Custom App") }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems) { item ->
                    MenuItemCard(item)
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(item: MenuItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall
            )
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ClienApp()
            }
        }
    }
}