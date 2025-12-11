# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Vaadin 25.0.0-beta9 + Spring Boot 4.0.0 application built with Java 21. The project currently contains a slot machine-style game called "MONARCH" as a demonstration. The application follows a feature-based package structure rather than traditional layered architecture.

## Technology Stack

- **Framework**: Vaadin 25 (Java-based UI framework)
- **Backend**: Spring Boot 4.0.0
- **Java Version**: 21
- **Build Tool**: Maven (use `./mvnw` wrapper, not system Maven)
- **Theme**: Lumo (with custom styles in `src/main/resources/META-INF/resources/styles.css`)

## Development Commands

### Running the Application

```bash
./mvnw
```

This starts the application in development mode with hot reload enabled. The default port is 8080, and the browser will launch automatically (`vaadin.launch-browser=true`).

### Building for Production

```bash
./mvnw clean package -Pproduction
```

This activates the production profile which:
- Enables Vaadin production mode with optimized frontend bundle
- Excludes development dependencies
- Creates a production-ready JAR

### Building Docker Image

```bash
docker build -t monarch:latest .
```

For commercial Vaadin components (if needed):

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```

### Running Tests

```bash
./mvnw test
```

## Architecture

### Package Structure

The project uses **feature-based packaging** where code is organized by functional units:

- `com.infraleap.base.ui` - Reusable UI components and layouts shared across features
- `com.infraleap.examplefeature` - Example feature package (currently contains MonarchView game)
  - `ui/` - View components (routes)
  - Feature logic, services, repositories, and entities are co-located in the same package

Each feature package is self-contained with its UI, business logic, data access, and tests together.

### Key Files

- `Application.java` - Spring Boot entry point, configures Vaadin with `@Push` for server push, Lumo theme, and custom styles
- `MonarchView.java` - Current root route (`@Route("")`) showing the slot machine game

### Vaadin Architecture

This is a **Java-based Vaadin application** (not React-based). UI components are created in Java using Vaadin's component API:

- Views extend Vaadin layouts (`VerticalLayout`, `HorizontalLayout`, etc.)
- Routes are defined with `@Route` annotation
- Server push is enabled via `@Push` on Application class for real-time updates
- CSS styling uses Vaadin's shadow DOM parts API (e.g., `::part(row)`, `::part(cell)`)

### Current Implementation Details

The MonarchView demonstrates:
- Scheduled background tasks with `ScheduledExecutorService`
- Vaadin's `UI.access()` pattern for thread-safe UI updates from background threads
- Proper cleanup of resources in `addDetachListener()`
- JavaScript execution via `element.executeJs()` for shadow DOM manipulation
- Grid component customization with CSS parts

## Build Profiles

### Development Mode (Default)

Running `./mvnw` or `./mvnw spring-boot:run` starts the application in development mode with:
- Hot reload enabled
- Vaadin DevTools available
- Frontend resources served in development mode

### Production Mode

Running `./mvnw clean package -Pproduction` activates the production profile with:
- Vaadin production mode enabled (`productionMode=true`)
- Optimized and minified frontend bundle
- Development dependencies excluded
- Production-ready JAR artifact

## Frontend Resources

- Generated frontend files are in `src/main/frontend/generated/` (ignored in git)
- Custom styles: `src/main/resources/META-INF/resources/styles.css`
- Frontend is built automatically by `vaadin-maven-plugin` during Maven lifecycle

## Vaadin-Specific Notes

- When using Vaadin MCP tools, specify `ui_language: "java"` (not React)
- Vaadin components use shadow DOM - style them with `::part()` selectors
- Background threads must use `UI.access()` to update UI components
- Always clean up schedulers/executors in detach listeners to prevent memory leaks

## Development Configuration

From `application.properties`:
- Server runs on port 8080 (configurable via `PORT` env var)
- Allowed packages for development: `com.vaadin,org.vaadin,com.flowingcode,com.infraleap`
- Browser auto-launch is enabled for development convenience
