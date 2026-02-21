# Helper Scripts Documentation

## Overview

This directory contains batch scripts to automate common Angler Assistant project tasks.

## Scripts

### 1. create-structure.bat
**Purpose**: Creates the complete folder structure for the fine-tuning project.

**Features**:
- Auto-detects current drive from script location
- Creates all required directories for fine-tuning project
- Sets up separate directories for data, output, and resources
- Creates placeholder files for Maven configuration and resources
- Creates config directory for forum engine JSON configurations
- Skips existing directories (safe to run multiple times)

**Directory Structure Created**:
```
AnglerAsistant/
├── fine-tuning/
│   ├── data/           # Operational data (raw/processed/labeled)
│   ├── output/         # Generated training files
│   ├── src/
│   │   └── main/
│   │       ├── java/   # Source code
│   │       │   └── com/
│   │       │       └── ft/
│   │       │           └── extractor/
│   │       │               ├── core/      # Core interfaces and common functionality
│   │       │               ├── forums/    # Common forum engine with JSON configs
│   │       │               ├── training/  # LoRA training utilities
│   │       │               └── util/      # Utility classes
│   │       └── resources/  # Static configs/dictionaries
│   │       └── config/      # Forum engine JSON configurations
│   ├── pom.xml         # Maven configuration
│   └── README.md       # Project documentation
├── Docs/               # Project documentation
├── Helpers/            # This directory
└── Progress/           # Daily progress tracking
```

**Usage**:
```batch
# Run from any drive/folder - creates structure on current drive
create-structure.bat
```

### 2. backup.bat
**Purpose**: Backs up the entire Angler Assistant project to a specified drive.

**Features**:
- Auto-detects source drive from script location
- Interactive target drive selection with validation
- Shows available drives if invalid target entered
- Uses robocopy for reliable file copying
- Preserves all file attributes, timestamps, and security
- Creates backup log file
- Handles retry logic for failed copies

**Usage**:
```batch
# Run from any location
backup.bat

# Example interaction:
Enter target drive letter (A-Z): G
Backup Configuration:
Source: C:\AnglerAsistant
Target: G:\AnglerAsistant
```

**Robocopy Options Used**:
- `/E` - Copy subdirectories including empty ones
- `/COPYALL` - Copy all file info (data, attributes, timestamps, security)
- `/R:2` - Retry 2 times on failed copies
- `/W:5` - Wait 5 seconds between retries
- `/ETA` - Show estimated time of arrival
- `/LOG` - Create log file

**Exit Codes**:
- 0-7: Success (robocopy convention)
- 8+: Warning/errors occurred

## Drive Detection

Both scripts use automatic drive detection:
- `%~dp0` = Script directory path
- `%~dp0.` = Drive-relative path  
- `%%~dd` = Extracts drive letter only

This makes scripts portable - they work on any drive when copied.

## Safety Features

- **create-structure.bat**: Checks for existing directories before creation
- **backup.bat**: Validates target drive existence before backup
- Both scripts preserve existing data and are safe to run multiple times

## Log Files

**backup.bat** creates: `{target_drive}\AnglerAsistant\backup-log.txt`
- Detailed backup operation log
- Success/failure information per file
- Total files copied and errors encountered

## Troubleshooting

**Common Issues**:

1. **Permission Denied**: Run as Administrator if needed
2. **Drive Not Found**: Check drive letters are available
3. **Backup Fails**: Check robocopy log file for details
4. **Structure Creation Fails**: Ensure drive has sufficient space

**Log Location**: `{target_drive}\AnglerAsistant\backup-log.txt`

### 3. download_israfish.bat
**Purpose**: Downloads content from IsraFish forum using the common forum engine.

**Features**:
- Auto-detects current drive from script location
- Runs IsrafishMain with default configuration
- Checks for Java availability and compiled classes
- Shows download paths and URLs before starting
- Handles download progress and error reporting
- Works with the common forum engine architecture

**Usage**:
```batch
# Run from any drive - uses default configuration
download_israfish.bat
```

**Default Configuration**:
- Forums: Freshwater (f=128) and Saltwater (f=129)
- Output: `C:\AnglerAsistant\Fine-tuning\data\raw\israfish_forum\{path}`
- Engine Type: vbulletin (automatic selection)
- Language: RU (Russian)

**Prerequisites**:
1. Java installed and in PATH
2. Maven compilation completed: `mvn compile`
3. Internet connectivity for forum access

**Exit Codes**:
- 0: Success
- 1: Error (Java missing, classes not found, or download error)

**Troubleshooting**:
1. **Java not found**: Install Java and add to PATH
2. **Classes not found**: Run `mvn compile` first
3. **Download errors**: Check forum accessibility and network connection