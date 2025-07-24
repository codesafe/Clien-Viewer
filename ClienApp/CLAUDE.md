# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ClienApp is an Android application that provides a custom UI for browsing m.clien.net content without using WebView. It fetches and parses HTML content to display in a native Android interface using Jetpack Compose.

## Build Commands

```bash
# Build the app
./gradlew build

# Install debug build on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Clean build artifacts
./gradlew clean

# Generate signed APK
./gradlew assembleRelease
```

## Architecture

### Core Components

1. **MainActivity.kt** - Entry point containing:
   - Navigation setup with Compose Navigation
   - Three main screens: BoardListScreen, BoardDetailScreen, PostDetailScreen
   - ClienRepository class for data fetching
   - Custom animations for navigation transitions

2. **Data Flow**:
   - Hardcoded board list → Fetch posts from board URL → Fetch post details
   - HTML parsing using JSoup to extract content
   - Caching layer with 5-minute TTL
   - Pull-to-refresh and infinite scroll implemented

3. **Key Features**:
   - SSL certificate validation bypass for m.clien.net
   - Swipe-to-go-back gesture support (swipe from anywhere)
   - Image display in post content
   - YouTube video thumbnail preview with external playback
   - First 2 posts (notices) filtered from board listings on first page only
   - Visited posts tracking with visual indication
   - Comment parsing and display
   - Clickable links in content

4. **Additional Components**:
   - **VisitedPostsManager.kt**: Tracks visited posts using SharedPreferences
   - **LinkifyText.kt**: Handles URL detection and YouTube preview in text
   - **SwipeGesture.kt**: Custom swipe gesture detector for back navigation

### Important Implementation Details

- **URL Encoding**: Uses Base64 encoding to handle Korean characters and special characters in navigation parameters (see UrlUtils.kt)
- **Network Security**: Custom SSL handling in SSLHelper.kt - development only, not for production
- **Caching**: ConcurrentHashMap-based cache in CacheManager.kt
- **HTML Parsing**: 
  - Title: `span[data-role='list-title-text']` attribute or text
  - Comments: `span.nickname` for author, hidden input value for content
  - YouTube: iframe src parsing for embed URLs
- **Pagination**: First page has no `po` param, subsequent pages use `po=1`, `po=2`, etc.
- **UI Customization**:
  - Visited posts: 60% opacity on entire card
  - Like count: Red rounded badge before title
  - Comment replies: Indented with 24dp padding

## Key Dependencies

- Jetpack Compose with Material3
- Retrofit2 + OkHttp3 (though mainly using JSoup for actual requests)
- JSoup for HTML parsing
- Coil for image loading
- Navigation Compose for screen navigation
- Kotlin Coroutines for async operations
- Google Accompanist for SwipeRefresh

## Common Issues & Solutions

1. **SSL Certificate Errors**: Already handled by SSLHelper.getUnsafeOkHttpClient()
2. **URL Encoding Issues**: Use UrlUtils.encodeUrl/decodeUrl instead of URLEncoder/URLDecoder
3. **Swipe Gesture**: Swipe right (> 100px) triggers back navigation
4. **YouTube WebView Issues**: Replaced with thumbnail preview + external intent

## UI Specifications

### Font Sizes
- Board list title: 18sp
- Board list description: 15sp
- Post list title: 12sp
- Post list metadata: 10sp
- Post content: 16sp
- Comment content: 14sp

### Spacing
- Board list cards: 8dp padding, 4dp spacing
- Post list cards: 8dp padding, 4dp spacing
- Comments: 4dp vertical padding

## Development Notes

- Target SDK: 34 (Android 14)
- Min SDK: 24 (Android 7.0)
- Uses Kotlin 1.9.20 with Java 17
- Network security config allows cleartext traffic
- ProGuard rules configured for JSoup and Retrofit