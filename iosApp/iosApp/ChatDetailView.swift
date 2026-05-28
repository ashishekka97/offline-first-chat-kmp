import SwiftUI
import Shared

struct ChatDetailView: View {
    @StateObject var viewModel: ObservableChatDetailViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var showPhotoPicker = false
    @State private var showCameraPicker = false
    @State private var showSourcePicker = false
    @State private var selectedImageFullscreen: String? = nil
    
    init(chatId: String) {
        _viewModel = StateObject(wrappedValue: ObservableChatDetailViewModel(chatId: chatId))
    }
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(viewModel.messages, id: \.timestamp) { message in
                            MessageBubble(message: message) { imageUrl in
                                selectedImageFullscreen = imageUrl
                            }
                        }
                        
                        if let state = viewModel.state, state.isAgentTyping {
                            TypingIndicator()
                        }
                        
                        Color.clear
                            .frame(height: 1)
                            .id("BOTTOM")
                    }
                }
                .onChange(of: viewModel.scrollToBottomTrigger) { _ in
                    scrollToBottom(proxy: proxy)
                }
                .onChange(of: viewModel.messages.count) { _ in
                    scrollToBottom(proxy: proxy)
                }
                .onChange(of: viewModel.state?.isAgentTyping) { isTyping in
                    if isTyping == true {
                        scrollToBottom(proxy: proxy)
                    }
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
                    Button(action: { showSourcePicker = true }) {
                        Image(systemName: "plus")
                            .font(.title3)
                            .foregroundColor(.accentColor)
                            .padding(8)
                            .background(Circle().fill(Color.accentColor.opacity(0.1)))
                    }
                    .confirmationDialog("Choose Source", isPresented: $showSourcePicker) {
                        Button("Camera") { showCameraPicker = true }
                        Button("Gallery") { showPhotoPicker = true }
                        Button("Cancel", role: .cancel) {}
                    }
                    .sheet(isPresented: $showPhotoPicker) {
                        PhotoPicker(isPresented: $showPhotoPicker) { url in
                            viewModel.onIntent(intent: ChatDetailIntentSendMessage(text: "", localMediaPath: url.path))
                        }
                    }
                    .sheet(isPresented: $showCameraPicker) {
                        CameraPicker(isPresented: $showCameraPicker) { url in
                            viewModel.onIntent(intent: ChatDetailIntentSendMessage(text: "", localMediaPath: url.path))
                        }
                    }
                    
                    TextField("Message", text: Binding(
                        get: { viewModel.state?.currentDraft ?? "" },
                        set: { viewModel.onIntent(intent: ChatDetailIntentUpdateDraft(text: $0)) }
                    ), axis: .vertical)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 20).fill(Color(UIColor.secondarySystemBackground)))
                    .lineLimit(1...5)
                    
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
        .fullScreenCover(item: $selectedImageFullscreen) { imageUrl in
            FullscreenImageView(imageUrl: imageUrl)
        }
    }
    
    private func scrollToBottom(proxy: ScrollViewProxy, delay: Double = 0.1) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            withAnimation(.easeOut(duration: 0.3)) {
                proxy.scrollTo("BOTTOM", anchor: .bottom)
            }
        }
    }
}

extension String: Identifiable {
    public var id: String { self }
}

struct TypingIndicator: View {
    @State private var animStep = 0
    
    var body: some View {
        HStack {
            HStack(spacing: 4) {
                ForEach(0..<3) { index in
                    Circle()
                        .fill(Color.secondary)
                        .frame(width: 6, height: 6)
                        .opacity(animStep == index ? 1 : 0.3)
                        .animation(.easeInOut(duration: 0.5), value: animStep)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(16, corners: [.topLeft, .topRight, .bottomRight])
            .cornerRadius(4, corners: [.bottomLeft])
            
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .onAppear {
            Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { timer in
                animStep = (animStep + 1) % 3
            }
        }
    }
}
