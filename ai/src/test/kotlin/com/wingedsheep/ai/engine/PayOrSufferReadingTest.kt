package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * "At the beginning of your upkeep, draw a card unless target opponent sacrifices a creature of
 * their choice or pays 3 life" (Indulgent Tormentor): a
 * [com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect] whose suffer never touches the payer —
 * it is our draw. Every way out still costs the chosen opponent something (a creature, 3 life, or
 * our card), so the slot hurts them, and the trigger is for what each way out does for us.
 *
 * Unread, the slot read UNKNOWN (only a suffer that hurt the payer counted) and the trigger had no
 * intent at all.
 */
class PayOrSufferReadingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val upkeep by lazy {
        cardRegistry.requireCard("Indulgent Tormentor").script.triggeredAbilities.single()
    }

    init {
        test("the opponent made to pay or let us draw reads as harmed") {
            withClue("Indulgent Tormentor: sacrifice, 3 life or our card, the opponent's pick") {
                intents.effectTargetPolarities(upkeep.effect, listOf(upkeep.targetRequirement!!), "Indulgent Tormentor") shouldBe
                    listOf(TargetPolarity.HARMFUL)
            }
        }

        test("the punisher reads as the draw, edict and life loss it forces") {
            withClue("Indulgent Tormentor: whichever the opponent picks is ours") {
                intents.forEffect(upkeep.effect).tags shouldContainAll
                    setOf(IntentTag.DRAW, IntentTag.REMOVAL, IntentTag.DAMAGE_TO_PLAYERS)
            }
        }
    }
}
