import time

def get_int_from_stdin():
    try:
        return int(input())
    except ValueError:
        print("Please insert an integer: ", end="")
        return get_int_from_stdin()

def initialize_matrixes(size):
    ma = [[1 for _ in range(size)] for _ in range(size)]
    mb = [[i + 1 for _ in range(size)] for i in range(size)]
    mc = [[0 for _ in range(size)] for _ in range(size)]
    return ma, mb, mc

def onMult(size):
    ma, mb, mc = initialize_matrixes(size)

    for i in range(size):
        for j in range(size):
            for k in range(size):
                mc[i][j] += ma[i][k] * mb[k][j]
    
    return mc

def onMultLine(size):
    ma, mb, mc = initialize_matrixes(size)

    for i in range(size):
        for k in range(size):
            for j in range(size):
                mc[i][j] += ma[i][k] * mb[k][j]
    
    return mc

def onMultBlock(size):
    return

# Switch-case Alternative
options = {1: onMult, 2: onMultLine, 3: onMultBlock}

def main():
    # Selection Menu
    print("1. Multiplication\n2. Line Multiplication\n3. Block Multiplication")
    selection = 0
    while selection < 1 or selection > 3:
        print("Insert a valid option: ", end="")
        selection = get_int_from_stdin()
    
    # Size Selection
    print("Matrix size (lines = columns): ", end="")
    size = get_int_from_stdin()

    # Algorithm Choosen
    start = time.time()
    res = options[selection](size)
    end = time.time()
    print(f"Time: {end-start:.3f} seconds")
    print(res[0][:10])

if __name__ == "__main__":
    main()
