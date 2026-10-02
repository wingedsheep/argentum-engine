package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thief of Existence — the cast trigger grants the creature *spell* a leaves-the-battlefield
 * ability that the permanent keeps once the spell resolves (CR 400.7a).
 */
class ThiefOfExistenceScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Thief of Existence")
        .withCardInHand(1, "Unsummon")
        .withCardInHand(1, "Counterspell")
        .withLandsOnBattlefield(1, "Wastes", 1)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Sol Ring")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castThief() {
        execute(CastSpell(player1Id, findCardsInHand(1, "Thief of Existence").single())).error shouldBe null
    }

    /** Bounce Thief and answer the granted trigger's "target opponent" prompt if the engine asks. */
    private fun TestGame.bounceThief() {
        castSpell(1, "Unsummon", findPermanent("Thief of Existence")!!).error shouldBe null
        resolveStack()
        if (getPendingDecision() is ChooseTargetsDecision) {
            selectTargets(listOf(player2Id)).error shouldBe null
        }
        resolveStack()
        isInHand(1, "Thief of Existence") shouldBe true
    }

    private fun TestGame.thiefGrants() =
        state.grantedTriggeredAbilities.count { it.entityId == findPermanent("Thief of Existence") }

    init {
        test("exiling a permanent gives the resolved creature the leaves-the-battlefield draw") {
            val game = board()
            game.castThief()
            game.selectTargets(listOf(game.findPermanent("Sol Ring")!!)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Sol Ring") shouldBe true
            game.isOnBattlefield("Thief of Existence") shouldBe true
            game.thiefGrants() shouldBe 1

            val opponentHand = game.handSize(2)
            game.bounceThief()
            game.handSize(2) shouldBe opponentHand + 1
        }

        test("choosing no target exiles nothing and grants nothing") {
            val game = board()
            game.castThief()
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Sol Ring") shouldBe true
            game.thiefGrants() shouldBe 0

            val opponentHand = game.handSize(2)
            game.bounceThief()
            game.handSize(2) shouldBe opponentHand
        }

        test("a creature is not a legal target") {
            val game = board()
            game.castThief()
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldNotBe null
        }

        test("countering Thief in response still exiles, and the grant dies with the spell") {
            val game = board()
            game.castThief()
            game.selectTargets(listOf(game.findPermanent("Sol Ring")!!)).error shouldBe null
            game.castSpellTargetingStackSpell(1, "Counterspell", "Thief of Existence").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Thief of Existence") shouldBe true
            game.isInExile(2, "Sol Ring") shouldBe true
            game.state.grantedTriggeredAbilities.size shouldBe 0
        }
    }
}
