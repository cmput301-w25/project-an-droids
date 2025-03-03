updateX:
	addi sp, sp, -20 # Allocating storage for registers in stack pointer
	# Storing the return address and registers for the function
	sw ra, 0(sp) 
	sw s0, 4(sp)
	sw s1, 8(sp)
	sw s2, 12(sp)
	sw s3, 16(sp)
	# Moving the argument values to callee saved registers
	mv s0, a0 # s0 = X
	mv s1, a1 # s1 = A
	mv s2, a2 # s2 = B
	# Initializing loop counter i
	li s3, 1
loop:
	slli t0, s3, 2	# t0 = i * 4
	add t1, s1, t0 # t1 = A + (i*4) = A[i]
	lw t2, 0(t1) # t2 = A[i]
	addi t3, s3, -1 # t3 = i-1
	slli t3, t3, 2 # t3 = t3 * 4
	add t4, s1, t3 # t4 = A + ((i-1)*4) = A[i-1]
	lw t5, 0(t4) # t5 = A[i-1]
	bge t2, t5, endLoop # if (A[i] >= A[i-1]), endLoop
	add t6, s0, t0 # t6 = X + (i*4) = X[i]
	lw t1, 0(t6) # t1 = X[i]
	lw t4, 0(t1) # t4 = *(X[i])
	sub t5, t5, t2 # t5 = A[i-1] - A[i]
	slli t5, t5, 2 # t5 = (A[i-1] - A[i]) * 4
	add t5, s2, t5 # t5 = B + ((A[i-1] - A[i]) * 4) = B[A[i-1] - A[i]]
	lw t5, 0(t5) # t5 = *(B[A[i-1] - A[i]])
	add t4, t4, t5 # t4 = *(X[i]) + *(B[A[i-1] - A[i]])
	sw t4, 0(t1) 
	addi s3, s3, 1 # i++
	j loop
endLoop:
	# Restoring all the registers and the return address
	lw ra, 0(sp)
	lw s0, 4(sp)
	lw s1, 8(sp)
	lw s2, 12(sp)
	lw s3, 16(sp)
	addi sp, sp, 20
	ret