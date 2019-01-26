package me.checkium.outofpaint.processors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static me.checkium.outofpaint.OutOfPaint.log;

public class UnprotectProcessor {

    public void proccess(Collection<ClassNode> classNodes) {
        Map<String, ClassNode> classNames = new HashMap<>();
        for (ClassNode classNode : classNodes) {
            classNames.put(classNode.name, classNode);
        }

        MethodNode onEnable = findOnEnable(classNodes);
        if (onEnable != null) {
            String classOneName = getNextInstance(onEnable);
            if (classOneName != null) {
                ClassNode classOne = classNames.get(classOneName);
                if (classOne != null) {
                    String classTwoName = getNextInstance(classOne);
                    ClassNode classTwo;
                    if (classTwoName != null && (classTwo = classNames.get(classTwoName)) != null) {
                        String licenseMethodName = null;
                        String licenseMethodDesc = null;
                        String mainVarName = null;
                        for (MethodNode method : classTwo.methods) {
                            if (method.name.equals("<init>")) {
                                for (AbstractInsnNode abstractInsnNode : method.instructions.toArray()) {
                                    if (abstractInsnNode instanceof MethodInsnNode && abstractInsnNode.getOpcode() == Opcodes.INVOKESPECIAL) {
                                        if (!((MethodInsnNode) abstractInsnNode).name.equals("<init>")) {
                                            licenseMethodName = ((MethodInsnNode) abstractInsnNode).name;
                                            licenseMethodDesc = ((MethodInsnNode) abstractInsnNode).desc;
                                        }
                                    } else if (abstractInsnNode instanceof FieldInsnNode && abstractInsnNode.getOpcode() == Opcodes.PUTFIELD) {
                                        if (!((FieldInsnNode) abstractInsnNode).desc.equals("Z")) {
                                            mainVarName = ((FieldInsnNode) abstractInsnNode).name;
                                        }
                                    }
                                }
                            }
                        }
                        if (licenseMethodName != null) {
                            for (MethodNode method : classTwo.methods) {
                                if (method.name.equals(licenseMethodName) && method.desc.equals(licenseMethodDesc)) {
                                    System.out.println("Class name: " + classTwo.name);
                                    System.out.println("License method: " + method.name);
                                    System.out.println("Main var: " + mainVarName);
                                    // Find starter class
                                    String starterClassName = null;
                                    for (AbstractInsnNode abstractInsnNode : method.instructions.toArray()) {
                                        if (abstractInsnNode instanceof TypeInsnNode) {
                                            ClassNode node = classNames.get(((TypeInsnNode) abstractInsnNode).desc);
                                            if (node != null) {
                                                for (MethodNode methodNode : node.methods) {
                                                    if (methodNode.name.equals("<init>") && methodNode.desc.startsWith("(L" + findMainClass(classNodes).name)) {
                                                        starterClassName = ((TypeInsnNode) abstractInsnNode).desc;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    System.out.println(starterClassName);

                                    // Unprotect
                                    method.instructions.clear();
                                    method.maxLocals = 1;
                                    method.maxStack = 4;
                                    method.tryCatchBlocks.clear();
                                    InsnList toAdd = new InsnList();
                                    toAdd.add(new LabelNode());

                                    toAdd.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/bukkit/Bukkit", "getConsoleSender", "()Lorg/bukkit/command/ConsoleCommandSender;", false));
                                    toAdd.add(new LdcInsnNode("§4§lWARNING: This plugin has been patched using OutOfPaint and shouldn't be used in production."));
                                    toAdd.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "org/bukkit/command/ConsoleCommandSender", "sendMessage", "(Ljava/lang/String;)V", true));

                                    toAdd.add(new TypeInsnNode(Opcodes.NEW, starterClassName));
                                    toAdd.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                    toAdd.add(new FieldInsnNode(Opcodes.GETFIELD, classTwo.name, mainVarName, "L" + findMainClass(classNodes).name + ";"));
                                    toAdd.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                    toAdd.add(new LdcInsnNode("OutOfPaint"));
                                    toAdd.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, starterClassName, "<init>", "(L" + findMainClass(classNodes).name + ";L" + classTwo.name + ";Ljava/lang/String;)V", false));
                                    toAdd.add(new InsnNode(Opcodes.RETURN));
                                    toAdd.add(new LabelNode());
                                    method.instructions.add(toAdd);

                                    // Unprotect starter
                                    ClassNode starterClass = classNames.get(starterClassName);
                                    for (MethodNode methodNode : starterClass.methods) {
                                        if (methodNode.desc.equals("(Ljava/lang/String;)V") && !methodNode.name.equals("<init>")) {
                                            for (AbstractInsnNode abstractInsnNode : methodNode.instructions.toArray()) {
                                                if (abstractInsnNode instanceof MethodInsnNode && ((MethodInsnNode) abstractInsnNode).name.equals("equals")) {
                                                    methodNode.instructions.remove(abstractInsnNode.getPrevious().getPrevious());
                                                    methodNode.instructions.remove(abstractInsnNode.getPrevious());
                                                    methodNode.instructions.insertBefore(abstractInsnNode, new LdcInsnNode("OutOfPaint"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            log("Couldn't find onEnable, unprotecting failed");
        }
    }

    private String getNextInstance(ClassNode cnode) {
        MethodNode node = cnode.methods.stream().filter(methodNode -> methodNode.name.equals("<init>")).findAny().orElse(null);
        return node != null ? getNextInstance(node) : null;
    }

    private String getNextInstance(MethodNode node) {
        for (AbstractInsnNode abstractInsnNode : node.instructions.toArray()) {
            if (abstractInsnNode.getOpcode() == Opcodes.NEW) {
                return ((TypeInsnNode) abstractInsnNode).desc;
            }
        }
        return null;
    }

    private ClassNode findMainClass(Collection<ClassNode> classes) {
        for (ClassNode aClass : classes) {
            if (aClass.superName.equals("org/bukkit/plugin/java/JavaPlugin")) {
                return aClass;
            }
        }
        return null;
    }

    private MethodNode findOnEnable(Collection<ClassNode> classes) {
        ClassNode node = findMainClass(classes);
        if (node != null) {
            for (MethodNode method : node.methods) {
                if (method.name.equals("onEnable")) {
                    return method;
                }
            }
        }
        return null;
    }
}
