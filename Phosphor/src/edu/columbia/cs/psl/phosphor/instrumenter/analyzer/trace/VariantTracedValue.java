package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.trace;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph.NaturalLoop;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Collections;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;

class VariantTracedValue extends TracedValue {

    private final Set<NaturalLoop<BasicBlock>> variantLoops;

    VariantTracedValue(int size, Set<AbstractInsnNode> instructions, Set<NaturalLoop<BasicBlock>> variantLoops) {
        super(size, instructions);
        this.variantLoops = Collections.unmodifiableSet(variantLoops);
    }

    @Override
    TracedValue newInstance(int size, Set<AbstractInsnNode> instructions) {
        return new VariantTracedValue(size, instructions, variantLoops);
    }

    Set<NaturalLoop<BasicBlock>> getVariantLoops() {
        return variantLoops;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof VariantTracedValue) || !super.equals(o)) {
            return false;
        }
        VariantTracedValue that = (VariantTracedValue) o;
        return variantLoops != null ? variantLoops.equals(that.variantLoops) : that.variantLoops == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (variantLoops != null ? variantLoops.hashCode() : 0);
        return result;
    }
}
