package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.FieryGambit
import com.wingedsheep.mtg.sets.definitions.mrd.cards.KrarksThumb
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Krark's Thumb (MRD #190) — "If you would flip a coin, instead flip two coins and ignore one."
 *
 * Exercised against Fiery Gambit, its own set-mate, because that card is the hardest thing in the
 * game for this replacement to interact with: its run has **two** questions per coin once the Thumb
 * is out — "which of these two coins do you keep?" and then "flip another coin?" — and the won-flip
 * tally has to survive both. A tally that were carried in the pipeline rather than in the frames
 * would come back wrong exactly here.
 *
 * The Thumb's own contract is checked at the same time: two coins are really flipped for every one
 * the Gambit asks for, exactly one of them counts, and the flipper — not the engine — picks which.
 *
 * Runs are seeded so the coin sequence is fixed; each test states the batch it relies on rather
 * than assuming one, so a change to the RNG shows up as a clear failure instead of a flake.
 */
class KrarksThumbScenarioTest : FunSpec({

    class Board(val d: GameTestDriver, val opponent: EntityId, val bears: EntityId, val gambit: EntityId) {
        val me: EntityId get() = d.player1
        fun lands(): List<EntityId> = d.getLands(me)
        fun handSize(): Int = d.getHandSize(me)
        fun question(): YesNoDecision? = d.pendingDecision as? YesNoDecision

        /** Hand size once the Gambit itself has left the hand — the baseline the draw tier moves. */
        var handAtResolution: Int = 0
    }

    /** The Fiery Gambit board, with [thumbs] copies of Krark's Thumb already on the battlefield. */
    fun board(seed: Long, thumbs: Int = 1): Board {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + FieryGambit + KrarksThumb)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opponent = d.getOpponent(d.player1)
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        repeat(3) { d.putLandOnBattlefield(d.player1, "Mountain") }
        d.getLands(d.player1).forEach { d.tapPermanent(it) }
        repeat(thumbs) { d.putPermanentOnBattlefield(d.player1, "Krark's Thumb") }

        val gambit = d.putCardInHand(d.player1, "Fiery Gambit")
        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 2)

        d.replaceState(d.state.copy(rng = GameRng.seeded(seed)))
        return Board(d, opponent, bears, gambit)
    }

    fun Board.cast(): ExecutionResult {
        val cast = d.castSpell(me, gambit, targets = listOf(bears))
        withClue("cast failed: ${cast.error}") { cast.isSuccess shouldBe true }
        // Read the hand *after* the Gambit has left it, so the draw-nine tier is measured against
        // the board the spell actually resolves on.
        handAtResolution = handSize()
        return d.bothPass()
    }

    /**
     * The seed of the first run in [range] whose very first coin came up mixed — the only case in
     * which the Thumb actually offers a choice, and therefore the only one worth driving.
     */
    fun seedWithAChoiceOnTheFirstFlip(range: LongRange): Long = range.first { seed ->
        val b = board(seed)
        b.cast()
        b.question()?.yesText == "Keep heads"
    }

    test("the Thumb turns the Gambit's first coin into two, and asks which one to keep") {
        val seed = seedWithAChoiceOnTheFirstFlip(1L..200L)
        val b = board(seed)
        val events = b.cast()

        val question = b.question()
        withClue("a mixed pair must put the choice to the flipper, not resolve itself") {
            question?.yesText shouldBe "Keep heads"
            question?.noText shouldBe "Keep tails"
            question?.playerId shouldBe b.me
        }
        withClue("both coins were rolled before the question — the flipper chooses knowing both") {
            question?.prompt shouldBe
                "You flipped 1 heads and 1 tails. Ignore all but one — which result do you keep?"
        }
        withClue("the flips are not reported until the batch is settled") {
            events.events.filterIsInstance<CoinFlipEvent>().size shouldBe 0
        }
    }

    test("keeping the heads coin wins the flip, so the run continues") {
        val seed = seedWithAChoiceOnTheFirstFlip(1L..200L)
        val b = board(seed)
        b.cast()

        b.d.submitYesNo(b.me, true).error shouldBe null

        withClue("a won flip is followed by the Gambit's own 'flip another coin?'") {
            b.question()?.yesText shouldBe "Flip again"
        }
    }

    test("keeping the tails coin loses the flip, which ends the run with nothing") {
        val seed = seedWithAChoiceOnTheFirstFlip(1L..200L)
        val b = board(seed)
        b.cast()
        val handBefore = b.handAtResolution

        val answer = b.d.submitYesNo(b.me, false)
        answer.error shouldBe null

        withClue("a lost flip ends the run — there is no 'flip again?' to answer") {
            b.question() shouldBe null
        }
        withClue("no tier fired: the Bears live, nobody lost life, nothing was drawn or untapped") {
            b.d.findPermanent(b.opponent, "Grizzly Bears") shouldBe b.bears
            b.d.getLifeTotal(b.opponent) shouldBe 20
            b.handSize() shouldBe handBefore
            b.lands().count { b.d.isTapped(it) } shouldBe 3
        }
        withClue("both coins are reported once the batch settles, and exactly one of them counted") {
            val flips = answer.events.filterIsInstance<CoinFlipEvent>()
            flips.size shouldBe 2
            flips.count { !it.ignored } shouldBe 1
            flips.single { !it.ignored }.won shouldBe false
        }
    }

    test("the won-flip tally survives both of the run's pauses and pays out all three tiers") {
        val seed = seedWithAChoiceOnTheFirstFlip(1L..200L)
        val b = board(seed)
        b.cast()
        val handBefore = b.handAtResolution

        // Win three flips, answering whichever question is in front of us: the Thumb's
        // "keep heads" whenever the pair came up mixed, and the Gambit's "flip again" after each
        // win. Both answers are `true`, so this is one loop rather than two interleaved scripts —
        // and it is precisely the interleaving that the tally has to survive.
        var wins = 0
        var guard = 0
        while (wins < 3 && guard++ < 40) {
            val question = b.question() ?: break
            if (question.yesText == "Flip again") wins++
            b.d.submitYesNo(b.me, true).error shouldBe null
        }
        withClue("keeping heads every time must be able to win three flips") { wins shouldBe 3 }

        // Stop of our own accord, so the tally is published by the "stop" path rather than a loss.
        b.d.submitYesNo(b.me, false).error shouldBe null

        withClue("three wins pays all three tiers — the tally came through both pause kinds intact") {
            b.d.findPermanent(b.opponent, "Grizzly Bears") shouldBe null
            b.d.getLifeTotal(b.opponent) shouldBe 14
            b.handSize() shouldBe handBefore + 9
            b.lands().count { b.d.isTapped(it) } shouldBe 0
        }
    }

    test("a second Thumb flips four coins per flip and still leaves exactly one counting") {
        // Four coins are unanimous far less often than two, so a mixed batch — and therefore a
        // question — is the overwhelmingly common case; find one and read the batch off its prompt.
        val seed = (1L..200L).first { s ->
            val probe = board(s, thumbs = 2)
            probe.cast()
            probe.question()?.yesText == "Keep heads"
        }
        val b = board(seed, thumbs = 2)
        b.cast()

        val prompt = b.question()?.prompt
        withClue("two Thumbs are four coins per flip, not three: the replacements multiply") {
            val counts = Regex("""You flipped (\d+) heads and (\d+) tails""").find(prompt ?: "")
            val heads = counts?.groupValues?.get(1)?.toInt() ?: 0
            val tails = counts?.groupValues?.get(2)?.toInt() ?: 0
            (heads + tails) shouldBe 4
        }

        val answer = b.d.submitYesNo(b.me, true)
        answer.error shouldBe null
        withClue("all four coins are reported, and three of them are ignored") {
            val flips = answer.events.filterIsInstance<CoinFlipEvent>()
            flips.size shouldBe 4
            flips.count { !it.ignored } shouldBe 1
            flips.single { !it.ignored }.won shouldBe true
        }
    }
})
