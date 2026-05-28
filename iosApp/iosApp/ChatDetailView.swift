import SwiftUI
import Shared

struct ChatDetailView: View {
    @StateObject var viewModel: ObservableChatDetailViewModel
    @Environment(\.dismiss) private var dismiss
    
    init(chatId: String) {
        _viewModel = StateObject(wrappedValue: ObservableChatDetailViewModel(chatId: chatId))
    }
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(viewModel.messages, id: \.timestamp) { message in
                            MessageBubble(message: message)
                        }
                        
                        if let state = viewModel.state, state.isAgentTyping {
                            HStack {
                                Text("Agent is typing...")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .padding(.horizontal, 20)
                                    .padding(.vertical, 8)
                                Spacer()
                            }
                        }
                        
                        Color.clear
                            .frame(height: 1)
                            .id("BOTTOM")
                    }
                }
                .onChange(of: viewModel.scrollToBottomTrigger) { _ in
                    scrollToBottom(proxy: proxy)
                }
                .onAppear {
                    viewModel.onIntent(intent: ChatDetailIntentOnInitialMessagesLoaded())
                    scrollToBottom(proxy: proxy, delay: 0.3)
                }
            }
            
            // Polished Input Area
            VStack(spacing: 0) {
                Divider()
                HStack(alignment: .bottom, spacing: 12) {
                    Button(action: {}) {
                        Image(systemName: "plus")
                            .font(.title3)
                            .foregroundColor(.accentColor)
                            .padding(8)
                            .background(Circle().fill(Color.accentColor.opacity(0.1)))
                    }
                    
                    TextEditor(text: Binding(
                        get: { viewModel.state?.currentDraft ?? "" },
                        set: { viewModel.onIntent(intent: ChatDetailIntentUpdateDraft(text: $0)) }
                    ))
                    .frame(minHeight: 36, maxHeight: 120)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(RoundedRectangle(cornerRadius: 20).fill(Color(UIColor.secondarySystemBackground)))
                    .fixedSize(horizontal: false, vertical: true)
                    
                    Button(action: {
                        if let draft = viewModel.state?.currentDraft, !draft.isEmpty {
                            viewModel.onIntent(intent: ChatDetailIntentSendMessage(text: draft, localMediaPath: nil))
                        }
                    }) {
                        Image(systemName: "paperplane.fill")
                            .font(.title3)
                            .foregroundColor(viewModel.state?.currentDraft.isEmpty ?? true ? .secondary : .white)
                            .padding(8)
                            .background(Circle().fill(viewModel.state?.currentDraft.isEmpty ?? true ? Color.gray.opacity(0.2) : Color.accentColor))
                    }
                    .disabled(viewModel.state?.currentDraft.isEmpty ?? true)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(UIColor.systemBackground))
            }
        }
        .navigationTitle(viewModel.state?.chat?.title ?? "Chat")
        .navigationBarTitleDisplayMode(.inline)
        .ignoresSafeArea(.keyboard, edges: .bottom) // Ensures content stays visible above keyboard
    }
    
    private func scrollToBottom(proxy: ScrollViewProxy, delay: Double = 0.1) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            withAnimation(.easeOut(duration: 0.3)) {
                proxy.scrollTo("BOTTOM", anchor: .bottom)
            }
        }
    }
}
