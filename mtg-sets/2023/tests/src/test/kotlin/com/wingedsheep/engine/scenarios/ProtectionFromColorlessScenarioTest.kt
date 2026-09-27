package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Protection from colorless (CR 702.16, 105.2c) — a colorless object has no color, so protection
 * from colorless matches every source whose colour set is empty. Exercises the blocking, combat
 * damage and attachment legs; the targeting leg lives in [AngelicInterventionScenarioTest].
 * Angelic Intervention is the grant.
 */
class ProtectionFromColorlessScenarioTest : ScenarioTestBase() {

    private fun protectFromColorless(game: TestGame, targetName: String) {
        val cast = game.castSpell(1, "Angelic Intervention", game.findPermanent(targetName)!!)
        withClue("Casting Angelic Intervention should succeed: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()
        val decision = game.getPendingDecision() as ChooseOptionDecision
        game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Colorless")))
    }

    init {
        context("protection from colorless") {

            test("combat damage from a colorless creature is prevented") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Intervention")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Juggernaut") // colorless 5/3
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                protectFromColorless(game, "Grizzly Bears")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Juggernaut" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Juggernaut"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Juggernaut's 5 damage is prevented") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
                withClue("the 3/3 Bears still deal their damage and kill the 5/3") {
                    game.isOnBattlefield("Juggernaut") shouldBe false
                }
            }

            test("a colorless creature can't block it, a colored one can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Intervention")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                protectFromColorless(game, "Grizzly Bears")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("Ornithopter is colorless") {
                    game.declareBlockers(mapOf("Ornithopter" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
                withClue("Hill Giant is red") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                }
            }

            test("colorless Equipment becomes unattached") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Intervention")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                withClue("setup: Bonesplitter starts attached") {
                    game.state.getEntity(bonesplitter)?.get<AttachedToComponent>() shouldNotBe null
                }

                protectFromColorless(game, "Grizzly Bears")

                withClue("CR 702.16d: the Equipment unattaches and stays on the battlefield") {
                    game.state.getEntity(bonesplitter)?.get<AttachedToComponent>() shouldBe null
                    game.isOnBattlefield("Bonesplitter") shouldBe true
                }
            }
        }
    }
}
