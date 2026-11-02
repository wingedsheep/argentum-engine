package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Severed Strands (GRN #85) — {1}{B} Sorcery.
 *   As an additional cost to cast this spell, sacrifice a creature.
 *   You gain life equal to the sacrificed creature's toughness. Destroy target creature an opponent controls.
 *
 * Pins the cost-linked amount: the life gained is the sacrificed creature's toughness (not its
 * power) as it last existed on the battlefield, including a continuous buff.
 */
class SeveredStrandsScenarioTest : ScenarioTestBase() {

    init {
        test("gains life equal to the sacrificed creature's last-known toughness and destroys the target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Severed Strands")
                .withCardOnBattlefield(1, "Horned Turtle") // 1/4
                .withCardOnBattlefield(1, "Glorious Anthem") // +1/+1 -> 2/5
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Severed Strands").single()
            val turtle = game.findPermanent("Horned Turtle")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(ChosenTarget.Permanent(bears)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(turtle)),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Horned Turtle") shouldBe true

            game.resolveStack()

            withClue("toughness 4 + Anthem's +1 = 5 life, not its power") {
                game.getLifeTotal(1) shouldBe 25
            }
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
