package com.ft.plugin.garble.bytecode;

import com.ft.plugin.garble.FTHookConfig;
import com.ft.plugin.garble.FTMethodCell;
import com.ft.plugin.garble.FTMethodType;
import com.ft.plugin.garble.FTSubMethodCell;
import com.ft.plugin.garble.FTTransformHelper;
import com.ft.plugin.garble.FTUtil;
import com.ft.plugin.garble.Logger;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * BY huangDianHua
 * DATE:2019-11-29 15:04
 * Description:
 */
public class FTMethodAdapter extends AdviceAdapter {
    private HashMap<String, FTMethodCell> mLambdaMethodCells = new HashMap<>();
    private FTTransformHelper ftTransformHelper;
    private String[] interfaces;
    private String className;
    private String superName;
    private String methodName;
    private String eventName = null;
    private String eventProperties = null;
    private boolean isHasTracked = false;
    private int variableID = 0;
    //nameDesc是"onClick(Landroid/view/View;)V"字符串
    private boolean isOnClickMethod = false;
    private boolean isOnItemClickMethod = false;
    //name + desc
    private String nameDesc;
    private boolean isSetUserVisibleHint = false;

    //访问权限是public并且非静态
    private boolean pubAndNoStaticAccess;
    private ArrayList<Integer> localIds;

    public FTMethodAdapter(MethodVisitor mv, int access, String name, String desc, String className,String[] interfaces,String supperName,FTTransformHelper ftTransformHelper) {
        super(FTUtil.ASM_VERSION, mv, access, name, desc);
        this.methodName = name;
        this.superName = supperName;
        this.ftTransformHelper = ftTransformHelper;
        this.className = className;
        this.interfaces = interfaces;
        Logger.info(">>>> 开始扫描类 <"+className+"> 的方法:"+methodName+"<<<<");
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        if (isHasTracked) {
            if (ftTransformHelper.extension.lambdaEnabled && mLambdaMethodCells.containsKey(nameDesc)) {
                mLambdaMethodCells.remove(nameDesc);
            }
            Logger.info("Hooked Class<"+className+">的 method: "+methodName+","+methodDesc);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name1, String desc1, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name1, desc1, bsm, bsmArgs);
        if (!ftTransformHelper.extension.lambdaEnabled) {
            return;
        }
        try {
            String desc2 = (String) bsmArgs[0];
            FTMethodCell ftMethodCell = FTHookConfig.LAMBDA_METHODS.get(Type.getReturnType(desc1).getDescriptor() + name1 + desc2);
            if (ftMethodCell != null) {
                Handle it = (Handle) bsmArgs[1];
                mLambdaMethodCells.put(it.getName() + it.getDesc(), ftMethodCell);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        nameDesc = methodName + methodDesc;
        pubAndNoStaticAccess = FTUtil.isPublic(methodAccess) && !FTUtil.isStatic(methodAccess);
        if ((nameDesc.equals("onClick(Landroid/view/View;)V")) && pubAndNoStaticAccess) {
            isOnClickMethod = true;
            variableID = newLocal(Type.getObjectType("java/lang/Integer"));
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ASTORE, variableID);
        } else if (nameDesc == "onItemClick(Landroid/widget/AdapterView;Landroid/view/View;IJ)V" && pubAndNoStaticAccess) {
            isOnItemClickMethod = true;
            variableID = newLocal(Type.getObjectType("java/lang/Integer"));
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ASTORE, variableID);
        }
        if (ftTransformHelper.extension.canHookMethod) {
            handleCode();
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode);
    }

