import SwiftUI
import Kingfisher

struct FullscreenImageView: View {
    let imageUrl: String
    @Environment(\.dismiss) private var dismiss
    
    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color.black.ignoresSafeArea()
                
                KFImage(URL(fileURLWithPath: imageUrl))
                    .resizable()
                    .scaledToFit()
                    .scaleEffect(scale)
                    .offset(offset)
                    .gesture(
                        MagnificationGesture()
                            .onChanged { value in
                                let delta = value / lastScale
                                lastScale = value
                                scale *= delta
                            }
                            .onEnded { _ in
                                lastScale = 1.0
                                if scale < 1.0 {
                                    withAnimation(.spring()) {
                                        scale = 1.0
                                        offset = .zero
                                        lastOffset = .zero
                                    }
                                } else {
                                    validateAndCorrectOffset(in: geometry.size)
                                }
                            }
                            .simultaneously(with: DragGesture()
                                .onChanged { value in
                                    offset = CGSize(
                                        width: lastOffset.width + value.translation.width,
                                        height: lastOffset.height + value.translation.height
                                    )
                                }
                                .onEnded { _ in
                                    validateAndCorrectOffset(in: geometry.size)
                                }
                            )
                    )
                    .onTapGesture(count: 2) {
                        withAnimation(.spring()) {
                            if scale > 1.0 {
                                scale = 1.0
                                offset = .zero
                                lastOffset = .zero
                            } else {
                                scale = 3.0
                            }
                        }
                    }
                
                VStack {
                    HStack {
                        Button(action: { dismiss() }) {
                            Image(systemName: "xmark")
                                .font(.title2)
                                .foregroundColor(.white)
                                .padding()
                                .background(Circle().fill(Color.black.opacity(0.5)))
                        }
                        .padding()
                        Spacer()
                    }
                    Spacer()
                }
            }
        }
        .statusBar(hidden: true)
    }
    
    private func validateAndCorrectOffset(in containerSize: CGSize) {
        // Basic bounds checking to keep the image somewhat on screen
        // In a real app, this would be more complex (calculating image aspect ratio etc.)
        let maxHorizontalOffset = (containerSize.width * (scale - 1)) / 2
        let maxVerticalOffset = (containerSize.height * (scale - 1)) / 2
        
        var newOffset = offset
        
        if scale > 1.0 {
            if abs(newOffset.width) > maxHorizontalOffset {
                newOffset.width = newOffset.width > 0 ? maxHorizontalOffset : -maxHorizontalOffset
            }
            if abs(newOffset.height) > maxVerticalOffset {
                newOffset.height = newOffset.height > 0 ? maxVerticalOffset : -maxVerticalOffset
            }
        } else {
            newOffset = .zero
        }
        
        withAnimation(.spring()) {
            offset = newOffset
            lastOffset = newOffset
        }
    }
}
