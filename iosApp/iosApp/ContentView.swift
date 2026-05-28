import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ObservableHomeViewModel()
    
    var body: some View {
        if viewModel.state.isInitialBootstrap {
            SplashView()
        } else {
            NavigationStack {
                Text("Home Ready")
                    .navigationTitle("Echo")
            }
        }
    }
}
