package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HeadstrongBruteScenarioTest : ScenarioTestBase() {
    init {
        test("itself and an opponent's Pirate do not grant menace") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Headstrong Brute")
                .withCardOnBattlefield(2, "Dire Fleet Interloper")
                .build()
            game.state.projectedState.hasKeyword(game.findPermanent("Headstrong Brute")!!, Keyword.MENACE) shouldBe false
        }

        for (partner in listOf("Dire Fleet Interloper", "Universal Automaton")) {
            test("another $partner grants menace only to the Brute until it leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Headstrong Brute")
                    .withCardOnBattlefield(1, partner)
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val brute = game.findPermanent("Headstrong Brute")!!
                game.state.projectedState.hasKeyword(brute, Keyword.MENACE) shouldBe true
                if (partner == "Universal Automaton") {
                    game.state.projectedState.hasKeyword(game.findPermanent(partner)!!, Keyword.MENACE) shouldBe false
                }
                game.castSpell(1, "Unsummon", targetId = game.findPermanent(partner)!!).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield(partner) shouldBe false
                game.state.projectedState.hasKeyword(brute, Keyword.MENACE) shouldBe false
            }
        }

        test("menace prevents a single blocker while another Pirate is controlled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Headstrong Brute", summoningSickness = false)
                .withCardOnBattlefield(1, "Universal Automaton")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Headstrong Brute" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Headstrong Brute"))).error shouldNotBe null
            game.declareBlockers(mapOf(
                "Grizzly Bears" to listOf("Headstrong Brute"),
                "Centaur Courser" to listOf("Headstrong Brute")
            )).error shouldBe null
        }

        test("cannot block even while another Pirate is controlled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Headstrong Brute")
                .withCardOnBattlefield(2, "Universal Automaton")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Headstrong Brute" to listOf("Grizzly Bears"))).error shouldNotBe null
        }
    }
}
