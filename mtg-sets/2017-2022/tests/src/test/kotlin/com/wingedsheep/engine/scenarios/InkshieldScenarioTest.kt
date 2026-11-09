package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Inkshield (C21 #71) — {3}{W}{B} Instant.
 *
 * "Prevent all combat damage that would be dealt to you this turn. For each 1 damage prevented this
 *  way, create a 2/1 white and black Inkling creature token with flying."
 *
 * A recipient-side ("to you") combat-only shield that carries its own reaction. Each application of
 * the shield — one per combat damage instance — makes that many Inklings, so several attackers add
 * up; damage that can't be prevented is dealt and makes none; noncombat damage isn't covered.
 */
class InkshieldScenarioTest : ScenarioTestBase() {

    private fun TestGame.inklings() = state.getBattlefield().filter {
        state.getEntity(it)?.get<CardComponent>()?.typeLine?.subtypes?.any { s -> s.value == "Inkling" } == true
    }

    /** Player 2 attacks; player 1 casts Inkshield in the declare-attackers step. */
    private fun TestGame.attackAndShield(attackers: Map<String, Int>) {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(attackers).error shouldBe null
        passPriority() // the attacker's controller passes; the defender now has priority
        castSpell(1, "Inkshield").error shouldBe null
        resolveStack()
    }

    private fun TestGame.finishCombat() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers().error shouldBe null
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        resolveStack()
    }

    private fun defenderGame(vararg opposing: String, extra: (ScenarioBuilder.() -> Unit)? = null): TestGame {
        val builder = scenario()
            .withPlayers("Defender", "Attacker")
            .withCardInHand(1, "Inkshield")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLifeTotal(1, 20)
        opposing.forEach { builder.withCardOnBattlefield(2, it, summoningSickness = false) }
        extra?.invoke(builder)
        return builder.withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
    }

    init {
        test("one Inkling per point of prevented combat damage, and no life is lost") {
            val game = defenderGame("Hill Giant")
            game.attackAndShield(mapOf("Hill Giant" to 1))
            game.finishCombat()

            game.getLifeTotal(1) shouldBe 20
            val inklings = game.inklings()
            withClue("Hill Giant dealt 3 combat damage, all prevented") { inklings.size shouldBe 3 }
            inklings.forEach { token ->
                val projected = game.state.projectedState
                withClue("2/1 white-and-black Inkling with flying, controlled by Inkshield's controller") {
                    projected.getPower(token) shouldBe 2
                    projected.getToughness(token) shouldBe 1
                    projected.hasKeyword(token, Keyword.FLYING) shouldBe true
                    projected.getColors(token) shouldBe setOf(Color.WHITE.name, Color.BLACK.name)
                    projected.getController(token) shouldBe game.player1Id
                }
            }
        }

        test("several attackers are several damage events — the tokens add up") {
            val game = defenderGame("Hill Giant", "Grizzly Bears", "Giant Spider")
            game.attackAndShield(mapOf("Hill Giant" to 1, "Grizzly Bears" to 1, "Giant Spider" to 1))
            game.finishCombat()

            game.getLifeTotal(1) shouldBe 20
            withClue("3 + 2 + 2 damage prevented") { game.inklings().size shouldBe 7 }
        }

        test("only damage dealt to the player is prevented — a blocked attacker's damage hits its blocker") {
            val game = defenderGame("Hill Giant", "Grizzly Bears") {
                withCardOnBattlefield(1, "Devout Chaplain", summoningSickness = false)
            }
            game.attackAndShield(mapOf("Hill Giant" to 1, "Grizzly Bears" to 1))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Devout Chaplain" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            withClue("Only the unblocked Hill Giant's 3 damage was prevented") { game.inklings().size shouldBe 3 }
        }

        test("damage that can't be prevented is dealt in full and makes no Inklings") {
            val game = defenderGame("Questing Beast")
            game.attackAndShield(mapOf("Questing Beast" to 1))
            game.finishCombat()

            withClue("Questing Beast's combat damage can't be prevented") {
                game.getLifeTotal(1) shouldBe 16
            }
            game.inklings().size shouldBe 0
        }

        test("noncombat damage is not prevented and makes no Inklings") {
            val game = defenderGame("Hill Giant") {
                withCardInHand(2, "Lightning Bolt")
                withLandsOnBattlefield(2, "Mountain", 1)
            }
            game.attackAndShield(mapOf("Hill Giant" to 1))

            val bolt = game.state.getHand(game.player2Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
            }
            game.execute(CastSpell(game.player2Id, bolt, listOf(ChosenTarget.Player(game.player1Id)))).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
            game.inklings().size shouldBe 0

            game.finishCombat()
            withClue("the combat damage is still prevented afterwards") {
                game.getLifeTotal(1) shouldBe 17
                game.inklings().size shouldBe 3
            }
        }

        // CR 616.1: when Fog and Inkshield both apply to the same combat damage, the player being
        // dealt it picks which applies first. The one that applies prevents it all, so only picking
        // Inkshield makes Inklings.
        for ((pick, inklings) in listOf("Inkshield" to 3, "Fog" to 0)) {
            test("with Fog also out, the defender chooses $pick to apply and gets $inklings Inklings") {
                val game = defenderGame("Hill Giant") {
                    withCardInHand(1, "Fog")
                    withLandsOnBattlefield(1, "Forest", 1)
                }
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.passPriority()
                game.castSpell(1, "Fog").error shouldBe null
                game.castSpell(1, "Inkshield").error shouldBe null // holding priority over Fog
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null

                var guard = 0
                while (game.getPendingDecision() == null && game.state.step != Step.POSTCOMBAT_MAIN && guard++ < 20) {
                    game.passPriority()
                }
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                decision.playerId shouldBe game.player1Id
                val index = decision.options.indexOfFirst { it.startsWith(pick) }
                withClue("options: ${decision.options}") { (index >= 0) shouldBe true }
                game.submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.inklings().size shouldBe inklings
            }
        }

        test("an attack the opponent makes next turn is not covered") {
            val game = defenderGame("Hill Giant")
            game.attackAndShield(mapOf("Hill Giant" to 1))
            game.finishCombat()
            game.inklings().size shouldBe 3

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS) // player 1's turn: shield is long gone
            game.state.floatingEffects.none {
                it.effect.modification is com.wingedsheep.engine.mechanics.layers.SerializableModification.PreventAllDamageToGroup
            } shouldBe true
        }
    }
}
