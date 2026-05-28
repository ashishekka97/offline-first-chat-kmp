import SwiftUI
import Shared

struct ChatRow: View {
    let chat: Chat
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(chat.title)
                    .font(.headline)
                    .lineLimit(1)
                Spacer()
                Text(chat.displayTimestamp)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Text(chat.lastMessage ?? "No messages yet")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .lineLimit(1)
        }
        .padding(.vertical, 4)
    }
}

struct HomeView: View {
    @ObservedObject var viewModel: ObservableHomeViewModel
    let onChatClick: (Any) -> Void
    let onNewChatClick: () -> Void
    
    var body: some View {
        NavigationStack {
            Group {
                if let state = viewModel.state {
                    if state.isDeleting {
                        ProgressView("Deleting...")
                    } else if !viewModel.chats.isEmpty {
                        List {
                            ForEach(viewModel.chats, id: \.title) { chat in
                                ChatRow(chat: chat)
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        onChatClick(chat.id)
                                    }
                                    .swipeActions(edge: HorizontalEdge.trailing) {
                                        Button(role: .destructive) {
                                            viewModel.onIntent(intent: HomeIntentConfirmDelete(chatId: chat.id))
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                            }
                        }
                        .listStyle(.plain)
                    } else {
                        VStack(spacing: 20) {
                            Text("No chats yet")
                                .font(.title2)
                                .foregroundColor(.secondary)
                            Button(action: onNewChatClick) {
                                Text("Start a new one!")
                                    .fontWeight(.bold)
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                } else {
                    ProgressView()
                }
            }
            .navigationTitle("Echo")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: onNewChatClick) {
                        Image(systemName: "plus")
                    }
                }
            }
            .alert("Delete Chat", isPresented: Binding(
                get: { viewModel.state?.pendingDeleteChatId != nil },
                set: { if !$0 { viewModel.onIntent(intent: HomeIntentCancelDelete()) } }
            )) {
                Button("Cancel", role: .cancel) {
                    viewModel.onIntent(intent: HomeIntentCancelDelete())
                }
                Button("Delete", role: .destructive) {
                    viewModel.onIntent(intent: HomeIntentDeletePendingChat())
                }
            } message: {
                Text("Are you sure you want to delete this conversation? This action cannot be undone.")
            }
        }
    }
}
