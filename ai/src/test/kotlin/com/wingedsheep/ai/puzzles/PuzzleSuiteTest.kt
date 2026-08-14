package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * The always-on tactical suite. Runs in seconds; `just arena-puzzles`.
 *
 * The gate is **"the failing set equals [KNOWN_FAILURES]"**, not "everything passes". Today's AI
 * solves some of these and not others, and a suite pinned to 48/48 would be red forever and
 * therefore ignored. Equality flags a regression *and* an unexpected fix — if a change makes the
 * AI solve `noncreature-04`, this test fails until the id is deleted from the set, which is
 * exactly the moment you want to notice.
 *
 * The reference profile stays [AiProfile.PRODUCTION] even though
 * [AiProfile.PRODUCTION_CANDIDATE_LANDDROP] is what players face since 2026-08-08. `KNOWN_FAILURES`
 * is only meaningful if it describes a *fixed* agent — repointing it at whatever is live would
 * make every promotion rewrite the set it is supposed to be checked against, and the rollout
 * evaluator would also put ~66 playout-driven positions into an always-on suite that runs in
 * seconds today. `PuzzleComparisonBenchmark` is where the live profile is scored, alongside every
 * other one.
 */
class PuzzleSuiteTest : ScenarioTestBase() {

    init {
        val runner = PuzzleRunner(cardRegistry) { scenario() }

        test("every category carries at least six puzzles, with unique ids") {
            PuzzleCatalog.all.map { it.id }.toSet().size shouldBe PuzzleCatalog.all.size
            PuzzleCategory.entries.forEach { category ->
                withClue(category) {
                    PuzzleCatalog.byCategory(category).size shouldBeGreaterThanOrEqual 6
                }
            }
            PuzzleCatalog.all.size shouldBe 98
        }

        test("every KNOWN_FAILURES id names a real puzzle") {
            (KNOWN_FAILURES - PuzzleCatalog.all.map { it.id }.toSet()) shouldBe emptySet()
        }

        test("the AI solves every puzzle outside KNOWN_FAILURES") {
            val results = runner.runAll(PuzzleCatalog.all, AiProfile.PRODUCTION)
            println(PuzzleReport.summary(AiProfile.PRODUCTION.id, results))
            results.filterNot { it.passed }.map { it.puzzle.id }.toSet() shouldBe KNOWN_FAILURES
        }

        // The arena proved it discriminates by beating a zero-weight agent 200-0. Same argument,
        // same control: if an evaluator that scores every board identically solves as many
        // positions as the real one, these are not puzzles, they are coin flips.
        test("a zero-weight agent solves strictly fewer puzzles") {
            val blind = AiProfile.LEGACY_V0.copy(
                id = "v0-blind",
                evalWeightsId = "blind",
            )
            val blindResults = runner.runAll(PuzzleCatalog.all, blind)
            val blindPassed = blindResults.count { it.passed }
            println(PuzzleReport.summary(blind.id, blindResults))
            blindPassed shouldBeLessThan (PuzzleCatalog.all.size - KNOWN_FAILURES.size)
        }
    }

