package me.checkium.outofpaint.processors;

import me.checkium.outofpaint.OutOfPaint;
import me.checkium.outofpaint.utils.methodexecutor.executor.Context;
import me.checkium.outofpaint.utils.methodexecutor.executor.MethodExecutor;
import me.checkium.outofpaint.utils.methodexecutor.executor.defined.JVMComparisonProvider;
import me.checkium.outofpaint.utils.methodexecutor.executor.defined.JVMMethodProvider;
import me.checkium.outofpaint.utils.methodexecutor.executor.defined.MappedFieldProvider;
import me.checkium.outofpaint.utils.methodexecutor.executor.providers.DelegatingProvider;
import me.checkium.outofpaint.utils.methodexecutor.executor.values.JavaValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeobfuscateStringsProcessor {

    public void proccess(List<ClassNode> classNodes) {
        Map<String, ClassNode> classNames = new HashMap<>();
        for (ClassNode classNode : classNodes) {
            classNames.put(classNode.name, classNode);
        }
        Map<String, MethodNode> decryptMethodCache = new HashMap<>();
        for (ClassNode classNode : classNames.values()) {
            for (MethodNode method : classNode.methods) {
                for (AbstractInsnNode abstractInsnNode : method.instructions.toArray()) {
                    if (abstractInsnNode instanceof LdcInsnNode && ((LdcInsnNode) abstractInsnNode).cst instanceof String) {
                        String encryptedString = (String) ((LdcInsnNode) abstractInsnNode).cst;
                        String decryptKey = classNode.name.replaceAll("/", ".") + method.name.replaceAll("/", ".");
                        System.out.println("Found string " + encryptedString);
                        System.out.println("Looking for decryption method...");
                        MethodInsnNode decryptCall;
                        if (abstractInsnNode.getNext() instanceof MethodInsnNode) {
                            decryptCall = (MethodInsnNode) abstractInsnNode.getNext();
                            if (decryptCall.getOpcode() == Opcodes.INVOKESTATIC && decryptCall.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
                                String decryptMethodOwner = decryptCall.owner;
                                String decryptMethodName = decryptCall.name;
                                ClassNode decryptClass = classNames.get(decryptMethodOwner);
                                if (decryptClass != null) {
                                    for (MethodNode decryptClassMethod : decryptClass.methods) {
                                        if (decryptClassMethod.name.equals(decryptMethodName) && decryptClassMethod.desc.equals(decryptCall.desc)) {
                                            String methodLoc = decryptMethodOwner + " " + decryptMethodName;
                                            System.out.println("Found decrypt method " + methodLoc);
                                            if (decryptMethodCache.containsKey(methodLoc)) {
                                                System.out.println("Method is cached, decrypting...");
                                                OutOfPaint.log(encryptedString + " -> " + getString(encryptedString, decryptClass, decryptMethodCache.get(methodLoc), decryptKey));
                                            } else {
                                                System.out.println("Method is not cached, transforming method...");
                                                int current = 0;
                                                for (AbstractInsnNode insnNode : decryptClassMethod.instructions.toArray()) {
                                                    if (current > 0) decryptClassMethod.instructions.remove(insnNode);
                                                    if (insnNode instanceof MethodInsnNode) {
                                                        if (((MethodInsnNode) insnNode).name.equals("toString")) break;
                                                    }
                                                    current++;
                                                }
                                                InsnList toAdd = new InsnList();
                                                toAdd.add(new LdcInsnNode("toChange"));
                                                decryptClassMethod.instructions.insert(decryptClassMethod.instructions.getFirst(), toAdd);
                                                decryptMethodCache.put(methodLoc, decryptClassMethod);
                                                OutOfPaint.log(encryptedString + " -> " + getString(encryptedString, decryptClass, decryptMethodCache.get(methodLoc), decryptKey));
                                            }
                                        }
                                    }
                                } else {
                                    System.out.println("Couldn't find class " + decryptMethodOwner);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getString(String encryptedString, ClassNode decrypterClass, MethodNode decrypterMethod, String decryptKey) {
        decrypterMethod.instructions.remove(decrypterMethod.instructions.getFirst().getNext());
        decrypterMethod.instructions.insert(decrypterMethod.instructions.getFirst(), new LdcInsnNode(decryptKey));

        DelegatingProvider provider = new DelegatingProvider();
        provider.register(new JVMMethodProvider());
        provider.register(new JVMComparisonProvider());
        //provider.register(new MappedMethodProvider(classes));
        provider.register(new MappedFieldProvider());
        Context context = new Context(provider);

        return MethodExecutor.execute(decrypterClass, decrypterMethod, Collections.singletonList(JavaValue.valueOf(encryptedString)), null, context);
    }
}
