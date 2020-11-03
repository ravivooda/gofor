package main

func (e *Editable) incrementI() {
	e.i += 1
}

func main() {
	var powS = []Editable{
		{ i : 1 }, { i : 2 }, { i : 3 }, { i : 4 },
	}

	for _, pow := range powS {
		pow.incrementI()
	}
}