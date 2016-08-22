chars = []
print('Enter the name of your file')
name = input()
with open(name, 'r') as infile:
	for line in infile:
		chars = chars + line.strip().split(':')
s = ''.join(chars)
print(len(s))
with open('keyapdu.txt', 'w') as outfile:
	print(s, file=outfile)
