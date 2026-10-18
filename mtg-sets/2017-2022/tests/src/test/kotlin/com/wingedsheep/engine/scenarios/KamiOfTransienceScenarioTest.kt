package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kami of Transience (NEO #197).
 *
 *   Trample
 *   Whenever you cast an enchantment spell, put a +1/+1 counter on this creature.
 *   At the beginning of each end step, if an enchantment was put into your graveyard from the
 *   battlefield this turn, you may return this card from your graveyard to your hand.
 */
class KamiOfTransienceScenarioTest : ScenarioTestBase() {

    init {
        fun setup(enchantmentOwner: Int) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInGraveyard(1, "Kami of Transience")
            .withCardOnBattlefield(enchantmentOwner, "Glorious Anthem")
            .withCardInHand(1, "Naturalize")
            .withLandsOnBattlefield(1, "Forest", 2)
            .apply { repeat(5) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Kami of Transience") {

            test("returns from the graveyard at end step after your enchantment went to your graveyard") {
                val game = setup(enchantmentOwner = 1)
                val anthem = game.findPermanent("Glorious Anthem")!!
                game.castSpell(1, "Naturalize", anthem).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Glorious Anthem") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("the end-step trigger offers the return") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()
                game.isInHand(1, "Kami of Transience") shouldBe true
            }

            test("an opponent's enchantment going to their graveyard doesn't count") {
                val game = setup(enchantmentOwner = 2)
                val anthem = game.findPermanent("Glorious Anthem")!!
                game.castSpell(1, "Naturalize", anthem).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Glorious Anthem") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(1, "Kami of Transience") shouldBe true
            }

            test("casting an enchantment spell puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kami of Transience")
                    .withCardInHand(1, "Glorious Anthem")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val kami = game.findPermanent("Kami of Transience")!!
                game.castSpell(1, "Glorious Anthem").error shouldBe null
                game.resolveStack()
                val counters = game.state.getEntity(kami)?.get<CountersComponent>()
                counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }
    }
}
