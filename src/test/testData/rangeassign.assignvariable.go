package main

func main() {
	var powS = []Editable{
		{ i : 1 }, { i : 2 }, { i : 3 }, { i : 4 },
	}

	var pow = Editable{}
	for _, pow = range powS {
		increment(&pow)
	}

	for range powS {

	}
}