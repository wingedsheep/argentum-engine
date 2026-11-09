package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "That source's controller loses that much life unless they sacrifice that many permanents"
 * (Archfiend of Spite) is a [com.wingedsheep.sdk.scripting.effects.ChooseActionEffect] the damage
 * source's controller answers. No branch is the controller's to pick, so the card does whichever
 * the decider takes: the sacrifice is an edict, the life is a clock. Unread, the trigger had no
 * intent at all.
 */
class PunisherChoiceReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("an opponent's choice between losing life and sacrificing reads as removal and a clock") {
            val ability = cardRegistry.requireCard("Archfiend of Spite").script.triggeredAbilities.single()
            val tags = intents.forEffect(ability.effect).tags
            withClue("sacrifice branch → removal, life branch → damage to players") {
                tags shouldContain IntentTag.REMOVAL
                tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
            }
        }

        test("opening a punisher choice does not invent intents for the cards that already had one") {
            // Thornplate Intimidator's branches are read too (discard), but nothing in them draws.
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Thornplate Intimidator")).tags shouldNotContain IntentTag.DRAW
        }
    }
}
