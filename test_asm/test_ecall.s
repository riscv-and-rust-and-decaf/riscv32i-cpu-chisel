_start:
	li x1, 14
	li x1, 13       
	j reset_vector  
	li x1,12        

trap_vector:
	j wtf           
	li x1,11        
	li x1,10        

reset_vector:
	auipc x7, 0     
	addi x7, x7, -12 
	csrw mtvec, x7 
	csrrsi x0,mstatus, 15
	ecall           
	li x1, 9        
	li x1, 8        
	li x1, 7
	li x1,6
	li x1,5

wtf:
	li x2, 2
	li x3, 3
	add x5,x2,x3
	add x8,x5,x3
	add x11,x8,x3
	j end

end1:
	li x30, 30
end:
	csrr x20, mcause
	beq x11, x20, end1
	li x1, 4
	li x1,3
	nop
	nop
	nop
	

	
