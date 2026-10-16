package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BardKingOfDale
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.DrawEvent.firstInDrawStep` — the [DrawStepFirstCard] axis, on triggers and on
 * draw replacements.
 *
 * Rules proven:
 *  - CR 504.1 — the turn-based draw is the first card drawn in a draw step; `ONLY` fires for it.
 *  - `ONLY` fires once per draw step: a second card drawn in that step doesn't fire it.
 *  - `ONLY` never fires outside the drawer's own draw step — not for main-phase draws, not for a
 *    draw made during an opponent's draw step.
 *  - CR 121.2 — a multi-card draw is several draws, so a draw-two in the main phase is still not
 *    "the first card during the draw step".
 *  - On a replacement, `ONLY` replaces just that first card and `EXCLUDED` (Bard, King of Dale)
 *    replaces every draw but it — including a single draw outside the draw step. (A replaced
 *    draw draws no card, so it doesn't use up "the first card drawn" that step.)
 */
class DrawStepFirstCardTest : FunSpec({

    val firstDrawWatcher = card("First Draw Watcher Test") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "Whenever you draw your first card during each of your draw steps, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.you.draws(DrawStepFirstCard.ONLY)
            effect = Effects.GainLife(1)
        }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T01" }
    }
    val firstDrawReplacer = card("First Draw Replacer Test") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "If you would draw your first card during each of your draw steps, gain 3 life instead."
        replacementEffect(
            ReplaceDrawWith(
                replacementEffect = Effects.GainLife(3),
                appliesTo = EventPattern.DrawEvent(Player.You, firstInDrawStep = DrawStepFirstCard.ONLY),
            )
        )
        metadata { rarity = Rarity.COMMON; collectorNumber = "T02" }
    }
    val drawInstant = card("Draw Instant Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T03" }
    }
    val drawTwo = card("Draw Two Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw two cards."
        spell { effect = Effects.DrawCards(2) }
        metadata { rarity = Rarity.COMMON; collectorNumber = "T04" }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(firstDrawWatcher, firstDrawReplacer, drawInstant, drawTwo, BardKingOfDale)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    fun GameTestDriver.resolveStack() {
        while (getTopOfStack() != null) bothPass()
    }

    test("ONLY fires for the turn-based draw, once per draw step") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p2, "First Draw Watcher Test")

        driver.passPriorityUntil(Step.DRAW, p2)
        driver.resolveStack()
        driver.getLifeTotal(p2) shouldBe 21

        // A second card drawn in the same draw step is not the first one.
        val instant = driver.putCardInHand(p2, "Draw Instant Test")
        driver.castSpell(p2, instant)
        driver.resolveStack()
        driver.getLifeTotal(p2) shouldBe 21
    }

    test("ONLY ignores draws outside the draw step, including a multi-card draw") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p1, "First Draw Watcher Test")

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, driver.putCardInHand(p1, "Draw Two Test"))
        driver.resolveStack()

        driver.getHandSize(p1) shouldBe handBefore + 2
        driver.getLifeTotal(p1) shouldBe 20
    }

    test("ONLY ignores a draw made during an opponent's draw step") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p1, "First Draw Watcher Test")

        driver.passPriorityUntil(Step.DRAW, p2)
        driver.passPriority(p2)
        driver.castSpell(p1, driver.putCardInHand(p1, "Draw Instant Test"))
        driver.resolveStack()

        driver.getLifeTotal(p1) shouldBe 20
    }

    test("an ONLY replacement replaces the turn-based draw but not a later draw outside the draw step") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(p2, "First Draw Replacer Test")
        val handBefore = driver.getHandSize(p2)

        driver.passPriorityUntil(Step.DRAW, p2)
        driver.getHandSize(p2) shouldBe handBefore
        driver.getLifeTotal(p2) shouldBe 23

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, p2)
        driver.castSpell(p2, driver.putCardInHand(p2, "Draw Instant Test"))
        driver.resolveStack()
        driver.getHandSize(p2) shouldBe handBefore + 1
        driver.getLifeTotal(p2) shouldBe 23
    }

    test("Bard, King of Dale (EXCLUDED replacement) leaves the turn-based draw alone and doubles a lone main-phase draw") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(p2, "Bard, King of Dale")
        val handBefore = driver.getHandSize(p2)

        driver.passPriorityUntil(Step.DRAW, p2)
        driver.getHandSize(p2) shouldBe handBefore + 1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, p2)
        driver.castSpell(p2, driver.putCardInHand(p2, "Draw Instant Test"))
        driver.resolveStack()
        driver.getHandSize(p2) shouldBe handBefore + 3
    }
})
