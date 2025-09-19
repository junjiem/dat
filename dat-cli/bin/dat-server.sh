#!/bin/bash

# DAT Server Management Script for Linux/macOS
# Set the LANG and LC_ALL environment variables to UTF-8 encoding
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

CURR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE=""
# Current working directory where user executes the script
USER_PWD="$(pwd)"
PROJECT_PATH="$(cd "$USER_PWD" 2>/dev/null && pwd || echo "$USER_PWD")"

# ==================== Service Type Configuration ====================

# Service type configuration - manage all supported service types here
SERVICE_TYPES=("openapi" "mcp" "a2a")

# Get all service type keys (for validation)
get_service_type_keys() {
    echo "${SERVICE_TYPES[@]}"
}

# Get service type description
get_service_type_description() {
    local service_type="$1"
    case "$service_type" in
        "openapi")
            echo "OpenAPI server with Swagger UI"
            ;;
        "mcp")
            echo "MCP server"
            ;;
        "a2a")
            echo "A2A server [Not supported yet]"
            ;;
        *)
            echo "Unknown service type: $service_type"
            ;;
    esac
}

# ==================== Common Functions ====================

# Dynamically calculate project .dat directory path
get_project_dat_dir() {
    echo "${PROJECT_PATH}/.dat"
}

# Get PID file path
get_pid_file() {
    local service_type="$1"
    echo "$(get_project_dat_dir)/dat-server-$service_type.pid"
}

# Get log file path
get_log_file() {
    local service_type="$1"
    echo "$(get_project_dat_dir)/dat-server-$service_type.log"
}

# Validate if service type is valid
validate_service_type() {
    local service_type="$1"
    
    # Check if service type exists in configuration list
    for valid_type in "${SERVICE_TYPES[@]}"; do
        if [[ "$service_type" == "$valid_type" ]]; then
            return 0
        fi
    done
    return 1
}

# Show available service types help information
show_service_types_help() {
    echo ""
    echo "Available service types:"
    
    # Dynamically generate service type help information
    for service_type in $(get_service_type_keys); do
        local description=$(get_service_type_description "$service_type")
        printf "  %-10s - %s\n" "$service_type" "$description"
    done
    
    echo ""
}

# Parse and validate service type parameter
parse_and_validate_service_type() {
    local command="$1"
    local service_type="$2"
    
    if [[ -z "$service_type" ]]; then
        echo "❌ Error: Service type must be specified"
        show_service_types_help
        echo "Usage: $0 $command <SERVICE_TYPE> [OPTIONS]"
        return 1
    fi
    
    if ! validate_service_type "$service_type"; then
        echo "❌ Error: Invalid service type '$service_type'"
        show_service_types_help
        echo "Usage: $0 $command <SERVICE_TYPE> [OPTIONS]"
        return 1
    fi
    
    return 0
}

# Function to check JAR file and Java
check_jar_and_java() {
    # Find JAR file
    JAR_FILE=$(ls "$CURR_DIR"/../dat-cli-*.jar 2>/dev/null | head -n 1)

    if [[ ! -f "$JAR_FILE" ]]; then
        echo "❌ Error: DAT CLI jar file not found in $CURR_DIR/.."
        return 1
    fi

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
        return 1
    fi

    return 0
}

# Function to check if service type is already running
check_service_running() {
    local service_type=$1
    SERVICE_RUNNING=false
    
    local project_dat_dir="$(get_project_dat_dir)"
    
    # Check if .dat directory exists in project directory
    if [[ ! -d "$project_dat_dir" ]]; then
        return 0
    fi
    
    local service_pid_file="$(get_pid_file "$service_type")"
    if [[ ! -f "$service_pid_file" ]]; then
        return 0
    fi
    
    # Read PID and check if process is still running
    local service_pid=$(cat "$service_pid_file")
    if kill -0 "$service_pid" 2>/dev/null; then
        SERVICE_RUNNING=true
    else
        # Clean up stale PID file
        rm -f "$service_pid_file"
    fi
}

