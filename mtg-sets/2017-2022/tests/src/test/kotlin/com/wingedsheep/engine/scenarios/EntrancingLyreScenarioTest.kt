package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.thb.cards.EntrancingLyre
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Entrancing Lyre (THB) — "{X}, {T}: Tap target creature with power X or less. It doesn't untap
 * during its controller's untap step for as long as this artifact remains tapped."
 *
 * Covers the X-bounded target, the lock holding through the opponent's untap step, and the ruling
 * that a Lyre untapped before its ability resolves — even if tapped again by then — still taps the
 * creature but doesn't hold it down.
 */
class EntrancingLyreScenarioTest : ScenarioTestBase() {

    private val lyreAbility = EntrancingLyre.activatedAbilities.first().id

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Entrancing Lyre")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.isTapped(name: String): Boolean =
        state.getEntity(findPermanent(name)!!)?.has<TappedComponent>() == true

    private fun TestGame.locked(name: String): Boolean =
        state.projectedState.hasKeyword(findPermanent(name)!!, AbilityFlag.DOESNT_UNTAP)

    init {
        test("taps a creature with power X or less and keeps it tapped through its controller's untap step") {
            val game = game()
            val lyre = game.findPermanent("Entrancing Lyre")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, lyre, lyreAbility, targets = listOf(ChosenTarget.Permanent(bears)), xValue = 2)
            ).error shouldBe null
            game.resolveStack()

            withClue("the Bears are tapped and locked") {
                game.isTapped("Grizzly Bears") shouldBe true
                game.locked("Grizzly Bears") shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("the Bears stayed tapped through Player2's untap step while the Lyre is tapped") {
                game.isTapped("Entrancing Lyre") shouldBe true
                game.isTapped("Grizzly Bears") shouldBe true
            }
        }

        test("a creature with power greater than X can't be targeted") {
            val game = game()
            val lyre = game.findPermanent("Entrancing Lyre")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(game.player1Id, lyre, lyreAbility, targets = listOf(ChosenTarget.Permanent(giant)), xValue = 2)
            ).error shouldNotBe null
            game.isTapped("Hill Giant") shouldBe false
        }

        test("untapped and re-tapped before resolution: the creature is tapped but can untap as usual") {
            val game = game()
            val lyre = game.findPermanent("Entrancing Lyre")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, lyre, lyreAbility, targets = listOf(ChosenTarget.Permanent(bears)), xValue = 2)
            ).error shouldBe null
            // In response, the Lyre untaps and is tapped again.
            game.state = untapOrConsumeStun(game.state, lyre).first
            game.state = tap(game.state, lyre).state
            game.resolveStack()

            withClue("the Bears are tapped, but the duration had already ended") {
                game.isTapped("Entrancing Lyre") shouldBe true
                game.isTapped("Grizzly Bears") shouldBe true
                game.locked("Grizzly Bears") shouldBe false
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("the Bears untapped during Player2's untap step") {
                game.isTapped("Grizzly Bears") shouldBe false
            }
        }
    }
}
