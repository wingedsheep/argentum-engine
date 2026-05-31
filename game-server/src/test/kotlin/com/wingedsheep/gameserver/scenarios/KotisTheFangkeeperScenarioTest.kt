package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Kotis, the Fangkeeper (TDM) — {1}{B}{G}{U} Legendary Zombie Warrior, 2/1.
 *
 * "Indestructible. Whenever Kotis deals combat damage to a player, exile the top X cards of
 * their library, where X is the amount of damage dealt. You may cast any number of spells with
 * mana value X or less from among them without paying their mana costs."
 *
 * Exercises the reusable `CastAnyNumberFromCollectionWithoutPayingCostEffect` loop and the
 * filter chain that feeds it (GatherCards(top of triggering player's library) → exile →
 * keep nonland → keep mana value ≤ X). Kotis is a 2/1, so X = 2 each combat: the top two cards
 * of the defender's library are exiled, and only nonland cards with mana value ≤ 2 among them
 * are offered to be cast for free, **during the trigger's resolution**.
 *
 * The casts happen mid-resolution (not via an until-end-of-turn grant), so:
 *  - card-type timing is ignored — a *creature* (sorcery-speed) is cast during the combat
 *    damage step;
 *  - the controller can't wait — declining grants no lingering may-play permission, and uncast
 *    cards just stay exiled.
 */
class KotisTheFangkeeperScenarioTest : ScenarioTestBase() {

    init {
        context("Kotis, the Fangkeeper combat damage trigger") {

            test("offers only the mana-value-≤-X spells and casts the chosen one for free during resolution") {
                // Defender's top two: Grizzly Bears (MV 2, castable) and Hill Giant (MV 4, too big).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kotis, the Fangkeeper", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kotis, the Fangkeeper" to 2))
                advanceToCastDecision(game)

                withClue("Kotis (2/1) deals 2 combat damage to the defender") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("X = 2 → the top two cards of the defender's library are exiled") {
                    namesInExile(game, 2) shouldBe setOf("Grizzly Bears", "Hill Giant")
                }

                // The trigger pauses mid-resolution to let Player1 cast from among the exiled cards.
                val decision = game.getPendingDecision()
                withClue("Kotis pauses for a cast-from-exile choice during resolution") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                withClue("Only the nonland card with mana value ≤ X (Grizzly Bears) is offered; Hill Giant (MV 4) is not") {
                    optionNames(game, decision) shouldBe setOf("Grizzly Bears")
                }

                // Cast Grizzly Bears for free. Player1 has no mana, and it's a creature cast during
                // the combat damage step — proving the cast is free and ignores sorcery-speed timing.
                val bearsId = decision.options.first { idNamed(game, it, "Grizzly Bears") }
                game.selectCards(listOf(bearsId))
                game.resolveStack()

                withClue("The free-cast Grizzly Bears resolves onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant (MV 4 > X) was never castable and stays in exile") {
                    namesInExile(game, 2).contains("Hill Giant") shouldBe true
                }
            }

            test("casts several spells in one resolution — a targeted spell pauses, then the loop offers the next") {
                // Defender's top two: Shock (MV 1, targets any target) and Grizzly Bears (MV 2).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kotis, the Fangkeeper", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Shock")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kotis, the Fangkeeper" to 2))
                advanceToCastDecision(game)

                // Both MV-≤-2 spells are offered together.
                val first = game.getPendingDecision() as SelectCardsDecision
                withClue("Both exiled spells (MV ≤ 2) are castable") {
                    optionNames(game, first) shouldBe setOf("Shock", "Grizzly Bears")
                }

                // Cast Shock first — it targets, so the loop pauses for target selection mid-cast.
                val shockId = first.options.first { idNamed(game, it, "Shock") }
                game.selectCards(listOf(shockId))
                withClue("Casting the targeted Shock pauses for its target during the loop") {
                    (game.getPendingDecision() is com.wingedsheep.engine.core.ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(game.player2Id))

                // After Shock is on the stack the loop re-enters and offers the remaining spell.
                val second = game.getPendingDecision() as SelectCardsDecision
                withClue("After the targeted cast, the loop offers only the remaining spell") {
                    optionNames(game, second) shouldBe setOf("Grizzly Bears")
                }
                val bearsId = second.options.first { idNamed(game, it, "Grizzly Bears") }
                game.selectCards(listOf(bearsId))

                // Both spells are now on the stack; let them resolve.
                game.resolveStack()

                withClue("Grizzly Bears (the second free cast) resolves onto the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("Shock dealt 2 to the defender on top of the 2 combat damage (20 → 18 → 16)") {
                    game.getLifeTotal(2) shouldBe 16
                }
                withClue("Both exiled spells were cast, so neither remains in exile") {
                    namesInExile(game, 2) shouldBe emptySet()
                }
            }

            test("lands are never offered, and declining leaves the cards exiled with no later permission") {
                // Defender's top two: Shock (MV 1, a spell) and Forest (a land).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kotis, the Fangkeeper", tapped = false, summoningSickness = false)
                    .withCardInLibrary(2, "Shock")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Kotis, the Fangkeeper" to 2))
                advanceToCastDecision(game)

                val decision = game.getPendingDecision()
                decision as SelectCardsDecision
                withClue("'You may cast spells' — the Forest (a land) is not offered, only Shock") {
                    optionNames(game, decision) shouldBe setOf("Shock")
                }

                // Decline the cast entirely.
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("Both cards remain in exile when the cast is declined") {
                    namesInExile(game, 2) shouldBe setOf("Shock", "Forest")
                }
                withClue("Per the rulings the casts can't wait — no may-play permission lingers for Player1") {
                    playablePermissionCardIds(game).intersect(
                        game.state.getExile(game.player2Id).toSet()
                    ) shouldBe emptySet()
                }
            }
        }
    }

    /**
     * Declare-attackers is already set; advance to the combat damage step (auto-submitting the
     * defender's empty blockers along the way), where Kotis deals damage and its trigger goes on
     * the stack, then pass priority until the trigger pauses mid-resolution for the cast choice.
     */
    private fun advanceToCastDecision(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var iterations = 0
        while (game.state.pendingDecision == null &&
            game.state.step != Step.POSTCOMBAT_MAIN &&
            iterations++ < 20
        ) {
            game.passPriority()
        }
    }

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    private fun idNamed(game: TestGame, id: EntityId, name: String): Boolean =
        game.state.getEntity(id)?.get<CardComponent>()?.name == name

    private fun namesInExile(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }.toSet()
    }

    private fun playablePermissionCardIds(game: TestGame): Set<EntityId> =
        game.state.mayPlayPermissions
            .filter { it.controllerId == game.player1Id }
            .flatMap { it.cardIds }
            .toSet()
}
