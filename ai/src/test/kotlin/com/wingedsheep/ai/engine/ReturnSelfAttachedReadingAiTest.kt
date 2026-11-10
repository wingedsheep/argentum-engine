package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Return this card to the battlefield (transformed) attached to target …"
 * ([com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect]): our own card back
 * out of the graveyard as an Aura — [IntentTag.RECURSION] — and its target gets whatever that Aura
 * does to what it enchants. Accursed Witch comes back as Infectious Curse, whose upkeep drain makes
 * "target opponent" a curse's victim; Gryff's Boon comes back as itself, a +1/+0-and-flying gift for
 * the creature it lands on. An Aura whose host reading is vetoed (Radiant Restraints' enter-tapped
 * replacement) is aimed by the side its slot is confined to.
 */
class ReturnSelfAttachedReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("the returned Aura's target reads as what the Aura does to its host") {
            val witch = card("Accursed Witch")
            val dies = witch.script.triggeredAbilities.single { it.targetRequirement != null }
            withClue("Accursed Witch returns as Infectious Curse, which drains the player it enchants") {
                intents.effectTargetPolarities(dies.effect, listOf(dies.targetRequirement!!), witch.name) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
            val boon = card("Gryff's Boon")
            val fromGraveyard = boon.script.activatedAbilities.single()
            withClue("Gryff's Boon returns as itself: enchanted creature gets +1/+0 and has flying") {
                intents.abilityTargetPolarities(boon.name, fromGraveyard.id) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            val grace = card("Radiant Grace")
            val graceDies = grace.script.triggeredAbilities.single { it.targetRequirement != null }
            withClue("Radiant Restraints' replacement vetoes its host reading; 'target opponent' still aims it") {
                intents.effectTargetPolarities(graceDies.effect, listOf(graceDies.targetRequirement!!), grace.name) shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }

        test("returning our own card from the graveyard attached to something reads as recursion") {
            val dies = card("Accursed Witch").script.triggeredAbilities.single { it.targetRequirement != null }
            CardIntentAnalyzer.analyzeEffect(dies.effect, listOf(dies.targetRequirement!!)).tags shouldContain
                IntentTag.RECURSION
        }
    }
}
