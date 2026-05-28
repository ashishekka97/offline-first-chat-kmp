import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ObservableHomeViewModel()
    
    var body: some View {
        if let state = viewModel.state {
            if state.isInitialBootstrap {
                SplashView()
            } else {
                HomeView(
                    viewModel: viewModel,
                    onChatClick: { chatId in
                        viewModel.onIntent(intent: HomeIntentClickChat(chatId: chatId))
                    },
                    onNewChatClick: {
                        viewModel.onIntent(intent: HomeIntentNewChat())
                    }
                )
            }
        } else {
            // Loading state
            SplashView()
        }
    }
}
