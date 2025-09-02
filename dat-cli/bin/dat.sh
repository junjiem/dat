#!/bin/bash

# Set the LANG and LC_ALL environment variables to UTF-8 encoding
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

CURR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Current working directory where user executes the script
USER_PWD="$(pwd)"

if ! command -v java &> /dev/null; then
    echo "❌ Error: Java was not found. Please install Java 17 or a higher version first"
    exit 1
fi

cd "$CURR_DIR"

# Find JAR file
JAR_FILE=$(ls "$CURR_DIR"/dat-cli-*.jar 2>/dev/null | head -n 1)
if [[ ! -f "$JAR_FILE" ]]; then
    echo "❌ Error: DAT CLI jar file not found in $CURR_DIR"
    exit 1
fi

# Common part of Java execution command
JAVA_CMD="java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# Function: Check if command supports -p/--project-path parameter
check_supports_project_path() {
    local command="$1"
    
    # Return false if no command provided
    if [[ -z "$command" ]]; then
        return 1
    fi
    
    # server command always supports project-path parameter
    if [[ "$command" == "server" ]]; then
        return 0
    fi
    
    # Try to get command help info and check if it contains -p, --project-path parameter
    local help_output
    help_output=$(eval "$JAVA_CMD -jar \"$JAR_FILE\" \"$command\" --help" 2>/dev/null)
    
    # Check if help output contains exactly "-p, --project-path=" pattern
    if echo "$help_output" | grep -q -E "^\s*-p,\s*--project-path="; then
        return 0
    else
        return 1
    fi
}

# Get first parameter as command
COMMAND="$1"

# Extract project path from arguments for logging
PROJECT_PATH="$USER_PWD"
for i in "${!@}"; do
    if [[ "${!i}" == "-p" ]] || [[ "${!i}" == "--project-path" ]]; then
        next_index=$((i + 1))
        if [[ $next_index -le $# ]]; then
            PROJECT_PATH="${!next_index}"
            # Convert to absolute path
            PROJECT_PATH="$(cd "$PROJECT_PATH" 2>/dev/null && pwd || echo "$PROJECT_PATH")"
        fi
        break
    fi
done

# Build arguments array
ARGS=("$@")

# Check if need to add project path parameter
if [[ ! " $* " =~ " -p " ]] && [[ ! " $* " =~ " --project-path " ]] && check_supports_project_path "$COMMAND"; then
    ARGS=("$@" -p "$PROJECT_PATH")
fi

# Execute Java program with project path for logging
eval "$JAVA_CMD -Ddat.project.path=\"$PROJECT_PATH\" -jar \"$JAR_FILE\" ${ARGS[*]@Q}"