# Function to start the server
start_server() {
    if ! check_jar_and_java; then
        return 1
    fi

    echo "🚀 Starting DAT Server..."
    
    # Initialize variables
    local service_type=""
    local remaining_args=()
    
    # Parse arguments - extract service type and project path, pass everything else to Java
    while [[ $# -gt 0 ]]; do
        # Check if it's a known service type
        if validate_service_type "$1"; then
            if [[ -z "$service_type" ]]; then
                service_type="$1"
                shift
            else
                # If service type already set, this becomes a regular argument
                remaining_args+=("$1")
                shift
            fi
        else
            case $1 in
                -p|--project-path)
                    # Pass project path arguments to both script and Java
                    PROJECT_PATH="$2"
                    # Convert to absolute path
                    PROJECT_PATH="$(cd "$PROJECT_PATH" 2>/dev/null && pwd || echo "$PROJECT_PATH")"
                    remaining_args+=("$1" "$2")
                    shift 2
                    ;;
                *)
                    # All other arguments go to Java
                    remaining_args+=("$1")
                    shift
                    ;;
            esac
        fi
    done
    
    # Validate that service type is specified
    if ! parse_and_validate_service_type "start" "$service_type"; then
        echo "Example: $0 start openapi -p /my/project"
        return 1
    fi
    
    # Create .dat directory in project directory if it doesn't exist
    mkdir -p "$(get_project_dat_dir)"
    
    # Check if this service type is already running
    check_service_running "$service_type"
    if [[ "$SERVICE_RUNNING" == "true" ]]; then
        echo "⚠️ DAT $service_type server is already running in project: $PROJECT_PATH"
        echo "   PID file: $(get_pid_file "$service_type")"
        return 0
    fi
    
    # Set project-specific files for service type management
    local project_pid_file="$(get_pid_file "$service_type")"
    local project_log_file="$(get_log_file "$service_type")"
    
    # Start the server in background with dynamic subcommand
    # Set Java options
    JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Xms256m -Xmx2g -Ddat.logs.root.path=$PROJECT_PATH"

    # Standard mode - use JAR file
    nohup $JAVA_CMD $JAVA_OPTS -jar "$JAR_FILE" server $service_type \
        "${remaining_args[@]}" \
        > "$project_log_file" 2>&1 &
    
    local server_pid=$!
    
    # Save PID to project-based PID file
    echo "$server_pid" > "$project_pid_file"
    
    # Wait a bit and check if the process is still running
    sleep 10

    if kill -0 "$server_pid" 2>/dev/null; then
        echo "✅ Server started successfully!"
        echo "🎯 Service Type: $service_type"
        echo "📂 Project Path: $PROJECT_PATH"
        echo "📝 Arguments: ${remaining_args[*]}"
        echo "📋 PID file: $project_pid_file"
        echo "📋 Log file: $project_log_file"
        return 0
    else
        echo "❌ Failed to start server"
        echo "📋 Check log file for details: $project_log_file"
        rm -f "$project_pid_file"
        return 1
    fi
}

# Function to stop the server
stop_server() {
    local service_type="$1"
    
    # Validate service type
    if ! parse_and_validate_service_type "stop" "$service_type"; then
        echo "Example: $0 stop openapi -p /my/project"
        return 1
    fi
    
    shift
    
    # Parse remaining arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -p|--project-path)
                PROJECT_PATH="$2"
                # Convert to absolute path
                PROJECT_PATH="$(cd "$PROJECT_PATH" 2>/dev/null && pwd || echo "$PROJECT_PATH")"
                shift 2
                ;;
            *)
                echo "❌ Error: Unknown option '$1'"
                echo "Usage: $0 stop <SERVICE_TYPE> [OPTIONS]"
                return 1
                ;;
        esac
    done
    
    stop_service_by_type "$service_type"
    return $?
}

# Function to stop service by type
stop_service_by_type() {
    local service_type="$1"
    echo "🛑 Stopping DAT $service_type server in project: $PROJECT_PATH"
    
    local project_pid_file="$(get_pid_file "$service_type")"
    
    if [[ ! -f "$project_pid_file" ]]; then
        echo "⚠️ No DAT $service_type server instance found in project: $PROJECT_PATH"
        return 0
    fi
    
    local pid=$(cat "$project_pid_file")
    
    # Try graceful shutdown first
    kill "$pid" 2>/dev/null
    
    # Wait for graceful shutdown
    local count=0
    while kill -0 "$pid" 2>/dev/null && [[ $count -lt 10 ]]; do
        sleep 1
        ((count++))
    done
    
    # Force kill if still running
    if kill -0 "$pid" 2>/dev/null; then
        echo "⚠️ Forcing DAT $service_type server shutdown..."
        kill -9 "$pid" 2>/dev/null
    fi
    
    # Clean up project files
    rm -f "$project_pid_file"
    
    echo "✅ DAT $service_type server stopped successfully!"
    return 0
}

# Function to show server status
show_status() {
    local service_type="$1"
    
    # Validate service type
    if ! parse_and_validate_service_type "status" "$service_type"; then
        echo "Example: $0 status openapi -p /my/project"
        return 1
    fi
    
    shift
    
    # Parse remaining arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -p|--project-path)
                PROJECT_PATH="$2"
                # Convert to absolute path
                PROJECT_PATH="$(cd "$PROJECT_PATH" 2>/dev/null && pwd || echo "$PROJECT_PATH")"
                shift 2
                ;;
            *)
                echo "❌ Error: Unknown option '$1'"
                echo "Usage: $0 status <SERVICE_TYPE> [OPTIONS]"
                return 1
                ;;
        esac
    done
    
    show_service_status "$service_type"
    return $?
}

# Function to show service status
show_service_status() {
    local service_type="$1"
    local project_pid_file="$(get_pid_file "$service_type")"
    local project_log_file="$(get_log_file "$service_type")"
    
    if [[ ! -f "$project_pid_file" ]]; then
        echo "❌ DAT $service_type server: Not running"
        return 0
    fi
    
    local service_pid=$(cat "$project_pid_file")
    
    # Check if process is still running
    if ! kill -0 "$service_pid" 2>/dev/null; then
        echo "❌ DAT $service_type server: Process not found (cleaning up)"
        rm -f "$project_pid_file"
        echo "📋 Check log file for details: $project_log_file"
        return 0
    fi
    
    echo "✅ DAT $service_type server: Running (PID: $service_pid)"
}

