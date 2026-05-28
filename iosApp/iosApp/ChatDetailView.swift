import SwiftUI
import Shared

struct ChatDetailView: View {
    @StateObject var viewModel: ObservableChatDetailViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var showPhotoPicker = false
    @State private var showCameraPicker = false
    @State private var showSourcePicker = false
    @State private var selectedImageFullscreen: String? = nil
    
    @State private var showRenameDialog = false
    @State private var newTitle = ""
    
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
            
            // Modern Pill Input Area
            VStack(spacing: 0) {
                HStack(alignment: .bottom, spacing: 10) {
                    HStack(alignment: .bottom, spacing: 4) {
                        Button(action: { showSourcePicker = true }) {
                            Image(systemName: "plus")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(8)
                                .background(Circle().fill(Color.accentColor))
                        }
                        .padding(.leading, 4)
                        .padding(.bottom, 4)
                        .confirmationDialog(LocalizedStringKey("chat_choose_source"), isPresented: $showSourcePicker) {
                            Button(LocalizedStringKey("chat_camera")) { showCameraPicker = true }
                            Button(LocalizedStringKey("chat_gallery")) { showPhotoPicker = true }
                            Button(LocalizedStringKey("common_cancel"), role: .cancel) {}
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
                        
                        TextField(LocalizedStringKey("chat_message_placeholder"), text: Binding(
                            get: { viewModel.state?.currentDraft ?? "" },
                            set: { viewModel.onIntent(intent: ChatDetailIntentUpdateDraft(text: $0)) }
                        ), axis: .vertical)
                        .font(.body)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .lineLimit(1...5)
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 25)
                            .fill(Color(UIColor.secondarySystemBackground))
                            .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 1)
                    )
                    
                    if !(viewModel.state?.currentDraft.isEmpty ?? true) {
                        Button(action: {
                            if let draft = viewModel.state?.currentDraft, !draft.isEmpty {
                                let generator = UIImpactFeedbackGenerator(style: .medium)
                                generator.impactOccurred()
                                viewModel.onIntent(intent: ChatDetailIntentSendMessage(text: draft, localMediaPath: nil))
                            }
                        }) {
                            Image(systemName: "arrow.up.circle.fill")
                                .font(.system(size: 36))
                                .foregroundColor(.accentColor)
                        }
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color(UIColor.systemBackground))
                .animation(.spring(), value: viewModel.state?.currentDraft.isEmpty)
            }
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(viewModel.state?.chat?.title ?? String(localized: "chat_new_title"))
                    .font(.headline)
                    .onTapGesture {
                        if let chat = viewModel.state?.chat {
                            newTitle = chat.title
                            showRenameDialog = true
                        }
                    }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .alert(LocalizedStringKey("chat_rename_title"), isPresented: $showRenameDialog) {
            TextField(LocalizedStringKey("chat_rename_label"), text: $newTitle)
            Button(LocalizedStringKey("common_save")) {
                viewModel.onIntent(intent: ChatDetailIntentRenameChat(newTitle: newTitle))
            }
            Button(LocalizedStringKey("common_cancel"), role: .cancel) {}
        } message: {
            Text(LocalizedStringKey("chat_rename_message"))
        }
        .alert(LocalizedStringKey("common_error"), isPresented: Binding(
            get: { viewModel.state?.error != nil },
            set: { if !$0 { viewModel.onIntent(intent: ChatDetailIntentClearError()) } }
        )) {
            Button(LocalizedStringKey("common_ok")) { viewModel.onIntent(intent: ChatDetailIntentClearError()) }
        } message: {
            if let error = viewModel.state?.error {
                Text(String(describing: error))
            }
        }
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
