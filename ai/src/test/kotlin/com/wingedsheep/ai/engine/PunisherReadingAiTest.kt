package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Target opponent loses 3 life unless they sacrifice a nonland permanent of their choice or
 * discard a card" — the punisher on a chosen player, written either as an unless-clause they pay
 * ([com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect], Starseer Mentor) or as a choice of
 * branches they make ([com.wingedsheep.sdk.scripting.effects.ChooseActionEffect], Thornplate
 * Intimidator). Every way out costs that player something, so the slot hurts them; and with a
 * card from hand among the ways out, it reads as the discard punisher Torment of Hailfire is.
 *
 * Unread, both slots read UNKNOWN and Thornplate Intimidator's trigger had no intent at all, though
 * Starseer Mentor's identical text already read as discard.
 */
class PunisherReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun trigger(card: String) =
        cardRegistry.requireCard(card).script.triggeredAbilities.first { it.targetRequirement != null }

    init {
        test("the punished player's slot reads as harmful") {
            for (card in listOf("Starseer Mentor", "Thornplate Intimidator")) {
                val ability = trigger(card)
                withClue("$card: every way out of the punisher is a loss for the opponent chosen") {
                    intents.effectTargetPolarities(ability.effect, listOf(ability.targetRequirement!!), card) shouldBe
                        listOf(TargetPolarity.HARMFUL)
                }
            }
        }

        test("an opponent's choice of losses that includes a discard reads as discard") {
            withClue("Thornplate Intimidator: sacrifice, discard or lose 3 life, the opponent's pick") {
                intents.forEffect(trigger("Thornplate Intimidator").effect).tags shouldContain IntentTag.DISCARD
            }
        }
    }
}
