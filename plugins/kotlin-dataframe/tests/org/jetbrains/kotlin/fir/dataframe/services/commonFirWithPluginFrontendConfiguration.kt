/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.frontend.fir.DisableLazyResolveChecksAfterAnalysisChecker
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolveContractViolationErrorHandler

fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
    useAfterAnalysisCheckers(
        ::DisableLazyResolveChecksAfterAnalysisChecker,
    )
    firHandlersStep {
        useHandlers(
            ::FirResolveContractViolationErrorHandler,
        )
    }
    defaultDirectives {
        +ENABLE_PLUGIN_PHASES
        +FIR_DUMP
        FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
    }
    useConfigurators(
        ::DataFramePluginAnnotationsProvider
    )
}
