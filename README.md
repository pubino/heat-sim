# Heat Diffusion Simulator

An interactive Java application for simulating the 2D heat equation on a rectangular plate. Originally developed by Professor Robert Vanderbei, Princeton University (1997).

## Overview

This educational tool allows users to:
- Simulate heat diffusion on a 2D rectangular plate
- Set boundary temperatures on all four edges
- Visualize temperature distribution with color coding
- Watch the system evolve toward thermal equilibrium

## Features

- **Configurable Boundaries**: Set temperatures for top, bottom, left, and right edges
- **Initial Plate Temperature**: Set the starting temperature of the plate interior
- **Adjustable Resolution**: Configure X and Y panel counts for finer detail
- **Real-time Visualization**: Color-coded display (red=hot, blue=cold)
- **Start/Stop Control**: Run and pause the simulation at any time
- **Output Panel**: Shows initial conditions and temperature bounds

## Requirements

- Java 14+ (for running the JAR)
- Java 14+ with `jpackage` (for building macOS app bundle)

## Building

```bash
# Build the JAR file
make jar

# Build macOS .app bundle (unsigned)
make app
```

## Running

```bash
# Run the JAR
make run

# Or run directly
java -jar dist/HeatSim.jar
```

## Usage

### Setting Up a Simulation

1. Enter boundary temperatures:
   - **Top Temperature**: Temperature along the top edge
   - **Bottom Temperature**: Temperature along the bottom edge
   - **Left Temperature**: Temperature along the left edge
   - **Right Temperature**: Temperature along the right edge

2. Set initial conditions:
   - **Plate Temperature**: Starting temperature of the interior

3. Configure resolution:
   - **X Panels**: Number of cells along the x-axis
   - **Y Panels**: Number of cells along the y-axis

4. Click **Begin Heating** to start the simulation
5. Click **Stop Heating** to pause

### Visual Indicators

- **Red cells**: High temperature (near maximum)
- **Blue cells**: Low temperature (near minimum)
- **Purple cells**: Intermediate temperatures
- **Max/Min labels**: Show temperature range
- **Mid label**: Shows temperature at center point
- **Time counter**: Displays iteration count

## Project Structure

```
heat-sim/
├── heat/
│   └── Heat.java           # Main app and HeatCanvas
├── myutil/
│   ├── AppletFrame.java    # Applet-to-Frame wrapper
│   ├── GL.java             # Graphics library utilities
│   ├── SL.java             # String parsing utilities
│   ├── GridPanel.java      # Grid layout panel
│   └── Format.java         # Number formatting
├── Makefile                # Build configuration
└── .github/workflows/      # CI/CD configuration
```

## Physics

The simulation solves the 2D heat equation using Jacobi iteration:

**Heat Equation**: ∂u/∂t = α(∂²u/∂x² + ∂²u/∂y²)

**Discrete Update**: u[i][j] = (u[i+1][j] + u[i-1][j] + u[i][j+1] + u[i][j-1]) / 4

Each interior cell is updated to be the average of its four neighbors, causing heat to diffuse from hot regions to cold regions until equilibrium is reached.

## Example Configurations

**Hot Top, Cold Bottom**:
- Top: 100, Bottom: 0, Left: 50, Right: 50
- Creates a vertical temperature gradient

**Hot Spot**:
- All boundaries: 0, Plate: 100
- Watch heat dissipate from the center

**Asymmetric**:
- Top: 100, Bottom: 0, Left: 0, Right: 50
- Creates interesting diagonal gradients