# Function to show Java help
show_java_help() {
    local service_type="$1"

    if ! check_jar_and_java; then
        return 1
    fi

    # Set Java options
    JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

    if [[ -n "$service_type" ]]; then
        echo "🔍 Getting help for DAT $service_type server..."
        $JAVA_CMD $JAVA_OPTS -jar "$JAR_FILE" server "$service_type" --help
    else
        echo "🔍 Getting general help from DAT server..."
        $JAVA_CMD $JAVA_OPTS -jar "$JAR_FILE" server --help
    fi
}

# Function to handle help command
handle_help() {
    local service_type=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        # Check if it's a known service type
        if validate_service_type "$1"; then
            service_type="$1"
            shift
        else
            echo "❌ Error: Unknown service type '$1'"
            echo ""
            echo "Available service types for help: $(get_service_type_keys)"
            echo "Usage: $0 help [SERVICE_TYPE]"
            echo "Examples:"
            echo "  $0 help          # Show general script help"
            echo "  $0 help openapi  # Show OpenAPI server specific help"
            return 1
        fi
    done
    
    if [[ -n "$service_type" ]]; then
        show_java_help "$service_type"
    else
        show_help
    fi
}

# Function to show script help
show_help() {
    echo ""
    echo "DAT Server Management Tool"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Server Management Commands:"
    echo "  start <SERVICE_TYPE> [OPTIONS]   Start server in background (SERVICE_TYPE required)"
    echo "  stop <SERVICE_TYPE> [OPTIONS]    Stop server by service type (SERVICE_TYPE required)"
    echo "  status <SERVICE_TYPE> [OPTIONS]  Show server status (SERVICE_TYPE required)"
    echo "  help [SERVICE_TYPE]              Show script help or specific service help"
    echo ""
    echo "Available Service Types:"
    
    for service_type in $(get_service_type_keys); do
        local description=$(get_service_type_description "$service_type")
        printf "  %-10s                       %s\n" "$service_type" "$description"
    done
    
    echo ""
    echo "Global Options:"
    echo "  -p, --project-path PATH          Specify project path (default: current directory)"
    echo ""
    echo "Start Examples:"
    
    local first_service=$(get_service_type_keys | awk '{print $1}')
    echo "  $0 start $first_service                 # Start $first_service server"
    echo "  $0 start $first_service -P 8080         # Start $first_service server on port 8080"
    echo "  $0 start $first_service -H 0.0.0.0      # Start $first_service server on all interfaces"
    echo "  $0 start $first_service -p /my/project  # Start $first_service server in specific project directory"
    
    for service_type in $(get_service_type_keys); do
        if [[ "$service_type" != "$first_service" ]]; then
            local description=$(get_service_type_description "$service_type")
            echo "  $0 start $service_type                     # Start $description"
        fi
    done
    
    echo ""
    echo "Stop Examples:"
    echo "  $0 stop $first_service                  # Stop $first_service service"
    echo "  $0 stop $first_service -p /my/project   # Stop $first_service service in specific project"
    
    for service_type in $(get_service_type_keys); do
        if [[ "$service_type" != "$first_service" ]]; then
            local description=$(get_service_type_description "$service_type")
            echo "  $0 stop $service_type                      # Stop $description"
            echo "  $0 stop $service_type -p /my/project       # Stop $description in specific project"
        fi
    done
    
    echo ""
    echo "Status Examples:"
    echo "  $0 status $first_service                # Show $first_service service status"
    echo "  $0 status $first_service -p /my/project # Show $first_service service status in specific project"
    
    for service_type in $(get_service_type_keys); do
        if [[ "$service_type" != "$first_service" ]]; then
            local description=$(get_service_type_description "$service_type")
            echo "  $0 status $service_type                    # Show $description status"
            echo "  $0 status $service_type -p /my/project     # Show $description status in specific project"
        fi
    done
    
    echo ""
    echo "Help Examples:"
    echo "  $0 help                          # Show this script help"
    echo "  $0 help $first_service                  # Show $first_service server specific help and options"
    
    for service_type in $(get_service_type_keys); do
        if [[ "$service_type" != "$first_service" ]]; then
            local description=$(get_service_type_description "$service_type")
            echo "  $0 help $service_type                      # Show $description specific help"
        fi
    done
    
    echo ""
    echo "Notes:"
    echo "  - Each project directory can run multiple service types simultaneously"
    echo "  - Each service type can only have one instance per project directory"
    echo "  - PID and log files are stored in .dat directory within the project directory"
    echo ""
    echo "For detailed server options and subcommands, use: $0 help [SERVICE_TYPE]"
    echo ""
}

# Main command handling
case "${1:-help}" in
    start)
        shift
        start_server "$@"
        ;;
    stop)
        shift
        stop_server "$@"
        ;;
    status)
        shift
        show_status "$@"
        ;;
    help)
        shift
        handle_help "$@"
        ;;
    *)
        echo "❌ Unknown command: $1"
        show_help
        exit 1
        ;;
esac
