/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup

class IrBodyFiller : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        DataFrameFileLowering(pluginContext).lower(moduleFragment)
    }
}

private class DataFrameFileLowering(val context: IrPluginContext) : FileLoweringPass, IrElementTransformerVoid() {
    companion object {
        val COLUMNS_CONTAINER_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("ColumnsContainer")), Name.identifier("get"))
        val DATA_ROW_ID =
            CallableId(ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier("DataRow")), Name.identifier("get"))
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val origin = declaration.origin
        return if (origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == DataFramePlugin) {
            declaration.transformChildren(this, null)
            declaration
        } else {
            super.visitClass(declaration)
        }
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        val origin = declaration.origin
        if (!(origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == DataFramePlugin)) return declaration
        declaration.body = generateBodyForDefaultConstructor(declaration)
        return declaration
    }

    private fun generateBodyForDefaultConstructor(declaration: IrConstructor): IrBody? {
        val type = declaration.returnType as? IrSimpleType ?: return null
        val irBuiltIns = context.irBuiltIns
        val delegatingAnyCall = IrDelegatingConstructorCallImpl(
            -1,
            -1,
            irBuiltIns.anyType,
            irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return null,
            typeArgumentsCount = 0,
            valueArgumentsCount = 0
        ).copyAttributes(declaration.parentAsClass)

        val initializerCall = IrInstanceInitializerCallImpl(
            -1,
            -1,
            (declaration.parent as? IrClass)?.symbol ?: return null,
            type
        )

        return context.irFactory.createBlockBody(-1, -1, listOf(delegatingAnyCall, initializerCall))
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val origin = declaration.origin
        if (!(origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == DataFramePlugin)) return declaration
        val getter = declaration.getter ?: return declaration

        val constructors = context.referenceConstructors(ClassId(FqName("kotlin.jvm"), Name.identifier("JvmName")))
        val jvmName = constructors.single { it.owner.valueParameters.size == 1 }
        val markerName = ((getter.extensionReceiverParameter!!.type as IrSimpleType).arguments.single() as IrSimpleType).classFqName?.shortName()!!
        val jvmNameArg = "${markerName.identifier}_${declaration.name.identifier}"
        getter.annotations = listOf(
            IrConstructorCallImpl(-1, -1, jvmName.owner.returnType, jvmName, 0, 0, 1)
                .also {
                    it.putValueArgument(0, IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, jvmNameArg))
                }
        )

        val returnType = getter.returnType
        val isDataColumn = returnType.classFqName!!.asString().let {
            it == DataColumn::class.qualifiedName!! || it == ColumnGroup::class.qualifiedName!!
        }

        val get = if (isDataColumn) {
            context
                .referenceFunctions(COLUMNS_CONTAINER_ID)
                .single {
                    it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.stringType
                }
        } else {
            context
                .referenceFunctions(DATA_ROW_ID)
                .single {
                    it.owner.valueParameters.size == 1 && it.owner.valueParameters[0].type == context.irBuiltIns.stringType
                }
        }

        val call = IrCallImpl(-1, -1, context.irBuiltIns.anyNType, get, 0, 1).also {
            val thisSymbol: IrValueSymbol = getter.extensionReceiverParameter?.symbol!!
            it.dispatchReceiver = IrGetValueImpl(-1, -1, thisSymbol)
            it.putValueArgument(0, IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, declaration.name.identifier))
        }

        val typeOp = IrTypeOperatorCallImpl(-1, -1, returnType, IrTypeOperator.CAST, returnType, call)
        val returnExpression = IrReturnImpl(-1, -1, returnType, getter.symbol, typeOp)
        getter.apply {
            body = IrBlockBodyImpl(-1, -1, listOf(returnExpression))
        }

        return declaration
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrExpression {
        val origin = (expression.type.classifierOrNull?.owner as? IrClass)?.origin ?: return expression
        val fromPlugin = origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == DataFramePlugin
        val scopeReference = expression.type.classFqName?.shortName()?.asString()?.startsWith("Scope") ?: false
        if (!(fromPlugin || scopeReference)) {
            return expression
        }
        val constructor = expression.type.getClass()!!.constructors.toList().single()
        val type = expression.type
        return IrConstructorCallImpl(-1, -1, type, constructor.symbol, 0, 0, 0)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        /*
* IrBlockBodyImpl
* - IrReturnImpl
*  - value: IrTypeOperatorCallImpl
*      - type: ...
*      - typeOperand: IrTypeOperator CAST
*      - argument: IrCallImpl
*/
        if (declaration.isPropertyAccessor) return declaration
        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != DataFramePlugin) {
            declaration.transformChildrenVoid(this)
            return declaration
        }
        val annotation = declaration.annotations.single { it.type.classFqName?.shortName() == Name.identifier("Refine") }
        val prototypeName = (annotation.getValueArgument(0)!! as IrConst<*>).value as String
        val functions = context
            .referenceFunctions(CallableId(FqName("org.jetbrains.kotlinx.dataframe.api"), Name.identifier(prototypeName.substringBefore("_"))))

        val function = functions.single {
            it.owner.annotations.any { (
                it.getValueArgument(Name.identifier("id")) as? IrConst<*>)?.let { (it.value as? String) == prototypeName } ?: false
            }
        }
        declaration.body = context.irFactory.createBlockBody(-1, -1) {
            val call = IrCallImpl(
                startOffset = -1,
                endOffset = -1,
                type = function.owner.returnType,
                symbol = function,
                typeArgumentsCount = function.owner.typeParameters.size,
                valueArgumentsCount = function.owner.valueParameters.size
            ).apply {
                extensionReceiver = IrGetValueImpl(-1, -1, declaration.extensionReceiverParameter!!.symbol)
            }
            declaration.valueParameters.forEachIndexed { index, irValueParameter ->
                call.putValueArgument(index, IrGetValueImpl(-1, -1, irValueParameter.symbol))
            }
            declaration.typeParameters.forEachIndexed { index, irTypeParameter ->
                call.putTypeArgument(index, IrSimpleTypeImpl(irTypeParameter.symbol, SimpleTypeNullability.NOT_SPECIFIED, emptyList(), emptyList()))
            }
            val typeOp = IrTypeOperatorCallImpl(
                startOffset = -1,
                endOffset = -1,
                type = declaration.returnType,
                operator = IrTypeOperator.CAST,
                typeOperand = declaration.returnType,
                argument = call
            )
            statements += IrReturnImpl(-1, -1, declaration.returnType, declaration.symbol, typeOp)
        }
        return declaration
    }
}
