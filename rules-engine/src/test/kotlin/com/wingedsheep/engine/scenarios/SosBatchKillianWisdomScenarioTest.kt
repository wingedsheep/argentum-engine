package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for two Secrets of Strixhaven cards:
 *  - Killian's Confidence  ({W}{B} sorcery: +1/+1 + draw; graveyard recursion when your creatures
 *                           deal combat damage, "you may pay {W/B}")
 *  - Wisdom of Ages        ({4}{U}{U}{U} sorcery: return all instants/sorceries from graveyard,
 *                           no maximum hand size for the rest of the game, then exile itself)
 */
class SosBatchKillianWisdomScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Killian's Confidence") {
            test("the spell gives +1/+1 to the target and draws a card") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Killian's Confidence")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 target
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(2) { builder = builder.withCardInLibrary(1, "Forest") }
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.state.getHand(game.player1Id).size

                game.castSpell(1, "Killian's Confidence", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears becomes 3/3 until end of turn") {
                    projector.getProjectedPower(game.state, bears) shouldBe 3
                    projector.getProjectedToughness(game.state, bears) shouldBe 3
                }
                withClue("controller draws a card (hand size unchanged: -1 spell cast, +1 draw)") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore
                }
            }

            test("returns itself from the graveyard when your creatures deal combat damage and you pay {W/B}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Killian's Confidence")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // attacker
                    .withLandsOnBattlefield(1, "Swamp", 1) // pays {W/B} via {B}
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                advanceToCombatDamage(game)

                withClue("the graveyard trigger asks whether to pay {W/B}") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                withClue("then asks which mana sources to use") {
                    (game.getPendingDecision() is SelectManaSourcesDecision) shouldBe true
                }
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("Killian's Confidence returns from graveyard to hand") {
                    game.findCardsInHand(1, "Killian's Confidence").size shouldBe 1
                    game.findCardsInGraveyard(1, "Killian's Confidence").size shouldBe 0
                }
            }

            test("stays in the graveyard when you decline the optional payment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Killian's Confidence")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                advanceToCombatDamage(game)

                withClue("the graveyard trigger asks whether to pay {W/B}") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declining leaves Killian's Confidence in the graveyard") {
                    game.findCardsInGraveyard(1, "Killian's Confidence").size shouldBe 1
                    game.findCardsInHand(1, "Killian's Confidence").size shouldBe 0
                }
            }
        }

        context("Wisdom of Ages") {
            test("returns all instants/sorceries from graveyard, removes max hand size, and exiles itself") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wisdom of Ages")
                    .withCardInGraveyard(1, "Lightning Bolt")   // instant
                    .withCardInGraveyard(1, "Divination")       // sorcery
                    .withCardInGraveyard(1, "Grizzly Bears")    // creature — must stay
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val game = builder.build()

                game.castSpell(1, "Wisdom of Ages").error shouldBe null
                game.resolveStack()

                withClue("both the instant and the sorcery return to hand") {
                    game.findCardsInHand(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInHand(1, "Divination").size shouldBe 1
                }
                withClue("the creature stays in the graveyard") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("Wisdom of Ages exiles itself (not in hand or graveyard)") {
                    game.findCardsInHand(1, "Wisdom of Ages").size shouldBe 0
                    game.findCardsInGraveyard(1, "Wisdom of Ages").size shouldBe 0
                    game.state.getExile(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Wisdom of Ages"
                    } shouldBe true
                }
                withClue("the caster has no maximum hand size for the rest of the game") {
                    game.state.getEntity(game.player1Id)?.has<PlayerNoMaximumHandSizeComponent>() shouldBe true
                }
            }
        }
    }

    /**
     * Declare-attackers is already submitted; advance to the combat damage step (auto-submitting
     * the defender's empty blockers along the way), where combat damage triggers go on the stack
     * and resolution pauses for the first decision.
     */
    private fun advanceToCombatDamage(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var iterations = 0
        while (game.state.pendingDecision == null &&
            game.state.step != Step.POSTCOMBAT_MAIN &&
            iterations++ < 20
        ) {
            game.passPriority()
        }
    }
}
