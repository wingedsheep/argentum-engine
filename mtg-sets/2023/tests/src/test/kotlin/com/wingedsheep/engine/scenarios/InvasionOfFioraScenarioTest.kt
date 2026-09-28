package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Fiora // Marchesa, Resolute Monarch.
 *
 * Front: "choose one or both — destroy all legendary creatures / destroy all nonlegendary
 * creatures". Back: menace, deathtouch; an attack trigger that removes all counters from up to one
 * target permanent; and "at the beginning of your upkeep, if you haven't been dealt combat damage
 * since your last turn, you draw a card and you lose 1 life" — the window runs from the end of your
 * previous turn, so combat damage on an opponent's turn switches it off for exactly one upkeep.
 */
class InvasionOfFioraScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseMode(label: String) {
        val decision = state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a mode ChooseOptionDecision; got ${state.pendingDecision}")
        val index = decision.options.indexOfFirst { it.startsWith(label) }
        require(index >= 0) { "mode '$label' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun TestGame.chooseDone() {
        val decision = state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision offering decline; got ${state.pendingDecision}")
        val index = decision.options.indexOf(ModalEffectExecutor.DECLINE_MODE_LABEL)
        require(index >= 0) { "decline option not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    private fun frontGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Invasion of Fiora")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .withCardOnBattlefield(1, "Isamaru, Hound of Konda")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.defeatSiegeAndCastBack() {
        checkStateBasedActions()
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackersWithPermanentTargets(
            permanentAttackers = mapOf("Serra Angel" to "Invasion of Fiora")
        ).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && guard++ < 30) {
            if (state.step == Step.DECLARE_BLOCKERS &&
                state.getEntity(player2Id)
                    ?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
            ) {
                declareNoBlockers()
            } else {
                passPriority()
            }
        }
        answerYesNo(true).error shouldBe null
        resolveStack()
    }

    /** Pass through turns until [playerId]'s upkeep, stopping with its triggers on the stack. */
    private fun TestGame.passToUpkeepOf(playerId: EntityId) {
        do {
            if (state.step == Step.UPKEEP) passUntilPhase(Phase.BEGINNING, Step.DRAW)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        } while (state.activePlayerId != playerId)
    }

    /** Marchesa on the battlefield after player 1 defeats the Siege on turn 1. */
    private fun backFaceGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Invasion of Fiora")
        .withCardOnBattlefield(1, "Serra Angel", summoningSickness = false)
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        .apply { repeat(6) { withCardInLibrary(1, "Island") } }
        .apply { repeat(6) { withCardInLibrary(2, "Island") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also {
            it.defeatSiegeAndCastBack()
            it.isOnBattlefield("Marchesa, Resolute Monarch") shouldBe true
        }

    init {
        context("front face — Invasion of Fiora") {
            test("legendary mode only destroys legendary creatures") {
                val game = frontGame()
                game.castSpell(1, "Invasion of Fiora").error shouldBe null
                game.resolveStack()
                game.chooseMode("Destroy all legendary")
                game.chooseDone()
                game.resolveStack()

                game.isInGraveyard(1, "Isamaru, Hound of Konda") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("choosing both modes destroys every creature") {
                val game = frontGame()
                game.castSpell(1, "Invasion of Fiora").error shouldBe null
                game.resolveStack()
                game.chooseMode("Destroy all legendary")
                game.chooseMode("Destroy all nonlegendary")
                game.resolveStack()

                game.isInGraveyard(1, "Isamaru, Hound of Konda") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
        }

        context("back face — Marchesa, Resolute Monarch") {
            test("no combat damage since your last turn — draw a card and lose 1 life") {
                val game = backFaceGame()
                game.passToUpkeepOf(game.player1Id)
                val handBefore = game.handSize(1)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
                game.getLifeTotal(1) shouldBe 19
            }

            test("combat damage on the opponent's turn skips exactly one upkeep") {
                val game = backFaceGame()
                game.passToUpkeepOf(game.player2Id)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.getLifeTotal(1) shouldBe 18

                game.passToUpkeepOf(game.player1Id)
                val handBefore = game.handSize(1)
                game.resolveStack()
                withClue("dealt combat damage since your last turn — no trigger") {
                    game.handSize(1) shouldBe handBefore
                    game.getLifeTotal(1) shouldBe 18
                }

                // The opponent's next turn passes without an attack; the window reopened when
                // player 1's own turn ended, so the next upkeep draws again.
                game.passToUpkeepOf(game.player2Id)
                game.passToUpkeepOf(game.player1Id)
                val handAfter = game.handSize(1)
                game.resolveStack()
                game.handSize(1) shouldBe handAfter + 1
                game.getLifeTotal(1) shouldBe 17
            }

            test("attacking removes all counters from the target permanent") {
                val game = backFaceGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.STUN to 1)))
                }
                game.passToUpkeepOf(game.player1Id)
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marchesa, Resolute Monarch" to 2)).error shouldBe null
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                (game.state.getEntity(bears)?.get<CountersComponent>()?.counters ?: emptyMap())
                    .values.sum() shouldBe 0
            }
        }
    }
}
