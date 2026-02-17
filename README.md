# CalcCubes

CalcCubes is a Kotlin-based Android application that expands on the MathDice game concept by generating and ranking mathematical solutions in real time.

Given a set of dice values, the app computes the top 1000 closest mathematical expressions to a target number across configurable difficulty levels and operator sets.

## Features

- Supports variable dice counts (2–5 dice)
- Configurable difficulty levels:
  - Addition & subtraction
  - Multiplication & division
  - Powers and roots
- Real-time generation and ranking of solutions
- Top-K solution tracking (top 1000 closest results)
- Random dice roll mode (game mode) or custom user input (verification mode)
- Parallelized computation using Kotlin coroutines

## Technical Overview

CalcCubes implements a custom mathematical expression engine built on an Abstract Syntax Tree (AST) model using Kotlin sealed classes.

Key design elements:

- **Combinatorial expression generation** across partitions of dice inputs
- **Canonicalization of expressions** to normalize equivalent forms
- **Memoization and deduplication** to reduce redundant computations
- **Top-K ranking strategy** based on absolute difference from target
- **Parallel computation** using Kotlin coroutines for performance

The canonicalization process ensures that mathematically equivalent expressions (e.g., `2 + 3` and `3 + 2`) are normalized into a consistent representation, significantly reducing search space explosion.

## Why This Project

This project explores algorithmic problem solving, combinatorial search optimization, and performance-aware application design in a constrained mobile environment.

It was built as a personal challenge to:

- Explore expression synthesis and evaluation
- Optimize combinatorial search space
- Apply structured AST modeling in Kotlin
- Experiment with concurrency in Android
- For fun and to be able to check myself if my answers to a MathDice game were the closest possible answers

## Tech Stack

- Kotlin
- Android SDK
- Kotlin Coroutines
- Gradle

## Future Improvements

- Heuristic pruning strategies for deeper search spaces
- UI/UX refinements
- Performance benchmarking for higher dice counts
- Expanded operator sets

## Author

Heather Koyuk

## Acknowledgment

CalcCubes is inspired by the MathDice game created by ThinkFun.  
This project is an independent software implementation and expansion of the game’s mathematical puzzle concept and is not affiliated with or endorsed by ThinkFun.
