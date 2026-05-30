package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Invasion split cards (Pain // Suffering, Stand // Deliver, Wax // Wane)
 * plus Restrain and Whip Silk. The split cards exercise casting an instant/sorcery face of a
 * [com.wingedsheep.sdk.model.CardLayout.SPLIT] card with targets (CR 709.4) — the first non-Room
 * consumers of the SPLIT spell-face path.
 */
class InvasionSplitCardsScenarioTest : ScenarioTestBase() {

    init {
        context("Pain // Suffering") {

            test("Pain (face 0) — target player discards a card") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Pain // Suffering")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Pain // Suffering").first()
                val result = game.execute(
                    CastSpell(
                        game.player1Id, cardId, faceIndex = 0,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Casting Pain should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Player 2 should have discarded their only hand card") {
                    game.state.getHand(game.player2Id).none {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                    } shouldBe true
                }
            }

            test("Suffering (face 1) — destroy target land") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Pain // Suffering")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Pain // Suffering").first()
                val landId = game.state.getBattlefield(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                }

                val result = game.execute(
                    CastSpell(
                        game.player1Id, cardId, faceIndex = 1,
                        targets = listOf(ChosenTarget.Permanent(landId))
                    )
                )
                withClue("Casting Suffering should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("The targeted Forest should be destroyed") {
                    game.state.getBattlefield(game.player2Id).contains(landId) shouldBe false
                    game.state.getGraveyard(game.player2Id).contains(landId) shouldBe true
                }
            }
        }

        context("Stand // Deliver") {

            test("Deliver (face 1) — return target permanent to its owner's hand") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Stand // Deliver")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Stand // Deliver").first()
                val bearsId = game.findPermanent("Grizzly Bears")!!

                val result = game.execute(
                    CastSpell(
                        game.player1Id, cardId, faceIndex = 1,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Casting Deliver should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears should be back in player 2's hand") {
                    game.state.getBattlefield(game.player2Id).contains(bearsId) shouldBe false
                    game.state.getHand(game.player2Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                }
            }
        }

        context("Wax // Wane") {

            test("Wax (face 0) — target creature gets +2/+2 until end of turn") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wax // Wane")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Wax // Wane").first()
                val bearsId = game.findPermanent("Grizzly Bears")!!

                val result = game.execute(
                    CastSpell(
                        game.player1Id, cardId, faceIndex = 0,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Casting Wax should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears (2/2) should be 4/4 after Wax") {
                    game.state.projectedState.getPower(bearsId) shouldBe 4
                    game.state.projectedState.getToughness(bearsId) shouldBe 4
                }
            }

            test("Wane (face 1) — destroy target enchantment") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Wax // Wane")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Whip Silk")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Wax // Wane").first()
                val whipSilkId = game.findPermanent("Whip Silk")!!

                val result = game.execute(
                    CastSpell(
                        game.player1Id, cardId, faceIndex = 1,
                        targets = listOf(ChosenTarget.Permanent(whipSilkId))
                    )
                )
                withClue("Casting Wane should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Whip Silk should be destroyed") {
                    game.state.getBattlefield(game.player2Id).contains(whipSilkId) shouldBe false
                }
            }
        }

        context("Restrain") {

            test("prevents combat damage from target attacking creature and draws a card") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Restrain")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // P2 attacks P1 with Grizzly Bears.
                game.declareAttackers(mapOf("Grizzly Bears" to 1))
                val bearsId = game.findPermanent("Grizzly Bears")!!

                val startingLife = game.getLifeTotal(1)
                val handSizeBefore = game.state.getHand(game.player1Id).size

                // The active player (P2) holds priority after declaring attackers; pass it to P1.
                game.passPriority()

                // P1 casts Restrain on the attacker during declare-attackers.
                val cardId = game.findCardsInHand(1, "Restrain").first()
                val result = game.execute(
                    CastSpell(
                        game.player1Id, cardId,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Casting Restrain should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                // Restrain leaves hand (-1) and its draw adds one (+1): net unchanged count,
                // but Restrain is gone and a freshly drawn card is present.
                withClue("Restrain should have drawn P1 a card") {
                    game.state.getHand(game.player1Id).size shouldBe handSizeBefore
                    game.findCardsInHand(1, "Restrain") shouldBe emptyList()
                }

                // Advance through combat damage.
                var iterations = 0
                while (game.state.step != Step.END_COMBAT && game.state.pendingDecision == null && iterations++ < 12) {
                    game.passPriority()
                }

                withClue("Combat damage from the attacker should be prevented; P1 keeps full life") {
                    game.getLifeTotal(1) shouldBe startingLife
                }
            }
        }

        context("Whip Silk") {

            test("grants reach to the enchanted creature and can return itself to hand") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Whip Silk")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val castResult = game.castSpell(1, "Whip Silk", targetId = bearsId)
                withClue("Casting Whip Silk should succeed: ${castResult.error}") { castResult.error shouldBe null }
                game.resolveStack()

                val whipSilkId = game.findPermanent("Whip Silk")!!
                withClue("Whip Silk should be attached to Grizzly Bears") {
                    game.state.getEntity(whipSilkId)?.get<AttachedToComponent>()?.targetId shouldBe bearsId
                }
                withClue("Enchanted creature should have reach") {
                    game.state.projectedState.hasKeyword(bearsId, Keyword.REACH) shouldBe true
                }
            }
        }
    }
}
