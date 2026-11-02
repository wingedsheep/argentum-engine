package com.wingedsheep.ai.engine

import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain

/**
 * The positions `CombatTradePropertyTest` found, one per fix: each pins what [AiProfile.LIVE] does
 * now and that switching that one flag off brings the mistake back.
 *
 * - [AiProfile.crackBackAfterCleanup] — a blocker's marked damage read as if it lasted into the
 *   crack-back.
 * - [com.wingedsheep.ai.engine.evaluation.CreatureValuation.thresholdsWholeBoard] — our own
 *   small creature counted in the share of every opposing creature that beats it, raising them
 *   all, so throwing it away scored as a gain.
 * - [AiProfile.soundBlockMath] — first strike against deathtouch, and double strike, in the block
 *   heuristic that predicts the defender.
 * - [AiProfile.blockSearchTriesSwaps] — blocking search could only reach a different body for a
 *   block in two steps (remove, then add), which the two-iteration search often never took.
 */
class CombatTradeFixesTest : ScenarioTestBase() {

    private fun ScenarioBuilder.on(seat: Int, names: List<String>) =
        names.fold(this) { b, n -> b.withCardOnBattlefield(seat, n) }

    private fun ScenarioBuilder.stocked(): ScenarioBuilder {
        var b = this
        repeat(10) { b = b.withCardInLibrary(1, "Craw Wurm").withCardInLibrary(2, "Craw Wurm") }
        return b
    }

    /** Seat 1 (the AI) at its declare-attackers step. */
    private fun attackBoard(ours: List<String>, theirs: List<String>, ourLife: Int, theirLife: Int): TestGame =
        scenario().withRngSeed(20261102L).withPlayers()
            .withLifeTotal(1, ourLife).withLifeTotal(2, theirLife)
            .on(1, ours).on(2, theirs).stocked()
            .build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.attackersChosenBy(profile: AiProfile): List<String?> =
        (AIPlayer.create(cardRegistry, seatId(1), profile).chooseAction(state) as? DeclareAttackers)
            ?.attackers?.keys.orEmpty().map { nameOf(it) }

    /** Seat 1 attacks with [attacker]; seat 2 (the AI) is asked to block. */
    private fun blockBoard(attacker: String, ours: List<String>, ourLife: Int): TestGame {
        val game = scenario().withRngSeed(20261102L).withPlayers()
            .withLifeTotal(2, ourLife)
            .on(1, listOf(attacker)).on(2, ours).stocked()
            .build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
        game.execute(DeclareAttackers(game.seatId(1), mapOf(game.findPermanent(attacker)!! to game.seatId(2))))
        return game.advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
    }

    init {
        context("marked damage is gone by the crack-back") {
            // A Hill Giant eats the Lions for free, keeping two damage until cleanup. Read with that
            // damage, the Giant looked killable by the Minotaur on its swing back.
            val board = { attackBoard(
                listOf("Pensive Minotaur", "Savannah Lions", "Trained Armodon"),
                listOf("Spined Wurm", "Hill Giant", "Hill Giant"), ourLife = 12, theirLife = 14,
            ) }

            test("does not send a lone Savannah Lions into Spined Wurm and two Hill Giants") {
                val attackers = board().attackersChosenBy(AiProfile.LIVE)
                withClue("attacked with $attackers") { attackers.shouldBeEmpty() }
            }

            test("reading the damage as permanent sends it") {
                board().attackersChosenBy(AiProfile.LIVE.copy(id = "t", crackBackAfterCleanup = false))
                    .shouldContain("Savannah Lions")
            }
        }

        context("losing our own small creature does not read as a gain") {
            val board = { attackBoard(
                listOf("Alpine Grizzly", "Savannah Lions"),
                listOf("Grizzly Bears", "Hill Giant", "Pensive Minotaur", "Hill Giant"), ourLife = 11, theirLife = 13,
            ) }

            test("does not throw the Lions away into four creatures that each kill it and survive") {
                val attackers = board().attackersChosenBy(AiProfile.LIVE)
                withClue("attacked with $attackers") { attackers.shouldNotContain("Savannah Lions") }
            }

            test("with fight bonuses paid by share, losing the Lions scores as a gain") {
                val shares = AiProfile.LIVE.copy(
                    id = "t",
                    creatureValuation = AiProfile.LIVE.creatureValuation.copy(thresholdsWholeBoard = false),
                )
                board().attackersChosenBy(shares).shouldContain("Savannah Lions")
            }
        }

        context("the defender model plays a one-on-one block out step by step") {
            val withoutSoundMath = AiProfile.LIVE.copy(id = "t", soundBlockMath = false)

            test("does not attack Typhoid Rats into a Youthful Knight that first-strikes it down") {
                // Read wrongly, the Knight is no free kill on the Rats, so the model trades their Rats
                // for ours instead — an even trade the attack was happy to offer.
                val board = { attackBoard(
                    listOf("Typhoid Rats", "Pensive Minotaur"), listOf("Typhoid Rats", "Youthful Knight"),
                    ourLife = 20, theirLife = 20,
                ) }
                val attackers = board().attackersChosenBy(AiProfile.LIVE)
                withClue("attacked with $attackers") { attackers.shouldNotContain("Typhoid Rats") }
                board().attackersChosenBy(withoutSoundMath).shouldContain("Typhoid Rats")
            }

            test("does not attack a Raging Redcap into Colossal Dreadmaw because a Wind Drake would trade with it") {
                val board = { attackBoard(listOf("Raging Redcap"), listOf("Wind Drake", "Colossal Dreadmaw"), 11, 13) }
                val attackers = board().attackersChosenBy(AiProfile.LIVE)
                withClue("attacked with $attackers") { attackers.shouldBeEmpty() }
                board().attackersChosenBy(withoutSoundMath).shouldContain("Raging Redcap")
            }

            test("as the defender, a Youthful Knight takes the free kill on Typhoid Rats") {
                val game = blockBoard("Typhoid Rats", listOf("Youthful Knight"), ourLife = 20)
                val rats = game.findPermanent("Typhoid Rats")!!
                val blocks = AIPlayer.create(cardRegistry, game.seatId(2), AiProfile.LIVE)
                    .chooseAction(game.state) as DeclareBlockers
                blocks.blockers.filterValues { rats in it }.keys.map { game.nameOf(it) }
                    .shouldContainExactly("Youthful Knight")
            }
        }

        context("blocking search tries a different body for the same block") {
            fun scoredPlans(profile: AiProfile): List<String> {
                val game = blockBoard("Typhoid Rats", listOf("Trained Armodon", "Giant Spider"), ourLife = 17)
                val labels = mutableListOf<String>()
                val sink = AiInsightSink { _, insight -> labels += insight.options.map { it.label } }
                AIPlayer.create(cardRegistry, game.seatId(2), profile, insightSink = sink).chooseAction(game.state)
                return labels
            }

            test("the Armodon is weighed against the Spider for the trade with the Rats") {
                scoredPlans(AiProfile.LIVE) shouldContain "Trained Armodon blocks Typhoid Rats"
            }
        }
    }
}
