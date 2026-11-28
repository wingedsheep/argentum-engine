package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nightbird's Clutches (ISD #154) — {1}{R} Sorcery.
 *
 *   Up to two target creatures can't block this turn.
 *   Flashback {3}{R}
 */
class NightbirdsClutchesScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Centaur Courser")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.attackWithGiant() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
    }

    init {
        test("both targeted creatures can't block this turn") {
            val game = base()
                .withCardInHand(1, "Nightbird's Clutches")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            val card = game.findCardsInHand(1, "Nightbird's Clutches").single()
            val result = game.execute(
                CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser)))
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.attackWithGiant()
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Hill Giant"))).error shouldNotBe null
        }

        test("flashback {3}{R} with a single target leaves the other creature free to block") {
            val game = base()
                .withCardInGraveyard(1, "Nightbird's Clutches")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val card = game.findCardsInGraveyard(1, "Nightbird's Clutches").single()
            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    card,
                    listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                )
            )
            withClue("flashback cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.isInExile(1, "Nightbird's Clutches") shouldBe true

            game.attackWithGiant()
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Hill Giant"))).error shouldBe null
        }
    }
}
