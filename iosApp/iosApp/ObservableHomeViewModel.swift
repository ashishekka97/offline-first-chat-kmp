import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

@MainActor
class ObservableHomeViewModel: ObservableObject {
    private let viewModel: HomeViewModel
    private let chatsPresenter = IosPagingPresenter<Chat>()
    
    @Published var state: HomeState?
    @Published var chats: [Chat] = []
    
    init() {
        self.viewModel = KoinHelper().homeViewModel
        
        // Observe State
        Task {
            do {
                let sequence = asyncSequence(for: viewModel.state)
                for try await state in sequence {
                    self.state = state
                    // Initialize collection once we have the flow
                    self.chatsPresenter.collectFrom(pagingDataFlow: state.chats)
                }
            } catch {
                print("Error observing HomeViewModel state: \(error)")
            }
        }
        
        // Observe Chats from Presenter
        Task {
            do {
                let sequence = asyncSequence(for: chatsPresenter.items)
                for try await chats in sequence {
                    self.chats = chats
                }
            } catch {
                print("Error observing chats from presenter: \(error)")
            }
        }
    }

    func onIntent(intent: HomeIntent) {
        viewModel.onIntent(intent: intent)
    }
}
