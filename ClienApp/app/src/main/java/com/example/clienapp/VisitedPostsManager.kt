import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf // Add this import

object VisitedPostsManager {
    private const val PREF_NAME = "visited_posts"
    private const val KEY_VISITED_POSTS = "visited_post_urls"
    private lateinit var sharedPreferences: SharedPreferences
    private val _visitedPosts = mutableStateListOf<String>() // Change to mutableStateListOf
    val visitedPosts: List<String> get() = _visitedPosts // Expose as immutable list

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadVisitedPosts()
    }

    private fun loadVisitedPosts() {
        val savedPosts = sharedPreferences.getStringSet(KEY_VISITED_POSTS, emptySet()) ?: emptySet()
        _visitedPosts.clear()
        _visitedPosts.addAll(savedPosts)
    }

    fun markAsVisited(postUrl: String) {
        if (!_visitedPosts.contains(postUrl)) { // Only add if not already present
            _visitedPosts.add(postUrl)
            saveVisitedPosts()
        }
    }

    fun isVisited(postUrl: String): Boolean {
        return _visitedPosts.contains(postUrl)
    }

    private fun saveVisitedPosts() {
        sharedPreferences.edit().putStringSet(KEY_VISITED_POSTS, _visitedPosts.toSet()).apply()
    }

    fun clearAll() {
        _visitedPosts.clear()
        saveVisitedPosts()
    }
}