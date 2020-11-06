package main

func increment(e *Editable) {
	e.i += 1
}

func main() {
	var powS = []Editable{
		{ i : 1 }, { i : 2 }, { i : 3 }, { i : 4 },
	}

	for _, pow := range powS {
		increment(&pow)
	}
}