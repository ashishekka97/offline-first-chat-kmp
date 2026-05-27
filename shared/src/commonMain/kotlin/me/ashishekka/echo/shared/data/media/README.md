# Media Processor

The `MediaProcessor` handles image downsizing and thumbnail generation across Android and iOS platforms. It is designed for offline performance, ensuring that large images are optimized for local storage and fast UI rendering.

## Architecture

The system follows a platform-specific implementation pattern using Koin for dependency injection:

- **Interface**: `MediaProcessor.kt` in `commonMain` defines the contract.
- **Android**: `AndroidMediaProcessor.kt` uses the native `Bitmap` API.
- **iOS**: `IosMediaProcessor.kt` uses `UIImage` and `UIGraphics`.

## Key Features

- **Downsizing**: Maintains aspect ratio while scaling images to fit within maximum dimensions.
- **Thumbnail Generation**: Creates small, low-quality previews for lists and chat bubbles.
- **Memory Efficiency**: 
  - On Android, it uses `inSampleSize` to avoid loading full-resolution images into memory when downsizing.
  - On iOS, it leverages native `UIGraphics` for efficient scaling.
- **Thread Safety**: All operations are offloaded to `Dispatchers.Default` using a `DispatcherProvider`.

## Usage

```kotlin
val mediaProcessor: MediaProcessor = get() // Injected via Koin

val downsizedBytes = mediaProcessor.downsizeImage(
    imageData = originalBytes,
    maxWidth = 1024,
    maxHeight = 1024,
    quality = 80
)

val thumbnailBytes = mediaProcessor.generateThumbnail(
    imageData = originalBytes,
    maxDimension = 256
)
```

## Implementation Details

### Android
Uses `BitmapFactory.Options.inSampleSize` to perform a preliminary downsampling during the decoding phase. This significantly reduces the memory footprint for large images. The final scaling is performed using `Bitmap.createScaledBitmap`.

### iOS
Uses `UIGraphicsBeginImageContextWithOptions` to create a new graphics context and draws the `UIImage` into the target rectangle. The resulting image is then compressed using `UIImageJPEGRepresentation`.

## Testing
- **Android**: Unit tests are located in `shared/src/androidTest` and use real `Bitmap` APIs via the Android unit test runner (requires an emulator or device).
- **iOS**: Unit tests are located in `shared/src/iosTest` and verify the logic using native `UIKit` APIs.
