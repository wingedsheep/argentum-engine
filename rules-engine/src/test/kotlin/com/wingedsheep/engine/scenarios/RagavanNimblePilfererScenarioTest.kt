package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.DashedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Ragavan, Nimble Pilferer (MH2) — {R} Legendary Monkey Pirate, 2/1.
 *
 * "Whenever Ragavan deals combat damage to a player, create a Treasure token and exile the top
 * card of that player's library. Until end of turn, you may cast that card.
 * Dash {1}{R}"
 *
 * Covers both halves: the combat-damage trigger (Treasure + exile + a normal-cost, normal-timing
 * "may cast" grant — not a synthesized free cast, per the official ruling) and the Dash
 * alternative cost (haste, returns to hand at the next end step).
 */
class RagavanNimblePilfererScenarioTest : ScenarioTestBase() {

    init {
        context("Ragavan, Nimble Pilferer — combat damage trigger") {

            test("creates a Treasure, exiles the defender's top library card, and grants a cast permission") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragavan, Nimble Pilferer", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2))
                advanceThroughCombatDamage(game)

                withClue("Ragavan (2/1) deals 2 combat damage to the defender") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("A Treasure token is created for Ragavan's controller") {
                    game.isOnBattlefield("Treasure") shouldBe true
                }
                withClue("The top card of the defender's library is exiled") {
                    namesInExile(game, 2) shouldBe setOf("Grizzly Bears")
                }

                val exiledCardId = exiledCardNamed(game, 2, "Grizzly Bears")
                withClue("Player1 (Ragavan's controller) has a cast permission for the exiled card") {
                    game.state.mayPlayPermissions.any {
                        it.controllerId == game.player1Id && exiledCardId in it.cardIds
                    } shouldBe true
                }
            }

            test("creates a Treasure even when the defending player's library is empty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragavan, Nimble Pilferer", tapped = false, summoningSickness = false)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2))
                advanceThroughCombatDamage(game)

                withClue("Ragavan still deals its combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("The Treasure is created regardless of the empty library") {
                    game.isOnBattlefield("Treasure") shouldBe true
                }
                withClue("Nothing was exiled — the defender's library was empty") {
                    namesInExile(game, 2) shouldBe emptySet()
                }
            }

            test("the exiled card can be cast by Ragavan's controller, following normal cost and timing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragavan, Nimble Pilferer", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2))
                advanceThroughCombatDamage(game)

                val exiledCardId = exiledCardNamed(game, 2, "Grizzly Bears")

                withClue("Grizzly Bears is a creature — sorcery-speed timing still applies, so it can't be cast mid-combat") {
                    game.execute(CastSpell(playerId = game.player1Id, cardId = exiledCardId)).error shouldNotBe null
                }

                // "Until end of turn" — the permission persists into the postcombat main phase.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.execute(CastSpell(playerId = game.player1Id, cardId = exiledCardId)).error shouldBe null
                game.resolveStack()

                withClue("The exiled Grizzly Bears resolved onto the battlefield under Player1's control") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("an exiled land can't be played through the cast permission") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragavan, Nimble Pilferer", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2))
                advanceThroughCombatDamage(game)

                val exiledLandId = exiledCardNamed(game, 2, "Forest")

                withClue("'You may cast' never authorizes playing a land — the legal actions omit it") {
                    val enumerator = LegalActionEnumerator.create(cardRegistry)
                    val legalActions = enumerator.enumerate(game.state, game.player1Id)
                    legalActions.none { action ->
                        (action.action as? PlayLand)?.cardId == exiledLandId
                    } shouldBe true
                }

                withClue("The authoritative handler rejects the play too, not just the enumerator") {
                    game.execute(PlayLand(playerId = game.player1Id, cardId = exiledLandId)).error shouldNotBe null
                }
            }

            test("blocked and dealing no combat damage to a player does not trigger the ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragavan, Nimble Pilferer", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(2, "Wall of Wood", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Wood" to listOf("Ragavan, Nimble Pilferer")))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (game.state.pendingDecision == null && game.state.stack.isNotEmpty() && iterations++ < 20) {
                    game.passPriority()
                }

                withClue("Ragavan's combat damage went to the 0/3 blocker, not the defending player") {
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("Neither creature dies (2 vs. 3 toughness, 0 power vs. 1 toughness)") {
                    game.isOnBattlefield("Ragavan, Nimble Pilferer") shouldBe true
                    game.isOnBattlefield("Wall of Wood") shouldBe true
                }
                withClue("No Treasure — the trigger requires combat damage to a player") {
                    game.isOnBattlefield("Treasure") shouldBe false
                }
                withClue("Nothing was exiled") {
                    namesInExile(game, 2) shouldBe emptySet()
                }
            }

            test("the cast permission expires with the turn — it doesn't carry into a later turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragavan, Nimble Pilferer", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2))
                advanceThroughCombatDamage(game)

                val exiledCardId = exiledCardNamed(game, 2, "Grizzly Bears")
                withClue("The permission exists during the turn Ragavan connected") {
                    game.state.mayPlayPermissions.any {
                        it.controllerId == game.player1Id && exiledCardId in it.cardIds
                    } shouldBe true
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()

                withClue("Player2's turn has begun") {
                    game.state.activePlayerId shouldBe game.player2Id
                }
                withClue("\"Until end of turn\" no longer covers the exiled card once that turn has ended") {
                    game.state.mayPlayPermissions.none {
                        it.controllerId == game.player1Id && exiledCardId in it.cardIds
                    } shouldBe true
                }
            }
        }

        context("Ragavan, Nimble Pilferer — Dash") {

            test("dashed Ragavan gains haste, can attack immediately, and returns to hand at the next end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ragavan, Nimble Pilferer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithAlternativeCost(1, "Ragavan, Nimble Pilferer").error shouldBe null
                game.resolveStack()

                val ragavan = game.findPermanent("Ragavan, Nimble Pilferer")!!
                withClue("Dashed Ragavan is marked and has haste despite summoning sickness") {
                    game.state.getEntity(ragavan)?.has<DashedComponent>() shouldBe true
                    game.state.projectedState.hasKeyword(ragavan, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ragavan, Nimble Pilferer" to 2)).error shouldBe null

                withClue("Ragavan is still on the battlefield through combat") {
                    game.isOnBattlefield("Ragavan, Nimble Pilferer") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("The dash delayed trigger returns Ragavan to hand at the next end step") {
                    game.isOnBattlefield("Ragavan, Nimble Pilferer") shouldBe false
                    game.state.getHand(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ragavan, Nimble Pilferer"
                    } shouldBe true
                }
            }

            test("Ragavan cast for its normal mana cost does not gain haste or get returned to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ragavan, Nimble Pilferer")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ragavan, Nimble Pilferer").error shouldBe null
                game.resolveStack()

                val ragavan = game.findPermanent("Ragavan, Nimble Pilferer")!!
                game.state.getEntity(ragavan)?.has<DashedComponent>() shouldBe false
                game.state.projectedState.hasKeyword(ragavan, Keyword.HASTE) shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("A normally-cast Ragavan just stays on the battlefield") {
                    game.isOnBattlefield("Ragavan, Nimble Pilferer") shouldBe true
                }
            }

            test("a dashed Ragavan that dies before the next end step stays in the graveyard, not returned to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ragavan, Nimble Pilferer")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithAlternativeCost(1, "Ragavan, Nimble Pilferer").error shouldBe null
                game.resolveStack()

                val ragavan = game.findPermanent("Ragavan, Nimble Pilferer")!!
                game.castSpell(1, "Lightning Bolt", targetId = ragavan).error shouldBe null
                game.resolveStack()

                withClue("Ragavan (2/1) died to Lightning Bolt (3 damage) before the end step") {
                    game.isOnBattlefield("Ragavan, Nimble Pilferer") shouldBe false
                    game.state.getGraveyard(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ragavan, Nimble Pilferer"
                    } shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("The delayed trigger only returns Ragavan if it's still on the battlefield when it resolves — it stays in the graveyard") {
                    game.state.getGraveyard(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ragavan, Nimble Pilferer"
                    } shouldBe true
                    game.state.getHand(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ragavan, Nimble Pilferer"
                    } shouldBe false
                }
            }

            test("a token copy of a dashed Ragavan has no haste and isn't returned to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ragavan, Nimble Pilferer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(2, "Test Token Copy")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithAlternativeCost(1, "Ragavan, Nimble Pilferer").error shouldBe null
                game.resolveStack()

                val original = game.findPermanent("Ragavan, Nimble Pilferer")!!
                withClue("The original dashed Ragavan has haste") {
                    game.state.projectedState.hasKeyword(original, Keyword.HASTE) shouldBe true
                }

                // Instants can be cast by either player on priority — hand priority to Player2
                // so they can copy Player1's still-dashed Ragavan before it returns to hand.
                game.execute(PassPriority(game.player1Id)).error shouldBe null
                game.castSpell(2, "Test Token Copy", targetId = original).error shouldBe null
                game.resolveStack()

                val copy = game.findPermanents("Ragavan, Nimble Pilferer").first { it != original }
                withClue("The copy (a new object controlled by Player2) does not carry over the dash haste grant") {
                    game.state.getEntity(copy)?.has<DashedComponent>() shouldBe false
                    game.state.projectedState.hasKeyword(copy, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("The original dashed Ragavan returns to Player1's hand at the next end step") {
                    game.state.getBattlefield().contains(original) shouldBe false
                    game.state.getHand(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ragavan, Nimble Pilferer"
                    } shouldBe true
                }
                withClue("The copy was never dashed, so it just stays on Player2's battlefield") {
                    game.state.getBattlefield().contains(copy) shouldBe true
                }
            }
        }
    }

    /**
     * Declare-attackers is already set; advance through the combat damage step (auto-submitting
     * the defender's empty blockers), letting Ragavan's trigger resolve.
     */
    private fun advanceThroughCombatDamage(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var iterations = 0
        while (game.state.pendingDecision == null &&
            game.state.stack.isNotEmpty() &&
            iterations++ < 20
        ) {
            game.passPriority()
        }
    }

    private fun namesInExile(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }.toSet()
    }

    private fun exiledCardNamed(game: TestGame, playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }
}
