#!/bin/bash

# Set the LANG and LC_ALL environment variables to UTF-8 encoding
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

CURR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Current working directory where user executes the script
USER_PWD="$(pwd)"

# Try to use bundled JRE first, fall back to system Java if not found
BUNDLED_JRE="$CURR_DIR/../jre"
if [[ -f "$BUNDLED_JRE/bin/java" ]]; then
    # Use bundled JRE
    JAVA_CMD="$BUNDLED_JRE/bin/java"
    echo "Using bundled JRE from $BUNDLED_JRE"
elif command -v java &> /dev/null; then
    # Use system Java
    JAVA_CMD="java"
    echo "Using system Java"
else
    echo "❌ Error: Java was not found. Please install Java 17 or a higher version first"
    exit 1
fi

cd "$CURR_DIR"

# Find JAR file
JAR_FILE=$(ls "$CURR_DIR"/../dat-cli-*.jar 2>/dev/null | head -n 1)
if [[ ! -f "$JAR_FILE" ]]; then
    echo "❌ Error: DAT CLI jar file not found in $CURR_DIR/.."
    exit 1
fi

# Common Java options
JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Xms256m -Xmx2g"

# Function: Check if command supports -p/--project-path parameter
check_supports_project_path() {
    local command="$1"
    
    # Return false if no command provided
    if [[ -z "$command" ]]; then
        return 1
    fi
    
    # Check if command is one of the supported commands
    case "$command" in
        "build"|"clean"|"list"|"run"|"server"|"seed")
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# Function: Check if command supports -w/--workspace-path parameter
check_supports_workspace_path() {
    local command="$1"

    # Return false if no command provided
    if [[ -z "$command" ]]; then
        return 1
    fi

    # Check if command is one of the supported commands
    case "$command" in
        "init")
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# Get first parameter as command
COMMAND="$1"

# Build arguments array
ARGS=("$@")

# Extract project path from arguments for logging
PROJECT_PATH="$(cd "$USER_PWD" 2>/dev/null && pwd || echo "$USER_PWD")"
for i in "${!ARGS[@]}"; do
    if [[ "${ARGS[i]}" == "-p" ]] || [[ "${ARGS[i]}" == "--project-path" ]]; then
        next_index=$((i + 1))
        if [[ $next_index -lt ${#ARGS[@]} ]]; then
            PROJECT_PATH="${ARGS[next_index]}"
            # Convert to absolute path
            if cd "$USER_PWD" 2>/dev/null; then
                PROJECT_PATH="$(cd "$PROJECT_PATH" 2>/dev/null && pwd || echo "$PROJECT_PATH")"
            fi
        fi
        break
    fi
done

# Extract workspace path from arguments for logging
WORKSPACE_PATH="$(cd "$USER_PWD" 2>/dev/null && pwd || echo "$USER_PWD")"
for i in "${!ARGS[@]}"; do
    if [[ "${ARGS[i]}" == "-w" ]] || [[ "${ARGS[i]}" == "--workspace-path" ]]; then
        next_index=$((i + 1))
        if [[ $next_index -lt ${#ARGS[@]} ]]; then
            WORKSPACE_PATH="${ARGS[next_index]}"
            # Convert to absolute path
            if cd "$USER_PWD" 2>/dev/null; then
                WORKSPACE_PATH="$(cd "$WORKSPACE_PATH" 2>/dev/null && pwd || echo "$WORKSPACE_PATH")"
            fi
        fi
        break
    fi
done

# Check if need to add project path parameter
if [[ ! " $* " =~ " -p " ]] && [[ ! " $* " =~ " --project-path " ]] && check_supports_project_path "$COMMAND"; then
    ARGS=("$@" -p "$PROJECT_PATH")
fi

# Check if need to add workspace path parameter
if [[ ! " $* " =~ " -w " ]] && [[ ! " $* " =~ " --workspace-path " ]] && check_supports_workspace_path "$COMMAND"; then
    ARGS=("$@" -w "$WORKSPACE_PATH")
fi

# Set logs root path based on whether command supports project path
if check_supports_project_path "$COMMAND"; then
    LOGS_ROOT_PATH="$PROJECT_PATH"
else
    LOGS_ROOT_PATH="$CURR_DIR/.."
fi

# Execute Java program with logs root path for logging
eval "$JAVA_CMD $JAVA_OPTS -Ddat.logs.root.path=\"$LOGS_ROOT_PATH\" -jar \"$JAR_FILE\" ${ARGS[*]@Q}"
