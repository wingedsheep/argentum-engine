package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fogwalker (EMN #60, {1}{U}, 1/3 Spirit).
 *
 *   Skulk (This creature can't be blocked by creatures with greater power.)
 *   When this creature enters, target creature an opponent controls doesn't untap during its
 *   controller's next untap step.
 */
class FogwalkerScenarioTest : ScenarioTestBase() {

    init {
        test("the entering trigger keeps an opponent's tapped creature tapped through their next untap step only") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Fogwalker")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val seeker = game.findPermanent("Glory Seeker")!!

            game.castSpell(1, "Fogwalker").error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision != null) game.selectTargets(listOf(seeker)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("opponent's turn: Glory Seeker skipped its untap") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getEntity(seeker)!!.has<TappedComponent>() shouldBe true
            }

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the opponent's following untap step untaps it normally") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getEntity(seeker)!!.has<TappedComponent>() shouldBe false
            }
        }

        test("skulk: a creature with greater power can't block it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fogwalker")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Fogwalker" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("2-power Glory Seeker vs 1-power Fogwalker") {
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Fogwalker"))).error shouldNotBe null
            }
        }
    }
}
