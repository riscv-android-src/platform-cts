.source "T_shr_int_lit8_8.java"
.class  public Ldot/junit/opcodes/shr_int_lit8/d/T_shr_int_lit8_8;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ljava/lang/Object;-><init>()V
       return-void
.end method

.method public run(D)I
.registers 8

       shr-int/lit8 v0, v6, 2
       return v0
.end method
