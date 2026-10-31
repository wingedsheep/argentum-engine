package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The card intent of a gated effect ([com.wingedsheep.sdk.scripting.effects.GatedEffect]) — "you
 * may pay {1}. If you do, …", "you may discard a card. If you do, draw two cards", "if you win the
 * clash, …".
 *
 * A gate is the condition its branches wait on, not what the card is for: the card does whatever
 * its `then` and `otherwise` do. Unread, the whole ability carried no tag at all, so Abandon
 * Attachments — an instant draw spell — was a card with no purpose, the end-step window
 * [HoldPolicy] hands a cantrip never opened, and the AI passed it through the opponent's end step
 * with the mana about to empty. The same blindness kept Pippin's Bravery out of
 * [IntentTag.COMBAT_TRICK], so the pump was free to be dumped outside combat.
 */
class GatedEffectReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    /** Two Islands, Abandon Attachments and a card to discard, at the opponent's end step. */
    private val opponentsEndStep = { s: ScenarioBuilder ->
        s.withPlayers()
            .withActivePlayer(2)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInHand(1, "Abandon Attachments")
            .withCardInHand(1, "Craw Wurm")
            .build()
            .advanceToPriority(1, Step.END)
    }

    init {
        test("a gated draw reads as a draw, and a gated expiring pump as a combat trick") {
            withClue("Abandon Attachments: the draw behind 'if you do' is what the card is for") {
                intents.forName("Abandon Attachments")!!.tags shouldContain IntentTag.DRAW
            }
            withClue("Pippin's Bravery: both branches pump until end of turn") {
                intents.forName("Pippin's Bravery")!!.tags shouldContain IntentTag.COMBAT_TRICK
            }
        }

        test("the opponent's end step is the window for a gated instant draw") {
            val game = opponentsEndStep(scenario())
            val policy = HoldPolicy(intents, cashCantripsInTheEndStep = true)
            val card = game.findCardsInHand(1, "Abandon Attachments").first()

            withClue("a gated draw is a cantrip: the end step is its window") {
                policy.verdictFor(game.state, game.player1Id, "Abandon Attachments", cast = CastSpell(game.player1Id, card))
                    .shouldBeInstanceOf<TimingVerdict.Adjust>()
            }
        }

        test("the AI cashes Abandon Attachments at the opponent's end step instead of letting the mana empty") {
            val result = runner.run(
                AiPuzzle(
                    id = "gated-draw-end-step",
                    category = PuzzleCategory.PRIORITY_TIMING,
                    expectation = "casts Abandon Attachments",
                    aiSeat = 1,
                    position = opponentsEndStep,
                    check = { playedCard shouldBe "Abandon Attachments" },
                ),
                AiProfile.LIVE,
            )
            withClue("gated draw read as DRAW, so the end-step window opens: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
