# Go For - Range Copy - Editing 
A [GoLand](https://www.jetbrains.com/go/) plugin to help developers detect an edge case: editing a range assigned variable.

# Motivation
Go dictates range clause to copy collection before iterating.

Consider the following go code
```go
package main
import "fmt"
type Editable struct{ i int }

func pumpUp(e *Editable)  {
 e.i = 100
}

func main() {
 es := []Editable { { i : 1 }, { i : 2 }, { i : 3 }, { i : 4 } }
 for _, e := range es {
 	pumpUp(&e)
 }
 fmt.Printf("%v", es)
}
```

below outlines the output in Go, Swift, Objective-C, Java (keeping the semantics same)
|  Language  | Output                     |
| ---------- |:---------------------------|
| **Go**     |  [{1} {2} {3} {4}]         |
| Swift      |  [{100} {100} {100} {100}] |
| Java       |  [{100} {100} {100} {100}] |
| Objective-C|  [{100} {100} {100} {100}] |

This plugin tries to catch such use cases and highlight them for the developer to reconsider the logic

## Supported

1. Detect usages of pointer references of assigned variable in a for block
2. Detect editable method references of assigned variable in the block

## To be supported

1. Assigned variable is predeclared before the range clause
2. Direct assignment of the struct's properties

\* please see issues for more

# Output

![Highlighted Text][file:highlighted_output.png]

### References
Assigned Variable = e in the above go for block

Gradle Scan: https://scans.gradle.com/s/vywyvtekrzqfg <img src="https://gradle.com/wp-content/themes/fuel/assets/img/branding/gradle-elephant-icon-white.svg" alt="Gradle Scan" width="50">

*Reference: [Plugin Extension Points in IntelliJ SDK Docs][docs:ep]*

[docs:ep]: https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_extensions.html
[file:highlighted_output.png]: .github/readme/output.png
