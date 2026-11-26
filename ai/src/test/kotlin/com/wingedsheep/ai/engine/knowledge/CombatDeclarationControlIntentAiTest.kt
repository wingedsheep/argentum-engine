package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain

/**
 * Taking over an opponent's block declarations ([com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsEffect])
 * and forcing their creatures to block ([com.wingedsheep.sdk.scripting.effects.MustBlockGroupEffect]) read as
 * combat enhancement: the attacker picks every fight.
 */
class CombatDeclarationControlIntentAiTest : ScenarioTestBase() {

    private fun tagsOf(name: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(name)).tags

    init {
        test("Odric's 'you choose which creatures block this combat' is a combat enhancement") {
            tagsOf("Odric, Master Tactician") shouldContain IntentTag.COMBAT_ENHANCEMENT
        }

        test("Brutal Hordechief's forced, chosen blocks are a combat enhancement") {
            tagsOf("Brutal Hordechief") shouldContain IntentTag.COMBAT_ENHANCEMENT
        }
    }
}
