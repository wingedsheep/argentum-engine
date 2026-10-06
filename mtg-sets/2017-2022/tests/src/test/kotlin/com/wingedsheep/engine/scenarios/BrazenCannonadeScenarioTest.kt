package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Brazen Cannonade (J22) — {3}{R} Enchantment.
 *
 * "Whenever an attacking creature you control dies, this enchantment deals 2 damage to each
 * opponent. Raid — At the beginning of each of your postcombat main phases, if you attacked this
 * turn, exile the top card of your library. Until end of combat on your next turn, you may play
 * that card."
 *
 * The play window is composed from two grants (this turn + a phase-gated next-turn grant), so the
 * test walks it across turns: open for the rest of this turn, through the opponent's turn, and
 * your next turn up to combat; closed from that turn's postcombat main phase on.
 */
class BrazenCannonadeScenarioTest : ScenarioTestBase() {

    init {
        context("death trigger") {
            test("an attacking creature you control dying deals 2 damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brazen Cannonade")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("Grizzly Bears died blocked by Hill Giant") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("The attacking creature's death dealt 2 damage to the opponent") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("a non-attacking creature you control dying deals no damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brazen Cannonade")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                withClue("Bears wasn't attacking, so the opponent takes no damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("raid") {
            test("no attack this turn — nothing is exiled at the postcombat main phase") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brazen Cannonade")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                game.state.getExile(game.player1Id).size shouldBe 0
                game.librarySize(1) shouldBe 1
            }

            test("the exiled card is playable until end of combat on your next turn, and no longer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brazen Cannonade")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                val exiled = game.state.getExile(game.player1Id).single()
                withClue("The raid trigger exiled the top card of the library") {
                    game.state.getEntity(exiled)?.get<CardComponent>()?.name shouldBe "Lightning Bolt"
                }
                withClue("Playable for the rest of this turn") {
                    canCast(game, exiled) shouldBe true
                }

                // Opponent's turn: Player2 passes in their upkeep, handing Player1 priority.
                advanceTo(game, game.player2Id, Phase.BEGINNING, Step.UPKEEP)
                game.execute(PassPriority(game.player2Id)).error shouldBe null
                withClue("Playable during the opponent's turn") {
                    canCast(game, exiled) shouldBe true
                }

                advanceTo(game, game.player1Id, Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("Playable in your next turn's precombat main phase") {
                    canCast(game, exiled) shouldBe true
                }

                advanceTo(game, game.player1Id, Phase.COMBAT, Step.BEGIN_COMBAT)
                withClue("Playable during your next turn's combat") {
                    canCast(game, exiled) shouldBe true
                }

                advanceTo(game, game.player1Id, Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("Combat on your next turn has ended — the card can no longer be played") {
                    canCast(game, exiled) shouldBe false
                    game.state.getExile(game.player1Id).contains(exiled) shouldBe true
                }

                advanceTo(game, game.player2Id, Phase.BEGINNING, Step.UPKEEP)
                withClue("The permission is gone once that turn has ended") {
                    game.state.mayPlayPermissions.none { exiled in it.cardIds } shouldBe true
                }
            }
        }
    }

    private fun canCast(game: TestGame, cardId: EntityId): Boolean =
        game.getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

    /** Advance to [phase]/[step] of [activePlayer]'s turn, passing through other players' instances. */
    private fun advanceTo(game: TestGame, activePlayer: EntityId, phase: Phase, step: Step) {
        var guard = 0
        while (!(game.state.activePlayerId == activePlayer && game.state.phase == phase && game.state.step == step)) {
            check(guard++ < 30) { "Could not reach $phase/$step for $activePlayer" }
            if (game.state.phase == phase && game.state.step == step) game.passPriority()
            game.passUntilPhase(phase, step)
        }
    }
}
