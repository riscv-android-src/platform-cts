.source "T_if_ne_11.java"
.class  public Ldot/junit/opcodes/if_ne/d/T_if_ne_11;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ljava/lang/Object;-><init>()V
       return-void
.end method

.method public run(II)Z
.registers 8

       if-ne v6, v7, :Label10
       const/4 v6, 0
       return v6

:Label10
       const v6, 0
       nop
       return v6
.end method
