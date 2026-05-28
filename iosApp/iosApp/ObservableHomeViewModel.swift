import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

@MainActor
class ObservableHomeViewModel: ObservableObject {
    private let viewModel: HomeViewModel
    private let chatsPresenter = IosPagingPresenter<Chat>()
    
    @Published var state: HomeState?
    @Published var chats: [Chat] = []
    @Published var navigateToChatId: String?
    
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
        
        // Observe Side Effects
        Task {
            do {
                let sequence = asyncSequence(for: viewModel.sideEffect)
                for try await effect in sequence {
                    if let navigateEffect = effect as? HomeSideEffectNavigateToChat {
                        // Value class ID is id/Any in Swift, cast to String using description or similar
                        // Based on Shared.h, we can probably use the underlying value.
                        // For safety, I'll use the description or check how to extract it.
                        // Actually, HomeSideEffectNavigateToChat has a chatId property.
                        self.navigateToChatId = String(describing: navigateEffect.chatId)
                    }
                }
            } catch {
                print("Error observing HomeViewModel side effects: \(error)")
            }
        }
    }

    func onIntent(intent: HomeIntent) {
        viewModel.onIntent(intent: intent)
    }
}
