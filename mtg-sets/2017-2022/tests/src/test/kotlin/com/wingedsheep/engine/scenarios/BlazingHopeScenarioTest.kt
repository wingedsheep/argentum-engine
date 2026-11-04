package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Blazing Hope (RIX #3) — {W} Instant.
 * "Exile target creature with power greater than or equal to your life total."
 *
 * The target filter compares the candidate's projected power against the *caster's* current life
 * total (`DynamicAmount.YourLifeTotal`), so "your" must bind to the spell's controller during
 * target validation — not to the candidate's controller.
 */
class BlazingHopeScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Blazing Hope")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLifeTotal(1, 3)
            .withLifeTotal(2, 20)
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("exiles a creature whose power equals your life total") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            withClue("a 3-power creature is a legal target at 3 life") {
                game.castSpell(1, "Blazing Hope", giant).error shouldBe null
            }
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInExile(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("cannot target a creature whose power is below your life total") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("a 2-power creature is not a legal target at 3 life") {
                game.castSpell(1, "Blazing Hope", bears).error shouldNotBe null
            }
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("the target is rechecked on resolution: gaining life above its power fizzles the spell") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Blazing Hope", giant).error shouldBe null

            // Before it resolves, the caster's life rises to 4 — above Hill Giant's 3 power, so the
            // only target is illegal on resolution (CR 608.2b) and the spell does nothing.
            game.state = game.state.updateEntity(game.player1Id) { it.with(LifeTotalComponent(4)) }
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isInGraveyard(1, "Blazing Hope") shouldBe true
        }
    }
}
