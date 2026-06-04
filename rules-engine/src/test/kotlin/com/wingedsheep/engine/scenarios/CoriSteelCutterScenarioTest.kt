package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cori-Steel Cutter (TDM #103, {1}{R} Artifact — Equipment).
 *
 *   Equipped creature gets +1/+1 and has trample and haste.
 *   Flurry — Whenever you cast your second spell each turn, create a 1/1 white Monk creature
 *   token with prowess. You may attach this Equipment to it.
 *   Equip {1}{R}
 *
 * Flurry is wired via the `flurry { }` builder ([com.wingedsheep.sdk.dsl.Triggers.NthSpellCast]
 * with `n = 2`), and the optional attach is a [com.wingedsheep.sdk.scripting.effects.MayEffect]
 * targeting the freshly-created token via the `CREATED_TOKENS` pipeline collection.
 */
class CoriSteelCutterScenarioTest : ScenarioTestBase() {

    init {
        context("Cori-Steel Cutter") {

            test("equipped creature gets +1/+1 and gains trample and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 vanilla
                    .withCardOnBattlefield(1, "Cori-Steel Cutter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cutter = game.findPermanent("Cori-Steel Cutter")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val equip = cardRegistry.getCard("Cori-Steel Cutter")!!.script.activatedAbilities[0]
                game.execute(
                    com.wingedsheep.engine.core.ActivateAbility(
                        game.player1Id, cutter, equip.id,
                        targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Grizzly Bears becomes a 3/3 (2/2 +1/+1)") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
                withClue("...and gains trample and haste") {
                    projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }

            test("Flurry: casting your second spell each turn creates a 1/1 white Monk token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cori-Steel Cutter")
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First spell of the turn (targets the opponent).
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                // Second spell triggers Flurry. Resolving the stack runs the trigger.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                // The optional "you may attach" prompt (no creatures to lose context) — decline if present.
                if (game.hasPendingDecision()) {
                    game.answerYesNo(false)
                    game.resolveStack()
                }

                val monks = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Monk Token"
                }
                withClue("Flurry created exactly one Monk token") {
                    monks.size shouldBe 1
                }
            }

            test("Flurry: accepting the prompt attaches the Equipment to the new Monk token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cori-Steel Cutter")
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                // Accept "you may attach this Equipment to it".
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.resolveStack()

                val cutter = game.findPermanent("Cori-Steel Cutter")!!
                val monk = game.state.getBattlefield().first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Monk Token"
                }
                withClue("The Equipment is attached to the freshly-created Monk token") {
                    game.state.getEntity(cutter)?.get<AttachedToComponent>()?.targetId shouldBe monk
                }
                val projected = game.state.projectedState
                withClue("The equipped Monk is a 2/2 with trample and haste") {
                    projected.getPower(monk) shouldBe 2
                    projected.getToughness(monk) shouldBe 2
                    projected.hasKeyword(monk, Keyword.TRAMPLE) shouldBe true
                    projected.hasKeyword(monk, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
