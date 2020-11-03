package main

func random() {

}

func main() {
	var powS = []Editable{
		{ i : 1 }, { i : 2 }, { i : 3 }, { i : 4 },
	}

	for _, pow := range powS {
		random()
		println(pow)
	}
}