    companion object {
        /**
         * Puzzles today's AI does not solve. **Shrinking this set is the deliverable of Phases 3–9**
         * of `backlog/engine-ai-improvement.md`; growing it needs a reason in the commit message.
         *
         * Baselined 2026-07-27 against `AiProfile.PRODUCTION` at 39/48; **44/48 since Phase 6**
         * (`CardIntent`), which closed noncreature-01/03/04 and instants-01/06; **60/66 since Phase
         * 2b** added the respond / activate / keywords categories; **71/83** after Phase 2c's
         * timing / lastchance categories and the combat trick window pair; **78/92** today, after
         * the five ambush-window positions. Per-category rates are in
         * `docs/ai/baseline-metrics.md`.
         */
        val KNOWN_FAILURES: Set<String> = setOf(
            // A one-ply evaluator cannot see a prevention effect: the state right after Fog
            // resolves has the same life totals as passing, so Fog is only ever "-1 card".
            // Needs the rollout evaluator (Phase 7) to play out the damage step.
            "instants-05",
            // `CardAdvantage.cardValue(0) = -3.0` makes emptying your hand read as a disaster, so
            // the AI holds its last land rather than playing it. sequencing-04 is the same decision
            // with one card of slack and passes.
            //
            // **Closed by `AiProfile.landDropIsNotCardLoss`**, which stops charging the land drop as
            // card loss at all, and still fails here only because [AiProfile.PRODUCTION] is the
            // frozen baseline this set describes. Measured on the 66-puzzle suite: it is the *only*
            // verdict that moves, for `production`, `production-horizon-concave-2` and the live
            // `production-candidate-tuned` alike (+1 each, nothing broken).
            "sequencing-02",
            // No model of "keep a blocker home": every attacker is scored on the damage it deals.
            //
            // **Also closed by `AiProfile.discountedRaceClock`** — not by a model of blocking, but
            // because scoring the race in urgency rather than turns makes the opponent's clock
            // linear in the power we leave unblocked, where the turns form flattened it.
            "race-03",
            // The same `cardValue(0)` cliff as sequencing-02, measured exactly: with one card in
            // hand, casting the Disenchant costs 4.0 of card advantage, and destroying an anthem
            // behind an *empty* board gains 2.4 of board value (weight 1.5 → +3.6). It misses by
            // 0.40. Phase 6 fixed the blindness — the AI now sees the anthem, ranks it correctly
            // and casts at noncreature-01/03/04 — but it cannot outvote a hand-drawn constant that
            // Phase 9 exists to refit. Raising the anthem prior until this passes would be tuning
            // one guess to cancel another.
            "noncreature-02",

            // ── Phase 2b ──
            // A regeneration shield is bought *before* the destruction it answers, so at the moment
            // of the activation the board is unchanged and two mana are gone — the same shape as
            // instants-05's Fog, and the same fix. Phase 7.
            "respond-05",
            // Pumping an unblocked attacker pays now for damage that lands at the combat-damage
            // step. `evaluate1Ply` simulates to the next quiet state, which is still inside
            // declare-blockers, so the +1/+0 shows up as `attackPotential` on a creature that is
            // already attacking and never as life off the opponent. Phase 7.
            "activate-05",

            // ── Land order ──
            // The only signal the evaluator has about which land to drop is `BoardPresence`: an
            // untapped land is worth 0.6, a tapped one 0.3. The basic therefore beats the tapland by
            // a flat +0.3 in every position — including this one, where the third mana is dead
            // (Hill Giant is {3}{R}), so the tapland's drawback is free *this* turn and the basic's
            // untapped-ness is what gets paid for *next* turn. Nothing asks whether the mana being
            // unlocked is live, and `Tempo`, the one feature that counts lands, ignores tapped state
            // entirely.
            //
            // Its pair, sequencing-08, is the same board with a {2}{R} on top instead, where the
            // constant happens to be right and the AI passes. Neither the constant nor its opposite
            // solves both, which is what makes the pair worth keeping: the fix is a term that reads
            // the hand's curve (or a horizon that reaches next turn's main phase), and it shows up
            // here as 07 flipping to a pass with 08 still passing.
            "sequencing-07",

            // ── Phase 2c: timing ──
            // b904bc8 added these two categories and deferred this list, so the four below have
            // been failing undeclared since. They are recorded here with what each actually does,
            // rather than as bare ids: an unexplained entry is indistinguishable from a regression
            // nobody noticed.
            //
            // Holding removal with five lands and three cards across the table. The evaluator sees
            // one thing — an opposing 2/2 is gone — and there is no term for the option the Murder
            // *was*. This is the exact case `HoldPolicy` declines on purpose: its KDoc records that
            // the symmetric "our main phase is the wrong window" penalty was built, measured and
            // removed, because holding removal is a preference between two futures rather than a
            // provable loss, and no constant prices "a better target may show up". Phase 7.
            "timing-01",
            // The same shape one step further out: Hill Giant is {3}{R} into exactly four lands, so
            // casting it blanks the Counterspell for a whole turn. What is spent is *held mana as
            // options*, and nothing in the evaluator carries that either — `Tempo` counts lands,
            // not what leaving them up would buy. Same fix as timing-01.
            "timing-03",
            // Phase 6 traded this one away and nothing replaced it. `Strategist`'s blanket
            // `passScore - 1.5` at the opponent's end step is what used to make the AI cash a
            // cantrip there, and it is switched off for every agent with card knowledge — correctly,
            // since it also paid for dumping a pump that expires in cleanup. But `HoldPolicy` only
            // ever hands back an end-step window to REMOVAL, so a DRAW-tagged instant now gets
            // nothing at all, and a cantrip's own board value is ~0 (a card drawn against a card
            // spent). Its pair is instants-06, which asserts the opposite about the same window.
            // The fix is a cantrip window in `HoldPolicy`, not a constant.
            "timing-05",

            // ── Phase 2c: last chance ──
            // Not a "does it respond" failure — the AI casts the Unsummon. It aims it at the
            // opponent's 2/2 instead of at its own Serra Angel, which is dying to the Murder on the
            // stack. So the miss is in target *polarity*, and the puzzle is built to catch exactly
            // that (the second legal target is there on purpose).
            //
            // **Diagnosed**, and not in target selection: `Strategist.chooseCommittedTargets` does
            // simulate both targets here and picks the worse board on the merits. `ThreatAssessment`
            // hands a side with no creatures the sentinel `99.0` turns and then *subtracts* it, so
            // saving the Angel — which leaves their 2/2 alive against our empty board — scores −160
            // against bouncing the 2/2's 0. Getting a 4/4 flier back is worth +3.8 of everything
            // else in the evaluator combined, and loses by forty times that.
            //
            // **Closed by `AiProfile.discountedRaceClock`**, which scores the race in urgency
            // (`power / life`) rather than in turns, so a distant clock is discounted and an absent
            // one is zero with no sentinel at all. Still fails here only because [AiProfile.
            // PRODUCTION] is the frozen baseline this set describes.
            "lastchance-05",

            // ── The combat trick window ──
            // `HoldPolicy.COMBAT_STEPS` pays a trick its combat bonus in every step of combat,
            // `BEGIN_COMBAT` and `DECLARE_ATTACKERS` included — both of which are *before* blocks.
            // So the AI fires the pump in the window where it telegraphs, and the defender who
            // would have taken 2 from a 2/2 chump-blocks the 5/5 instead.
            //
            // Its pair, instants-07, is the same board one priority window later, where casting is
            // right — and `production` passes it, because a greedy agent with no budget tiers still
            // refines the trick's target by simulation. What makes the pair worth keeping is that
            // the live agent failed 07 for a *different* reason: the pre-damage window is graded
            // ROUTINE, which is below the threshold where targets are picked by simulation at all.
            // One fix does not close both — see `AiProfile.PRODUCTION_CANDIDATE_TRICKWINDOW`.
            "instants-08",

            // ── Is the target worth the card? ──
            // Murder on a 1/1, on turn one, with three cards of slack in hand and a 0/8 holding the
            // ground. The evaluator sees an opposing creature gone and has no term at all for the
            // option the Murder *was*, so it fires at the first legal target it is offered.
            //
            // **Closed by `AiProfile.holdRemovalForBetterTargets`**, but only in combination with
            // `discountedRaceClock` — see `AiProfile.PRODUCTION_PATIENCE`. On this frozen baseline
            // the race is scored in turns, so killing the opponent's only creature swaps a `99.0`
            // sentinel into the subtraction and scores about +160; no discount that leaves the AI
            // able to cast removal at all competes with that. Its pair, `removal-08`, is the same
            // board with a full hand and passes here and everywhere.
            "removal-07",

            // ── The ambush window ──
            // Restoration Angel jammed in our own precombat main, off a real game. The leaf scores
            // a 3/4 flier the same wherever in the turn it lands, so casting beats passing by the
            // creature's whole board value (+4.06) and every reason flash is printed — hold the
            // mana, see their attack, ambush a creature — is worth nothing to a one-ply evaluator.
            // It cannot attack this turn either way.
            //
            // **Closed by `AiProfile.holdFlashPermanentsForAmbush`**, and unlike the entries above
            // it needs nothing else stacked with it: `AiProfile.PRODUCTION_AMBUSH` closes it alone.
            // Still listed here because [AiProfile.PRODUCTION] is the frozen baseline this set
            // describes.
            //
            // Its four controls all pass on this baseline and must keep passing: `instants-10` (the
            // ambush itself, at their declare-attackers), `-11` (flash *and* haste), `-12` (an ETB
            // that taps a blocker before we attack) and `-13` (the same board as this one, past the
            // patience horizon).
            "instants-09",

            // ── The expiring-grant window ──
            // Olivia's Dragoon on the opponent's precombat main, off a real game: a card discarded
            // to give a 2/2 flying, on a turn with no combat it can use and against a board with no
            // flier to block. `BoardPresence` prices flying at `1.5 + power × 0.3` with no reading
            // of whether it is evasive *now*, so the leaf scored the activation +2.35 over passing.
            //
            // Structurally it is `instants-06` — an expiring pump bought in a window that cannot
            // spend it — with the text printed on a creature instead of an instant, and that is why
            // the AI gets one right and the other wrong: `HoldPolicy` resolves an activation to its
            // source permanent, `CardIntentAnalyzer` types that permanent `Speed.ACTIVATED`, and
            // `windowVerdictFor` declines everything that is not `Speed.INSTANT` at its first line.
            // No branch there has ever seen an activated ability.
            //
            // **Closed by `AiProfile.holdExpiringGrantsForCombat`**, unaided —
            // `AiProfile.PRODUCTION_EXPIRING` closes it alone. Still listed here because
            // [AiProfile.PRODUCTION] is the frozen baseline this set describes.
            //
            // Its three controls pass on this baseline and must keep passing: `instants-15` (the
            // same ability at their declare-attackers, where the window is released and the block
            // it buys is real), `instants-16` (the same board at a full hand, where the discard is
            // the card cleanup was taking anyway) and `instants-17` (our own begin combat, the
            // release that keeps the floor from talking the AI out of the attack).
            "instants-14",
        )
    }
}
