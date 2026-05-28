import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

@MainActor
class ObservableHomeViewModel: ObservableObject {
    private let viewModel: HomeViewModel
    
    @Published var state: HomeState = HomeState(
        chats: .init(elements: []),
        isDeleting: false,
        pendingDeleteChatId: nil,
        isInitialBootstrap: false,
        error: nil
    )
    
    init() {
        self.viewModel = KoinHelper().homeViewModel
        
        Task {
            for await state in asyncSequence(for: viewModel.state) {
                self.state = state
            }
        }
    }
}
