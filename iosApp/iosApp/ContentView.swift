import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ObservableHomeViewModel()
    
    var body: some View {
        if let state = viewModel.state {
            if state.isInitialBootstrap {
                SplashView()
            } else {
                NavigationStack {
                    Text("Home Ready")
                        .navigationTitle("Echo")
                }
            }
        } else {
            // Loading state
            SplashView()
        }
    }
}
