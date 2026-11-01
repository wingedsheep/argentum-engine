package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.drawWithIdleMana], one position per misplay from the 2026-10-11 `live`-profile
 * AI-vs-AI logs, each with the answer before the flag pinned beside it so the misplay stays visible.
 *
 *  - Game 3, turns 1–13: Sleight of Hand ({U} sorcery) held for seven turns, never cast, on turn
 *    11 with five untapped lands doing nothing.
 *  - Game 2, turn 11: Omashu City's "{4}, {T}, Sacrifice this land: Draw a card" activated in the
 *    precombat main with two instant tricks in hand, tapping five of six lands.
 *
 * The decision-level tests run the whole agent through [PuzzleRunner] with [AiProfile.LIVE] against
 * the same profile with the flag off; the verdict-level tests pin why.
 */
class DrawWithIdleManaTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val off by lazy { HoldPolicy(intents) }
    private val on by lazy { HoldPolicy(intents, drawWithIdleMana = true) }

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }
    // `precombatPumpsForThePlan` off too: the g2 board (Bears facing a Hill Giant, Giant Growth in
    // hand) is one where a precombat pump changes the attack plan, so with it on the without-flag
    // agent casts the trick instead of cracking Omashu City and the control stops isolating this
    // flag. Likewise the static leaf as a prior playout (`RolloutSettings.staticPriorPlayouts`):
    // it holds Omashu City here on its own, so the control reads its playouts raw.
    private val liveWithoutFlag = AiProfile.LIVE.copy(
        id = "live-without-idledraw",
        drawWithIdleMana = false,
        precombatPumpsForThePlan = false,
        rollouts = AiProfile.LIVE.rollouts?.copy(staticPriorPlayouts = 0.0),
    )

    private fun decide(
        profile: AiProfile,
        position: (ScenarioBuilder) -> TestGame,
        check: PuzzleMove.() -> Unit,
    ): PuzzleResult = runner.run(
        AiPuzzle(
            id = "idle-draw",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "",
            aiSeat = 1,
            position = position,
            check = check,
        ),
        profile,
    )

    private fun PuzzleResult.shouldSucceed() = withClue("${puzzle.id}: chose $move — $failure") { passed shouldBe true }

    private fun TestGame.castVerdict(policy: HoldPolicy, cardName: String): TimingVerdict {
        val card = findCardsInHand(1, cardName).first()
        return policy.verdictFor(state, player1Id, cardName, cast = CastSpell(player1Id, card))
    }

    private fun TestGame.omashuVerdict(policy: HoldPolicy): TimingVerdict {
        val draw = cardRegistry.getCard("Omashu City")!!.script.activatedAbilities.single { !it.isManaAbility }
        val activation = ActivateAbility(player1Id, findPermanent("Omashu City")!!, draw.id)
        return policy.verdictFor(state, player1Id, "Omashu City", activation = activation)
    }

    // ── Game 3: Sleight of Hand ──

    /** Five lands and a Sleight of Hand, in our postcombat main unless [step] says otherwise. */
    private fun sleight(step: Step = Step.POSTCOMBAT_MAIN, extraInHand: String? = null) = { s: ScenarioBuilder ->
        s.withPlayers()
            .withLandsOnBattlefield(1, "Island", 3)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInHand(1, "Sleight of Hand")
            .let { if (extraInHand != null) it.withCardInHand(1, extraInHand) else it }
            .build()
            .let { if (step == Step.PRECOMBAT_MAIN) it else it.advanceToPriority(1, step) }
    }

    // ── Game 2: Omashu City ──

    /** Omashu City and five Forests, a creature to protect, and [hand] — our precombat main. */
    private fun omashu(vararg hand: String, opponentsEndStep: Boolean = false) = { s: ScenarioBuilder ->
        s.withPlayers()
            .let { if (opponentsEndStep) it.withActivePlayer(2) else it }
            .withLandsOnBattlefield(1, "Forest", 5)
            .withLandsOnBattlefield(1, "Omashu City", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .let { hand.fold(it) { b, card -> b.withCardInHand(1, card) } }
            .build()
            .let { if (opponentsEndStep) it.advanceToPriority(1, Step.END) else it }
    }

    init {
        test("g3: a sorcery cantrip in our last main phase with idle mana gets the end-step bonus") {
            val game = sleight()(scenario())

            game.castVerdict(off, "Sleight of Hand") shouldBe TimingVerdict.Neutral
            val verdict = game.castVerdict(on, "Sleight of Hand").shouldBeInstanceOf<TimingVerdict.Adjust>()
            verdict.onlyWhenIdle shouldBe true
        }

        test("g3: the precombat main is not the last window, so no bonus there") {
            val game = sleight(step = Step.PRECOMBAT_MAIN)(scenario())

            game.castVerdict(on, "Sleight of Hand") shouldBe TimingVerdict.Neutral
        }

        test("g3: mana held up for an instant in hand is not idle") {
            val game = sleight(extraInHand = "Giant Growth")(scenario())

            game.castVerdict(on, "Sleight of Hand") shouldBe TimingVerdict.Neutral
        }

        test("g3: without the flag the agent passes with Sleight of Hand and five lands; with it, casts") {
            decide(liveWithoutFlag, sleight()) { shouldPass() }.shouldSucceed()
            decide(AiProfile.LIVE, sleight()) { shouldCast("Sleight of Hand") }.shouldSucceed()
        }

        test("g3: control — with one mana for one of them, the creature is cast, not the cantrip") {
            // Grizzly Bears outscores the cantrip-plus-bonus here on its own; `Strategist` also drops
            // the bonus outright once any other candidate beats passing, which covers the marginal
            // play this position is too clear-cut to reach.
            val position = { s: ScenarioBuilder ->
                s.withPlayers()
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Sleight of Hand")
                    .withCardInHand(1, "Grizzly Bears")
                    .build()
                    .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            }
            decide(AiProfile.LIVE, position) { shouldCast("Grizzly Bears") }.shouldSucceed()
        }

        test("g2: a draw activation is held in our main phase while an instant wants the mana") {
            val game = omashu("Giant Growth", "Pillar Launch")(scenario())

            game.omashuVerdict(off) shouldBe TimingVerdict.Neutral
            game.omashuVerdict(on) shouldBe TimingVerdict.NoWindow
        }

        test("g2: a copy-and-cast activation reads as card advantage but is not held for the end step") {
            // Isochron Scepter's copy is a spell with its own window (an imprinted Fog in their
            // combat), so the draw-activation hold must not bind it to the opponent's end step.
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(1, "Isochron Scepter")
                .withCardInHand(1, "Giant Growth")
                .build()
            val copy = cardRegistry.getCard("Isochron Scepter")!!.script.activatedAbilities
                .single { !it.isManaAbility }
            val activation = ActivateAbility(game.player1Id, game.findPermanent("Isochron Scepter")!!, copy.id)

            CardIntentAnalyzer.analyzeEffect(copy.effect).tags shouldBe setOf(IntentTag.DRAW)
            on.verdictFor(game.state, game.player1Id, "Isochron Scepter", activation = activation) shouldBe
                TimingVerdict.Neutral
        }

        test("g2: with no instant in hand the hold does not apply") {
            val game = omashu("Hill Giant")(scenario())

            game.omashuVerdict(on) shouldBe TimingVerdict.Neutral
        }

        test("g2: the opponent's end step is the window for a draw activation") {
            val game = omashu("Giant Growth", opponentsEndStep = true)(scenario())

            game.omashuVerdict(on).shouldBeInstanceOf<TimingVerdict.Adjust>()
        }

        test("g2: without the flag the agent cracks Omashu City precombat with tricks in hand; with it, holds") {
            val position = omashu("Giant Growth", "Pillar Launch")
            decide(liveWithoutFlag, position) { shouldActivate("Omashu City") }.shouldSucceed()
            decide(AiProfile.LIVE, position) { shouldNotActivate("Omashu City") }.shouldSucceed()
        }

        test("g2: and cracks it at the opponent's end step") {
            decide(AiProfile.LIVE, omashu("Giant Growth", opponentsEndStep = true)) {
                shouldActivate("Omashu City")
            }.shouldSucceed()
        }
    }
}
