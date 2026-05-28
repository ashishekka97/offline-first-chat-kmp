import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ObservableHomeViewModel()
    
    var body: some View {
        if let state = viewModel.state {
            if state.isInitialBootstrap {
                SplashView()
            } else {
                NavigationStack {
                    HomeView(
                        viewModel: viewModel,
                        onChatClick: { chatId in
                            viewModel.onIntent(intent: HomeIntentClickChat(chatId: chatId))
                        },
                        onNewChatClick: {
                            viewModel.onIntent(intent: HomeIntentNewChat())
                        }
                    )
                    .navigationDestination(isPresented: Binding(
                        get: { viewModel.navigateToChatId != nil },
                        set: { if !$0 { viewModel.navigateToChatId = nil } }
                    )) {
                        if let chatId = viewModel.navigateToChatId {
                            ChatDetailView(chatId: chatId)
                        }
                    }
                }
            }
        } else {
            // Loading state
            SplashView()
        }
    }
}
