package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Curse of Surveillance (MID #46) — {4}{U} Enchantment — Aura Curse.
 *
 * "Enchant player
 *  At the beginning of enchanted player's upkeep, any number of target players other than that
 *  player each draw cards equal to the number of Curses attached to that player."
 *
 * The Curses are cast for real, so the count reads the attachments the enchanted *player* carries;
 * the restriction is read against the player this Curse is attached to, not its controller.
 */
class CurseOfSurveillanceScenarioTest : ScenarioTestBase() {

    private fun TestGame.idOf(playerNumber: Int): EntityId = if (playerNumber == 1) player1Id else player2Id

    /**
     * Player 1 casts Curse of Surveillance on [cursed], plus Fraying Sanity on the same player when
     * [secondCurse], then passes into the next upkeep of [cursed].
     */
    private fun cursedGame(cursed: Int, secondCurse: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Curse of Surveillance")
            .withLandsOnBattlefield(1, "Island", 8)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (secondCurse) builder.withCardInHand(1, "Fraying Sanity")
        repeat(6) {
            builder.withCardInLibrary(1, "Island")
            builder.withCardInLibrary(2, "Island")
        }
        val game = builder.build()

        game.castSpellTargetingPlayer(1, "Curse of Surveillance", cursed).error shouldBe null
        game.resolveStack()
        if (secondCurse) {
            game.castSpellTargetingPlayer(1, "Fraying Sanity", cursed).error shouldBe null
            game.resolveStack()
        }
        val curse = game.findPermanent("Curse of Surveillance")!!
        game.state.getEntity(curse)?.get<AttachedToComponent>()?.targetId shouldBe game.idOf(cursed)

        // Pass to the cursed player's next upkeep (player 2's this turn cycle, player 1's the one
        // after), stopping at the trigger's target choice.
        var guard = 0
        while (game.state.pendingDecision == null && guard++ < 200 &&
            !(game.state.activePlayerId == game.idOf(cursed) && game.state.step == Step.DRAW)
        ) {
            game.execute(PassPriority(game.state.priorityPlayerId ?: error("no priority at ${game.state.step}")))
        }
        return game
    }

    private fun TestGame.targetDecision(): ChooseTargetsDecision {
        val decision = state.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        return decision
    }

    init {
        context("Curse of Surveillance") {

            test("on an opponent with two Curses: only its controller is a legal target, and draws two") {
                val game = cursedGame(cursed = 2, secondCurse = true)
                val decision = game.targetDecision()
                withClue("the decision is the Curse controller's") { decision.playerId shouldBe game.player1Id }
                withClue("the enchanted player can't be targeted") {
                    decision.legalTargets[0] shouldBe listOf(game.player1Id)
                }

                val handBefore = game.handSize(1)
                game.selectTargets(listOf(game.player1Id)).error shouldBe null
                game.resolveStack()
                withClue("two Curses are attached to player 2") { game.handSize(1) shouldBe handBefore + 2 }
            }

            test("a lone Curse counts itself") {
                val game = cursedGame(cursed = 2, secondCurse = false)
                val handBefore = game.handSize(1)
                game.selectTargets(listOf(game.player1Id)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 1
            }

            test("choosing no targets draws nothing") {
                val game = cursedGame(cursed = 2, secondCurse = true)
                val p1Before = game.handSize(1)
                val p2Before = game.handSize(2)
                game.skipTargets().error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe p1Before
                game.handSize(2) shouldBe p2Before
            }

            test("cursing yourself: the opponent is the only target and it fires on your upkeep") {
                val game = cursedGame(cursed = 1, secondCurse = false)
                withClue("it is player 1's upkeep") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.step shouldBe Step.UPKEEP
                }
                val decision = game.targetDecision()
                decision.legalTargets[0] shouldBe listOf(game.player2Id)

                val handBefore = game.handSize(2)
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()
                game.handSize(2) shouldBe handBefore + 1
            }
        }
    }
}
