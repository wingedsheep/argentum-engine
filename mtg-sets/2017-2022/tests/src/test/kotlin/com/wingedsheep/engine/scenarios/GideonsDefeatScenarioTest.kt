package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gideon's Defeat (HOU #13) — {W} Instant.
 *
 *   Exile target white creature that's attacking or blocking. If it was a Gideon planeswalker,
 *   you gain 5 life.
 *
 * The Gideon check is read before the exile, against projected state — Gideon Blackblade is a
 * creature on his controller's turn and can attack, so he's the bonus case.
 */
class GideonsDefeatScenarioTest : ScenarioTestBase() {

    init {
        fun opponentAttacksWith(attacker: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Gideon's Defeat")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(2, attacker)
            .withActivePlayer(2)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()
            .also { game ->
                game.declareAttackers(mapOf(attacker to 1)).error shouldBe null
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
            }

        test("exiles an attacking white creature; no life is gained") {
            val game = opponentAttacksWith("Savannah Lions")

            game.castSpell(1, "Gideon's Defeat", targetId = game.findPermanent("Savannah Lions")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Savannah Lions") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("exiling an attacking Gideon planeswalker gains the caster 5 life") {
            val game = opponentAttacksWith("Gideon Blackblade")

            game.castSpell(1, "Gideon's Defeat", targetId = game.findPermanent("Gideon Blackblade")!!)
                .error shouldBe null
            game.resolveStack()

            withClue("indestructible doesn't stop exile") { game.isInExile(2, "Gideon Blackblade") shouldBe true }
            withClue("the caster gains 5") { game.getLifeTotal(1) shouldBe 25 }
            game.getLifeTotal(2) shouldBe 20
        }

        test("exiles a blocking white creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gideon's Defeat")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldBe null
            if (game.state.priorityPlayerId == game.player2Id) game.passPriority()

            game.castSpell(1, "Gideon's Defeat", targetId = game.findPermanent("Savannah Lions")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Savannah Lions") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }

        test("a white creature that isn't in combat can't be targeted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Gideon's Defeat")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gideon's Defeat", targetId = game.findPermanent("Savannah Lions")!!).error shouldNotBe null
        }

        test("an attacking nonwhite creature can't be targeted") {
            val game = opponentAttacksWith("Grizzly Bears")

            game.castSpell(1, "Gideon's Defeat", targetId = game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
