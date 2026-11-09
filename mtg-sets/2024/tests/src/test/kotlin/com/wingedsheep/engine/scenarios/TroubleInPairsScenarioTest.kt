package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnSkippedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Trouble in Pairs (MKC #15) —
 *  "If an opponent would begin an extra turn, that player skips that turn instead.
 *   Whenever an opponent attacks you with two or more creatures, draws their second card each
 *   turn, or casts their second spell each turn, you draw a card."
 *
 * Player 1 controls Trouble in Pairs; player 2 is the opponent.
 */
class TroubleInPairsScenarioTest : ScenarioTestBase() {

    private val trouble = "Trouble in Pairs"

    private fun TestGame.takeExtraTurn(taker: Int) {
        val id = if (taker == 1) player1Id else player2Id
        val result = services.effectExecutorRegistry.execute(state, Effects.TakeExtraTurn(),
            EffectContext(sourceId = null, controllerId = id))
        result.error shouldBe null
        state = result.state
    }

    private fun TestGame.endTurn(): List<com.wingedsheep.engine.core.GameEvent> {
        val result = services.turnManager.endTurn(state)
        result.error shouldBe null
        state = result.state
        return result.events
    }

    private fun TestGame.begun(player: Int): Int =
        state.getEntity(if (player == 1) player1Id else player2Id)?.get<ExtraTurnsComponent>()?.begun ?: 0

    init {
        context("the extra-turn replacement") {
            fun endStep(configure: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
                .withPlayers("Controller", "Opponent")
                .withCardOnBattlefield(1, trouble)
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .configure()
                .withActivePlayer(2).withPriorityPlayer(2).inPhase(Phase.ENDING, Step.END).build()

            test("an opponent's extra turn is created, then skipped when it would begin") {
                val game = endStep()
                game.takeExtraTurn(2)
                withClue("creating the extra turn isn't prevented") {
                    game.state.getEntity(game.player2Id)!!.get<ExtraTurnsComponent>()!!.pending.size shouldBe 1
                }

                val events = game.endTurn()
                val skipped = events.filterIsInstance<TurnSkippedEvent>().single()
                skipped.playerId shouldBe game.player2Id
                skipped.sourceId shouldBe game.findPermanent(trouble)
                withClue("the skipped extra turn never begins, so the controller's regular turn is next") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.begun(2) shouldBe 0
                    game.state.getEntity(game.player2Id)!!.get<ExtraTurnsComponent>()!!.pending.size shouldBe 0
                }
                game.endTurn()
                withClue("and the opponent's regular turn is not lost") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.begun(2) shouldBe 0
                }
            }

            test("if Trouble in Pairs leaves before the extra turn would begin, the turn happens") {
                val game = endStep { withCardInHand(2, "Disenchant").withLandsOnBattlefield(2, "Plains", 2) }
                game.takeExtraTurn(2)

                game.state = game.state.copy(priorityPlayerId = game.player2Id)
                game.castSpell(2, "Disenchant", game.findPermanent(trouble)).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield(trouble) shouldBe false

                val events = game.endTurn()
                events.filterIsInstance<TurnSkippedEvent>() shouldBe emptyList()
                withClue("the opponent takes the extra turn") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.begun(2) shouldBe 1
                }
            }

            test("its controller's own extra turns are not skipped") {
                val game = endStep()
                game.takeExtraTurn(1)
                game.endTurn()
                game.state.activePlayerId shouldBe game.player1Id
                game.begun(1) shouldBe 1
            }
        }

        context("the draw trigger") {
            fun mainPhase(configure: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
                .withPlayers("Controller", "Opponent")
                .withCardOnBattlefield(1, trouble)
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains")
                .configure()
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            test("an opponent's second card each turn draws a card, once even when one draw crosses it") {
                val game = mainPhase {
                    withCardInHand(2, "Divination").withLandsOnBattlefield(2, "Island", 3).withCardsDrawnThisTurn(2, 1)
                }
                val before = game.handSize(1)
                game.castSpell(2, "Divination").error shouldBe null
                game.resolveStack()
                withClue("Divination draws their second and third cards: one trigger") {
                    game.handSize(1) shouldBe before + 1
                }
            }

            test("a single card that is only their first of the turn doesn't trigger it") {
                val game = mainPhase { withCardInHand(2, "Think Twice").withLandsOnBattlefield(2, "Island", 2).withCardsDrawnThisTurn(2, 0) }
                val before = game.handSize(1)
                game.castSpell(2, "Think Twice").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before
            }

            test("a single card that is their second of the turn does") {
                val game = mainPhase { withCardInHand(2, "Think Twice").withLandsOnBattlefield(2, "Island", 2).withCardsDrawnThisTurn(2, 1) }
                val before = game.handSize(1)
                game.castSpell(2, "Think Twice").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
            }

            test("its controller's own second draw doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, trouble)
                    .withCardInHand(1, "Divination").withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1).withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val before = game.handSize(1)
                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before - 1 + 2
            }
        }

        context("the spell trigger") {
            fun shockBoard() = scenario()
                .withPlayers("Controller", "Opponent")
                .withCardOnBattlefield(1, trouble)
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withCardInHand(2, "Shock").withCardInHand(2, "Shock").withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            test("an opponent's second spell each turn draws a card — only the second") {
                val game = shockBoard()
                val before = game.handSize(1)

                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.state.stack.size shouldBe 1
                game.resolveStack()
                withClue("their first spell doesn't trigger it") { game.handSize(1) shouldBe before }

                game.state = game.state.copy(priorityPlayerId = game.player2Id)
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                withClue("the second spell triggers it: the trigger waits above the spell") { game.state.stack.size shouldBe 2 }
                game.resolveStack()
                game.handSize(1) shouldBe before + 1

                game.state = game.state.copy(priorityPlayerId = game.player2Id)
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.resolveStack()
                withClue("the third spell doesn't") { game.handSize(1) shouldBe before + 1 }
            }
        }

        context("the attack trigger") {
            fun combatBoard(configure: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
                .withPlayers("Controller", "Opponent")
                .withCardOnBattlefield(1, trouble)
                .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .configure()
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            test("an opponent attacking you with two creatures draws a card") {
                val game = combatBoard()
                val before = game.handSize(1)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before + 1
            }

            test("attacking you with a single creature doesn't") {
                val game = combatBoard()
                val before = game.handSize(1)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe before
            }

            test("each attack declaration with two or more creatures triggers it again") {
                val game = combatBoard {
                    withCardInHand(2, "Relentless Assault").withLandsOnBattlefield(2, "Mountain", 4)
                }
                val before = game.handSize(1)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                game.resolveStack()
                withClue("first combat") { game.handSize(1) shouldBe before + 1 }

                // Relentless Assault untaps the attackers and adds a combat phase (plus a main phase).
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.castSpell(2, "Relentless Assault").error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                game.resolveStack()
                withClue("second combat") { game.handSize(1) shouldBe before + 2 }
            }
        }

        context("independent clauses") {
            test("one turn can trigger it from an attack, a second draw and a second spell") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, trouble)
                    .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(2, "Shock").withCardInHand(2, "Shock").withCardInHand(2, "Divination")
                    .withLandsOnBattlefield(2, "Mountain", 2).withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains")
                    .withActivePlayer(2).withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                val before = game.handSize(1)

                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
                game.resolveStack()
                game.state = game.state.copy(priorityPlayerId = game.player2Id)
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null // second spell
                game.resolveStack()
                withClue("second spell") { game.handSize(1) shouldBe before + 1 }

                game.state = game.state.copy(priorityPlayerId = game.player2Id)
                game.castSpell(2, "Divination").error shouldBe null // their third spell, but draws their second card
                game.resolveStack()
                withClue("second card drawn") { game.handSize(1) shouldBe before + 2 }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state = game.state.copy(priorityPlayerId = game.player2Id)
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                game.resolveStack()
                withClue("attack: three separate triggers in one turn") { game.handSize(1) shouldBe before + 3 }
            }
        }
    }
}
