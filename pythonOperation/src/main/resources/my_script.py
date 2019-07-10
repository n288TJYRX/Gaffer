# Sample taken from pyStrich GitHub repository
# https://github.com/mmulqueen/pyStrich
from pystrich.datamatrix import DataMatrixEncoder
import sys
input0 = sys.argv[0]
print('input 0',input0)
input1 = sys.argv[1]
print('input 1',input1)
print('something was printed')
# sys.stdout = 'does this do anything?'

encoder = DataMatrixEncoder('This is a DataMatrix.')
encoder.save('./datamatrix_test.png')
print(encoder.get_ascii())
# return 'returned something'