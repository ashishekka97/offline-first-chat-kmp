import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

@MainActor
class ObservableHomeViewModel: ObservableObject {
    private let viewModel: HomeViewModel
    
    @Published var state: HomeState?
    
    init() {
        self.viewModel = KoinHelper().homeViewModel
        
        Task {
            do {
                let sequence = asyncSequence(for: viewModel.state)
                for try await state in sequence {
                    self.state = state
                }
            } catch {
                print("Error observing HomeViewModel state: \(error)")
            }
        }
    }
}
