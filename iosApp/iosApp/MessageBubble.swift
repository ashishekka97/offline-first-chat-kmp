import SwiftUI
import Shared
import Kingfisher

struct MessageBubble: View {
    let message: Message
    var onImageClick: ((String) -> Void)? = nil
    
    var body: some View {
        HStack {
            if message.isFromMe { Spacer() }
            
            VStack(alignment: message.isFromMe ? .trailing : .leading, spacing: 4) {
                VStack(alignment: .leading, spacing: 8) {
                    if message.type == MessageType.file && message.file != nil {
                        let thumbnailPath = message.file?.thumbnail?.fullPath ?? message.file?.fullPath ?? ""
                        let fullPath = message.file?.fullPath ?? ""
                        
                        KFImage(URL(fileURLWithPath: thumbnailPath))
                            .resizable()
                            .placeholder {
                                ImagePlaceholder()
                            }
                            .scaledToFill()
                            .frame(width: 200, height: 200)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .onTapGesture {
                                onImageClick?(fullPath)
                            }
                        
                        if !message.message.isEmpty {
                            Text(message.message)
                                .font(.body)
                        }
                        
                        Text(message.displaySize)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    } else {
                        Text(message.message)
                            .font(.body)
                    }
                }
                .padding(12)
                .background(message.isFromMe ? Color.accentColor : Color(UIColor.secondarySystemBackground))
                .foregroundColor(message.isFromMe ? .white : .primary)
                .cornerRadius(16, corners: [
                    .topLeft, .topRight, 
                    message.isFromMe ? .bottomLeft : .bottomRight
                ])
                .cornerRadius(4, corners: [message.isFromMe ? .bottomRight : .bottomLeft])
                
                Text(message.displayTimestamp)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
            }
            
            if !message.isFromMe { Spacer() }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
}

struct ImagePlaceholder: View {
    var body: some View {
        ZStack {
            Color(UIColor.secondarySystemBackground)
            Image(systemName: "photo")
                .foregroundColor(.secondary)
        }
    }
}

// Helper for selective corner radius
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
