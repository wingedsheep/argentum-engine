package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Akki Lavarunner // Tok-Tok, Volcano Born (CHK #153) — a flip card.
 *
 * "Haste. Whenever this creature deals damage to an opponent, flip it."
 * Tok-Tok: "Protection from red. If a red source would deal damage to a player, it deals that much
 * damage plus 1 to that player instead."
 */
class AkkiLavarunnerScenarioTest : ScenarioTestBase() {

    init {
        context("Akki Lavarunner") {

            test("flips after dealing combat damage to an opponent; Tok-Tok adds 1 to red damage to players") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Akki Lavarunner", summoningSickness = true)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val runner = game.findPermanent("Akki Lavarunner")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("haste lets it attack the turn it arrives") {
                    game.declareAttackers(mapOf("Akki Lavarunner" to 2)).error shouldBe null
                }
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.getPendingDecision() is CombatResolutionDecision) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                game.getLifeTotal(2) shouldBe 19
                val card = game.state.getEntity(runner)!!.get<CardComponent>()!!
                card.name shouldBe "Tok-Tok, Volcano Born"
                game.state.projectedState.getPower(runner) shouldBe 2
                game.state.projectedState.getToughness(runner) shouldBe 2

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                withClue("a red source deals 3 + 1 to a player under Tok-Tok") {
                    game.getLifeTotal(2) shouldBe 15
                }
            }

            test("Tok-Tok has protection from red") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Akki Lavarunner")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val runner = game.findPermanent("Akki Lavarunner")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Akki Lavarunner" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.getPendingDecision() is CombatResolutionDecision) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }
                game.state.getEntity(runner)!!.get<CardComponent>()!!.name shouldBe "Tok-Tok, Volcano Born"

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("a red spell can't target a creature with protection from red") {
                    (game.castSpell(1, "Lightning Bolt", runner).error != null) shouldBe true
                }
            }
        }
    }
}
