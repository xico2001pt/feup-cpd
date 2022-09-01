#include <stdlib.h>

class Matrix {
private:
    double * ptr;
    unsigned int size;
public:
    Matrix(unsigned int size);

    ~Matrix();
    
    double get(unsigned int col, unsigned int row);
    
    void set(unsigned int col, unsigned int row, double value);

    void split(unsigned int block_size); // TODO: LATER?
};

Matrix::Matrix(unsigned int size) {
    ptr = (double *) malloc((size * size) * sizeof(double));
    this->size = size;
}

Matrix::~Matrix() {
    free(ptr);
}

double Matrix::get(unsigned int col, unsigned int row){
    if(col < this->size && row < this->size && col >= 0 && row >= 0){
        return ptr[col+row*size];
    }
    // TODO: Throw an proper exception
    throw "Invalid index";
}

void Matrix::set(unsigned int col, unsigned int row, double value) {
    if(col < this->size && row < this->size && col >= 0 && row >= 0){
        ptr[col+row*size] = value;
    }
    // TODO: Throw an proper exception
    throw "Invalid index";
}


