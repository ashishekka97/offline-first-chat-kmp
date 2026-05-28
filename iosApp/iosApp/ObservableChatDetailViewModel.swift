import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

@MainActor
class ObservableChatDetailViewModel: ObservableObject {
    private let viewModel: ChatDetailViewModel
    private let messagesPresenter = IosPagingPresenter<Message>()
    
    @Published var state: ChatDetailState?
    @Published var messages: [Message] = []
    @Published var scrollToBottomTrigger: Int = 0
    
    init(chatId: String) {
        self.viewModel = KoinHelper().getChatDetailViewModel(chatId: chatId)
        
        // Observe State
        Task {
            do {
                let sequence = asyncSequence(for: viewModel.state)
                for try await state in sequence {
                    self.state = state
                    self.messagesPresenter.collectFrom(pagingDataFlow: state.messages)
                }
            } catch {
                print("Error observing ChatDetailViewModel state: \(error)")
            }
        }
        
        // Observe Messages from Presenter
        Task {
            do {
                let sequence = asyncSequence(for: messagesPresenter.items)
                for try await messages in sequence {
                    self.messages = messages
                }
            } catch {
                print("Error observing messages from presenter: \(error)")
            }
        }
        
        // Observe Side Effects
        Task {
            do {
                let sequence = asyncSequence(for: viewModel.sideEffect)
                for try await effect in sequence {
                    if effect is ChatDetailSideEffectScrollToBottom {
                        self.scrollToBottomTrigger += 1
                    }
                }
            } catch {
                print("Error observing ChatDetailViewModel side effects: \(error)")
            }
        }
    }
    
    func onIntent(intent: ChatDetailIntent) {
        viewModel.onIntent(intent: intent)
    }
}
