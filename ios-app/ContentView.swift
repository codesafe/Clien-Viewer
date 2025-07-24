import SwiftUI
import SwiftSoup

struct MenuItem: Identifiable {
    let id = UUID()
    let title: String
    let url: String
    let description: String
}

class ClienViewModel: ObservableObject {
    @Published var menuItems: [MenuItem] = []
    @Published var isLoading = false
    
    func fetchMenuItems() {
        isLoading = true
        
        Task {
            do {
                let url = URL(string: "https://m.clien.net")!
                var request = URLRequest(url: url)
                request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15", forHTTPHeaderField: "User-Agent")
                
                let (data, _) = try await URLSession.shared.data(for: request)
                let html = String(data: data, encoding: .utf8) ?? ""
                
                let doc = try SwiftSoup.parse(html)
                var items: [MenuItem] = []
                
                // 메뉴 아이템 파싱
                let menuElements = try doc.select("nav a, .menu-item a")
                for element in menuElements {
                    let title = try element.text()
                    let urlString = try element.attr("href")
                    
                    if !title.isEmpty && !urlString.isEmpty {
                        let fullUrl = urlString.hasPrefix("http") ? urlString : "https://m.clien.net\(urlString)"
                        items.append(MenuItem(
                            title: title,
                            url: fullUrl,
                            description: ""
                        ))
                    }
                }
                
                await MainActor.run {
                    self.menuItems = items
                    self.isLoading = false
                }
            } catch {
                print("Error fetching menu items: \(error)")
                await MainActor.run {
                    self.isLoading = false
                }
            }
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = ClienViewModel()
    
    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(1.5)
                } else {
                    List(viewModel.menuItems) { item in
                        MenuItemRow(item: item)
                    }
                }
            }
            .navigationTitle("Clien Custom App")
            .onAppear {
                viewModel.fetchMenuItems()
            }
        }
    }
}

struct MenuItemRow: View {
    let item: MenuItem
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(item.title)
                .font(.headline)
            if !item.description.isEmpty {
                Text(item.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}