    void handleCode(FTMethodCell ftMethodCell){
        if(ftMethodCell.subMethodCellList != null && !ftMethodCell.subMethodCellList.isEmpty()){
            for (FTSubMethodCell f:ftMethodCell.subMethodCellList) {
                if(f.type == FTMethodType.ALOAD){
                    mv.visitVarInsn(ALOAD, f.value);
                }else if(f.type == FTMethodType.ILOAD){
                    mv.visitVarInsn(ILOAD, f.value);
                }else if(f.type == FTMethodType.INVOKEVIRTUAL){
                    mv.visitMethodInsn(INVOKEVIRTUAL,f.className,f.agentName,f.agentDesc,f.itf);
                }else if(f.type == FTMethodType.INVOKESPECIAL){
                    mv.visitMethodInsn(INVOKESPECIAL,f.className,f.agentName,f.agentDesc,f.itf);
                }
            }
        }
        mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, ftMethodCell.agentName, ftMethodCell.agentDesc, false);
    }

    void handleCode(){
        /**
         * androidx/fragment/app/Fragment，androidx/fragment/app/ListFragment，androidx/fragment/app/DialogFragment
         */
        if (FTUtil.isInstanceOfXFragment(superName)) {
            //Logger.info("方法扫描>>>类是FragmentX>>>方法是"+nameDesc);
            FTMethodCell ftMethodCell = FTHookConfig.FRAGMENT_X_METHODS.get(nameDesc);
            if (ftMethodCell != null) {
                handleCode(ftMethodCell);
                isHasTracked = true;
                return;
            }
        }

        /**
         * android/support/v4/app/Fragment，android/support/v4/app/ListFragment，android/support/v4/app/DialogFragment，
         */
        if (FTUtil.isInstanceOfV4Fragment(superName)) {
            //Logger.info("方法扫描>>>类是FragmentV4>>>方法是"+nameDesc);
            FTMethodCell ftMethodCell = FTHookConfig.FRAGMENT_V4_METHODS.get(nameDesc);
            if (ftMethodCell != null) {
                handleCode(ftMethodCell);
                isHasTracked = true;
                return;
            }
        }

        /**
         * android/app/Fragment，android/app/ListFragment， android/app/DialogFragment，
         */
        if (FTUtil.isInstanceOfFragment(superName)) {
            //Logger.info("方法扫描>>>类是Fragment>>>方法是"+nameDesc);
            FTMethodCell ftMethodCell = FTHookConfig.FRAGMENT_METHODS.get(nameDesc);
            if (ftMethodCell != null) {
                handleCode(ftMethodCell);
                isHasTracked = true;
                return;
            }
        }

        if(FTUtil.isInstanceOfActivity(superName)){
            FTMethodCell ftMethodCell = FTHookConfig.ACTIVITY_METHODS.get(nameDesc);
            //Logger.info("方法扫描>>>类是Activity>>>方法是"+nameDesc+" ftMethodCell="+ftMethodCell);
            if(ftMethodCell != null){
                handleCode(ftMethodCell);
                isHasTracked = true;
                return;
            }
        }

        /**
         * 在 android.gradle 的 3.2.1 版本中，针对 view 的 setOnClickListener 方法 的 lambda 表达式做特殊处理。
         */
        if (ftTransformHelper.extension.lambdaEnabled) {
            FTMethodCell lambdaMethodCell = mLambdaMethodCells.get(nameDesc);
            if (lambdaMethodCell != null) {
                Logger.info("方法扫描>>>类是"+className+" Lambda>>>方法是"+nameDesc+" lambdaMethodCell="+lambdaMethodCell);
                Type[] types = Type.getArgumentTypes(lambdaMethodCell.desc);
                int length = types.length;
                Type[] lambdaTypes = Type.getArgumentTypes(methodDesc);
                // paramStart 为访问的方法参数的下标，从 0 开始
                int paramStart = lambdaTypes.length - length;
                if (paramStart < 0) {
                    return;
                } else {
                    for (int i = 0; i < length; i++) {
                        if (lambdaTypes[paramStart + i].getDescriptor() != types[i].getDescriptor()) {
                            return;
                        }
                    }
                }
                boolean isStaticMethod = FTUtil.isStatic(methodAccess);
                if (!isStaticMethod) {
                    if (lambdaMethodCell.desc == "(Landroid/view/MenuItem;)Z") {
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitVarInsn(Opcodes.ALOAD, getVisitPosition(lambdaTypes, paramStart, isStaticMethod));
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, FTHookConfig.FT_SDK_API, lambdaMethodCell.agentName, "(Ljava/lang/Object;Landroid/view/MenuItem;)V", false);
                        isHasTracked = true;
                        return;
                    }
                }
                for (int i = paramStart; i < paramStart + lambdaMethodCell.paramsCount; i++) {
                    mv.visitVarInsn(lambdaMethodCell.opcodes.get(i - paramStart), getVisitPosition(lambdaTypes, i, isStaticMethod));
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, FTHookConfig.FT_SDK_API, lambdaMethodCell.agentName, lambdaMethodCell.agentDesc, false);
                isHasTracked = true;
                return;
            }
        }


        if (!pubAndNoStaticAccess) {
            Logger.info("方法扫描>>>类是"+className+">>>方法是"+nameDesc+" 静态和非公共方法");
            return;
        }

        /**
         * 支持 onContextItemSelected(MenuItem item)、onOptionsItemSelected(MenuItem item)、onNavigationItemSelected(MenuItem item)
         */
        if (FTUtil.isTargetMenuMethodDesc(nameDesc)) {
            handleCode(FTHookConfig.MENU_METHODS);
            return;
        }

        if (nameDesc == "onDrawerOpened(Landroid/view/View;)V") {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, "trackDrawerOpened", "(Landroid/view/View;)V", false);
            isHasTracked = true;
            return;
        } else if (nameDesc == "onDrawerClosed(Landroid/view/View;)V") {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, "trackDrawerClosed", "(Landroid/view/View;)V", false);
            isHasTracked = true;
            return;
        }

        if (isOnClickMethod && className == "android/databinding/generated/callback/OnClickListener") {
            trackViewOnClick(mv, 1);
            isHasTracked = true;
            return;
        }

        if (!FTUtil.isTargetClassInSpecial(className)) {
            if ((className.startsWith("android/") || className.startsWith("androidx/")) && !(className.startsWith("android/support/v17/leanback") || className.startsWith("androidx/leanback"))) {
                return;
            }
        }

        if (methodDesc.equals("(Landroid/view/View;)V")) {
            Logger.info("方法扫描>>>类是"+className+">>>方法是"+nameDesc+" 点击");
            trackViewOnClick(mv, 1);
            isHasTracked = true;
            return;
        }

        if (nameDesc == "onItemSelected(Landroid/widget/AdapterView;Landroid/view/View;IJ)V" || nameDesc == "onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V") {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, "trackListView", "(Landroid/widget/AdapterView;Landroid/view/View;I)V", false);
            isHasTracked = true;
            return;
        }

        if (eventName != null && eventName.length() != 0) {
            mv.visitLdcInsn(eventName);
            mv.visitLdcInsn(eventProperties);
            mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, "track", "(Ljava/lang/String;Ljava/lang/String;)V", false);
            isHasTracked = true;
            return;
        }

        if (interfaces != null && interfaces.length > 0) {
            boolean hasInterface = false;
            for (String anInterface : interfaces) {
                if(anInterface.contains("android/widget/AdapterView$OnItemClickListener")){
                    hasInterface = true;
                    break;
                }
            }
            if (isOnItemClickMethod && hasInterface) {
                mv.visitVarInsn(ALOAD, variableID);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitVarInsn(ILOAD, 3);
                mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, "trackListView", "(Landroid/widget/AdapterView;Landroid/view/View;I)V", false);
                isHasTracked = true;
                return;
            } else {
                for (String interfaceName : interfaces) {
                    FTMethodCell ftMethodCell = FTHookConfig.INTERFACE_METHODS.get(interfaceName + nameDesc);
                    if (ftMethodCell != null) {
                        visitMethodWithLoadedParams(mv, INVOKESTATIC, FTHookConfig.FT_SDK_API, ftMethodCell.agentName, ftMethodCell.agentDesc, ftMethodCell.paramsStart, ftMethodCell.paramsCount, ftMethodCell.opcodes);
                        isHasTracked = true;
                        return;
                    }
                }
            }
        }

        if (isOnClickMethod) {
            trackViewOnClick(mv, variableID);
            isHasTracked = true;
        }
    }

    private static void visitMethodWithLoadedParams(MethodVisitor methodVisitor, int opcode, String owner, String methodName, String methodDesc, int start, int count, List<Integer> paramOpcodes) {
        for (int i = start; i < start + count; i++) {
            methodVisitor.visitVarInsn(paramOpcodes.get(i - start), i);
        }
        methodVisitor.visitMethodInsn(opcode, owner, methodName, methodDesc, false);
    }

    /**
     * 获取方法参数下标为 index 的对应 ASM index
     * @param types 方法参数类型数组
     * @param index 方法中参数下标，从 0 开始
     * @param isStaticMethod 该方法是否为静态方法
     * @return 访问该方法的 index 位参数的 ASM index
     */
    int getVisitPosition(Type[] types, int index, boolean isStaticMethod) {
        if (types == null || index < 0 || index >= types.length) {
            throw new Error("getVisitPosition error");
        }
        if (index == 0) {
            return isStaticMethod ? 0 : 1;
        } else {
            return getVisitPosition(types, index - 1, isStaticMethod) + types[index - 1].getSize();
        }
    }

    void trackViewOnClick(MethodVisitor mv, int index) {
        mv.visitVarInsn(ALOAD, index);
        mv.visitMethodInsn(INVOKESTATIC, FTHookConfig.FT_SDK_API, "trackViewOnClick", "(Landroid/view/View;)V", false);
    }

}

