.source "T_shr_int_1.java"
.class  public Ldot/junit/opcodes/shr_int/d/T_shr_int_1;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ljava/lang/Object;-><init>()V
       return-void
.end method

.method public run(II)I
.registers 8

       shr-int v0, v6, v7
       return v0
.end method
