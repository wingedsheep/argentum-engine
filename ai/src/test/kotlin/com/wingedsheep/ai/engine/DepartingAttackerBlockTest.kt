package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.departuresAreGone]: an attacker due to be sacrificed at this turn's end step — the
 * token copy Gray and Adira, Forever Joined makes — is not worth creatures to kill. The AI takes
 * the hit, or chumps it when the life is needed, instead of gang-blocking it.
 *
 * The 2026-11 report: a human swung a 5/6 token copy that was leaving at the end step, and the AI
 * blocked it with four creatures and lost some of them to kill it.
 */
class DepartingAttackerBlockTest : ScenarioTestBase() {

    /**
     * Seat 1 attacks with a Spined Wurm (5/4) that a delayed trigger sacrifices at the end step
     * when [departing]; seat 2 (the AI) holds the shape of the reported board — a 2/1, a 2/1, a 2/4
     * and a 2/3, two of which together kill the Wurm and lose one of themselves doing it.
     */
    private fun board(ourLife: Int, departing: Boolean): TestGame {
        var b = scenario().withRngSeed(20261108L).withPlayers()
            .withLifeTotal(2, ourLife)
            .withCardOnBattlefield(1, WURM)
        listOf("Savannah Lions", "Goblin Piker", "Giant Spider", "Pensive Minotaur")
            .forEach { b = b.withCardOnBattlefield(2, it) }
        repeat(10) { b = b.withCardInLibrary(1, "Craw Wurm").withCardInLibrary(2, "Craw Wurm") }
        val game = b.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
        val wurm = game.findPermanent(WURM)!!
        if (departing) {
            game.state = game.state.addDelayedTrigger(
                DelayedTriggeredAbility(
                    id = "sacrifice-wurm",
                    effect = SacrificeTargetEffect(EffectTarget.SpecificEntity(wurm)),
                    fireAtStep = Step.END,
                    sourceId = wurm,
                    sourceName = WURM,
                    controllerId = game.seatId(1),
                ),
            )
        }
        game.execute(DeclareAttackers(game.seatId(1), mapOf(wurm to game.seatId(2))))
        return game.advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
    }

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.blockersOnWurm(profile: AiProfile): List<String?> {
        val wurm = findPermanent(WURM)!!
        val blocks = AIPlayer.create(cardRegistry, seatId(2), profile).chooseAction(state) as DeclareBlockers
        return blocks.blockers.filterValues { wurm in it }.keys.map { nameOf(it) }
    }

    private companion object {
        const val WURM = "Spined Wurm"
    }

    init {
        test("a Spined Wurm that stays is worth ganging up on") {
            val blockers = board(ourLife = 20, departing = false).blockersOnWurm(AiProfile.LIVE)
            withClue("blocked with $blockers") { (blockers.size >= 2) shouldBe true }
        }

        test("a Spined Wurm sacrificed at the end step is not worth a creature: take the five") {
            val blockers = board(ourLife = 20, departing = true).blockersOnWurm(AiProfile.LIVE)
            withClue("blocked with $blockers") { blockers.shouldBeEmpty() }
        }

        test("reading the departing Wurm as a lasting creature gang-blocks it") {
            val blockers = board(ourLife = 20, departing = true)
                .blockersOnWurm(AiProfile.LIVE.copy(id = "t", departuresAreGone = false))
            withClue("blocked with $blockers") { (blockers.size >= 2) shouldBe true }
        }

        test("facing lethal from a departing Wurm, one creature chumps it") {
            val blockers = board(ourLife = 5, departing = true).blockersOnWurm(AiProfile.LIVE)
            withClue("blocked with $blockers") { blockers.size shouldBe 1 }
        }
    }
}
