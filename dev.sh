#!/bin/bash

# dev.sh - A helper script to ensure the correct environment for Echo Chat App development.
# It automatically sets DEVELOPER_DIR on macOS to point to the full Xcode installation,
# mimicking the behavior of Android Studio for CLI builds.

if [ "$(uname)" = "Darwin" ]; then
    # Check if DEVELOPER_DIR is already set
    if [ -z "$DEVELOPER_DIR" ]; then
        # Standard Xcode path
        XCODE_PATH="/Applications/Xcode.app/Contents/Developer"
        
        if [ -d "$XCODE_PATH" ]; then
            export DEVELOPER_DIR="$XCODE_PATH"
            echo "✅ dev.sh: Using Xcode at $XCODE_PATH"
        else
            echo "⚠️ dev.sh: Full Xcode not found at $XCODE_PATH."
            echo "   iOS builds might fail if xcode-select is pointing to Command Line Tools."
        fi
    fi
fi

# Pass all arguments to the real command (defaulting to gradlew if no command is provided)
if [ $# -eq 0 ]; then
    ./gradlew tasks
else
    exec "$@"
fi
