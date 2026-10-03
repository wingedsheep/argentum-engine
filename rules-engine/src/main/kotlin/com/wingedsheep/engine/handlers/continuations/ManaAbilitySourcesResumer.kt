package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ManaAbilitySourcesContinuation

/** The dispatcher has popped the scope, so subsequent instructions see ordinary mana sources. */
class ManaAbilitySourcesResumer : AutoResumerModule {
    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(ManaAbilitySourcesContinuation::class) { state, _, events, checkForMore ->
            checkForMore(state, events)
        }
    )
}
