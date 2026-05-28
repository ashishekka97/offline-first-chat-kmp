import SwiftUI

struct SplashView: View {
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            Text(LocalizedStringKey("home_title"))
                .font(.system(size: 48, weight: .bold, design: .rounded))
                .foregroundColor(.accentColor)
        }
    }
}

#Preview {
    SplashView()
}
