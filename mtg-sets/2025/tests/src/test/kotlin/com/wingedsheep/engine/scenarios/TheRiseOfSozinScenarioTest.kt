package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.TheRiseOfSozin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for The Rise of Sozin // Fire Lord Sozin (TLA #117), a transforming Saga DFC.
 *
 * Front {4}{B}{B} Enchantment — Saga:
 *   I — Destroy all creatures.
 *   II — Choose a card name. Search target opponent's graveyard, hand, and library for up to four
 *        cards with that name and exile them. Then that player shuffles.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 * Back — Fire Lord Sozin (5/5 Legendary Creature, Menace + firebending 3): whenever it deals combat
 *   damage to a player, you may pay {X}, then reanimate creature cards with total mana value X or
 *   less from that player's graveyard under your control.
 *
 * The saga wiring (lore accrual, chapter III exile-and-return-transformed) mirrors The Legend of
 * Kuruk / Roku; chapter II's name-then-multi-zone-search mirrors Lobotomy / Desperate Research; the
 * back face's pay-{X} reflexive reanimation mirrors Hollow Specter / Scion of Darkness with the
 * dynamic total-mana-value cap.
 */
class TheRiseOfSozinScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheRiseOfSozin))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.giveSagaMana(playerId: com.wingedsheep.sdk.model.EntityId) {
        giveColorlessMana(playerId, 4)
        giveMana(playerId, Color.BLACK, 2)
    }

    fun GameTestDriver.countControlled(playerId: com.wingedsheep.sdk.model.EntityId, name: String): Int =
        getPermanents(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("chapter I destroys all creatures") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opp, "Hill Giant")

        val saga = driver.putCardInHand(me, "The Rise of Sozin")
        driver.giveSagaMana(me)
        driver.castSpell(me, saga)
        driver.resolveAllSozin()

        withClue("chapter I is a board wipe — no creatures survive on either side") {
            driver.getCreatures(me).size shouldBe 0
            driver.getCreatures(opp).size shouldBe 0
        }
        withClue("the Saga itself (an enchantment) is still on the battlefield") {
            driver.findPermanent(me, "The Rise of Sozin") shouldNotBe null
        }
    }

    test("chapter II names a card and exiles up to four of the opponent's matching cards") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Seed five Grizzly Bears across the opponent's graveyard, hand, and library, plus a
        // non-matching Hill Giant in the graveyard that must be left untouched.
        repeat(2) { driver.putCardInGraveyard(opp, "Grizzly Bears") }
        driver.putCardInGraveyard(opp, "Hill Giant")
        driver.putCardInHand(opp, "Grizzly Bears")
        repeat(2) { driver.putCardOnTopOfLibrary(opp, "Grizzly Bears") }

        val saga = driver.putCardInHand(me, "The Rise of Sozin")
        driver.giveSagaMana(me)
        driver.castSpell(me, saga)
        driver.resolveAllSozin() // chapter I resolves (nothing to destroy here)

        // Accrue lore to 2 → chapter II. Drive turns forward (auto-resolving intermediate choices
        // like the forced target) until the chapter pauses to choose a card name.
        val nameDecision = driver.advanceUntilChooseOption()
        withClue("chapter II pauses to choose a card name") { nameDecision shouldNotBe null }
        withClue("Grizzly Bears is offered as a nameable card") {
            nameDecision!!.options.indexOf("Grizzly Bears") shouldNotBe -1
        }
        driver.submitDecision(
            nameDecision!!.playerId,
            OptionChosenResponse(nameDecision.id, nameDecision.options.indexOf("Grizzly Bears"))
        )
        driver.resolveAllSozin() // the up-to-four select + exile + shuffle

        withClue("up to four matching cards were exiled from the opponent's zones") {
            driver.getExileCardNames(opp).count { it == "Grizzly Bears" } shouldBe 4
        }
        withClue("the non-matching Hill Giant is untouched — not exiled, still in the graveyard") {
            driver.getExileCardNames(opp) shouldNotContain "Hill Giant"
            driver.getGraveyardCardNames(opp).contains("Hill Giant") shouldBe true
        }
    }

    test("chapter III transforms into Fire Lord Sozin, whose combat damage targets and reanimates graveyard creatures under your control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val sozin = driver.transformAndReadySozinForAttack(me, opp)

        withClue("chapter III returns the Saga transformed into a 5/5 Fire Lord Sozin you control") {
            driver.state.projectedState.isCreature(sozin) shouldBe true
            driver.state.projectedState.getPower(sozin) shouldBe 5
            driver.state.projectedState.getToughness(sozin) shouldBe 5
        }

        // Two Grizzly Bears (mana value 2 each) in the opponent's graveyard to reanimate.
        val bear1 = driver.putCardInGraveyard(opp, "Grizzly Bears")
        val bear2 = driver.putCardInGraveyard(opp, "Grizzly Bears")

        // Attack, deal combat damage, pay {X} = 4 → the reflexive presents its target selection.
        val targetDecision = driver.attackAndPayX(me, opp, sozin, payX = 4)
        withClue("both bears in the damaged player's graveyard are legal targets") {
            targetDecision.legalTargets[0]!! shouldContain bear1
            targetDecision.legalTargets[0]!! shouldContain bear2
        }
        // Target both — total mana value 4 ≤ X (4).
        driver.submitTargetSelection(me, listOf(bear1, bear2)).error shouldBe null
        driver.resolveAllSozin()

        withClue("targeting both bears (total mana value 4 ≤ 4) reanimates them under your control") {
            driver.countControlled(me, "Grizzly Bears") shouldBe 2
        }
    }

    test("the total-mana-value cap trims: targeting creatures whose combined mana value exceeds X is rejected") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val sozin = driver.transformAndReadySozinForAttack(me, opp)

        // Three Grizzly Bears (mana value 2 each = 6 total) in the opponent's graveyard.
        val bears = List(3) { driver.putCardInGraveyard(opp, "Grizzly Bears") }

        val targetDecision = driver.attackAndPayX(me, opp, sozin, payX = 4)
        withClue("all three bears are individually legal targets") {
            bears.forEach { targetDecision.legalTargets[0]!! shouldContain it }
        }
        // Targeting all three (total mana value 6) exceeds X = 4 and must be rejected.
        val rejected = driver.submitTargetSelection(me, bears)
        withClue("an over-cap target set (total mana value 6 > X 4) is rejected, nothing reanimated") {
            rejected.error shouldNotBe null
            driver.countControlled(me, "Grizzly Bears") shouldBe 0
        }
        // The decision is still pending — a two-bear set (total mana value 4) is within the cap.
        driver.submitTargetSelection(me, bears.take(2)).error shouldBe null
        driver.resolveAllSozin()
        withClue("only the two bears within the mana-value cap are reanimated") {
            driver.countControlled(me, "Grizzly Bears") shouldBe 2
        }
    }

    test("only the damaged player's graveyard is targetable, not your own") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val sozin = driver.transformAndReadySozinForAttack(me, opp)

        // A creature in YOUR graveyard and one in the opponent's.
        val myBear = driver.putCardInGraveyard(me, "Grizzly Bears")
        val oppBear = driver.putCardInGraveyard(opp, "Grizzly Bears")

        val targetDecision = driver.attackAndPayX(me, opp, sozin, payX = 2)
        withClue("only the damaged player's graveyard creature is a legal target") {
            targetDecision.legalTargets[0]!! shouldContain oppBear
            targetDecision.legalTargets[0]!! shouldNotContain myBear
        }
        driver.submitTargetSelection(me, listOf(oppBear)).error shouldBe null
        driver.resolveAllSozin()
        withClue("the opponent's bear is reanimated under your control") {
            driver.countControlled(me, "Grizzly Bears") shouldBe 1
        }
    }
})

// --- Drive helpers -----------------------------------------------------------------------------

/** Resolve the stack, resolving every triggered ability that lands on it (no player choices). */
private fun GameTestDriver.resolveStackSozin() {
    var guard = 0
    while (state.stack.isNotEmpty() && guard++ < 50) {
        bothPass()
    }
}

/**
 * Cast The Rise of Sozin, accrue lore through chapter III so it transforms into Fire Lord Sozin,
 * then advance to a later turn and clear summoning sickness so it can attack. Returns Fire Lord
 * Sozin's entity id.
 */
private fun GameTestDriver.transformAndReadySozinForAttack(me: EntityId, opp: EntityId): EntityId {
    passPriorityUntil(Step.PRECOMBAT_MAIN)
    val saga = putCardInHand(me, "The Rise of Sozin")
    // The Rise of Sozin front face costs {4}{B}{B}.
    giveColorlessMana(me, 4)
    giveMana(me, Color.BLACK, 2)
    castSpell(me, saga)
    resolveAllSozin()

    // Accrue lore to 3 → chapter III exiles and returns transformed.
    var guard = 0
    while (findPermanent(me, "Fire Lord Sozin") == null && guard++ < 8) {
        advanceToNextMainAndResolve()
    }
    // Advance to my next turn so Fire Lord Sozin can attack (it entered transformed last turn).
    advanceToNextMainAndResolve() // opponent's turn
    advanceToNextMainAndResolve() // my turn
    val sozin = findPermanent(me, "Fire Lord Sozin")!!
    removeSummoningSickness(sozin)
    return sozin
}

/**
 * Attack with Fire Lord Sozin, resolve the firebending attack trigger, deal combat damage, and pay
 * {X} = [payX] on the resulting reflexive's pay-{X} chooser. Returns the [ChooseTargetsDecision] the
 * reflexive then presents for "any number of target creature cards with total mana value X or less".
 */
private fun GameTestDriver.attackAndPayX(
    me: EntityId,
    opp: EntityId,
    sozin: EntityId,
    payX: Int
): ChooseTargetsDecision {
    passPriorityUntil(Step.DECLARE_ATTACKERS)
    declareAttackers(me, listOf(sozin), opp)
    resolveStackSozin() // firebending attack trigger

    passPriorityUntil(Step.COMBAT_DAMAGE)
    // Mana to pay {X} (firebending also adds {R}{R}{R} live in combat). Added here, at the
    // combat-damage step, because unspent mana now empties as each step/phase ends (CR 500.5) —
    // given earlier it would drain before the "deals combat damage" pay-{X} trigger resolves.
    giveColorlessMana(me, payX + 2)
    var g = 0
    while (pendingDecision == null && state.step != Step.POSTCOMBAT_MAIN && g++ < 40) {
        bothPass()
    }
    val consent = pendingDecision as? YesNoDecision
        ?: error("Expected the pay-{X} yes/no, got ${pendingDecision}")
    submitYesNo(me, true)
    val payDecision = pendingDecision as? ChooseNumberDecision
        ?: error("Expected pay-{X} ChooseNumberDecision, got ${pendingDecision}")
    submitDecision(me, NumberChosenResponse(payDecision.id, payX))
    return pendingDecision as? ChooseTargetsDecision
        ?: error("Expected a ChooseTargetsDecision after paying {X}, got ${pendingDecision}")
}

/**
 * Drain the stack and answer any pending decision. [nameToChoose] picks that card name on a
 * ChooseOptionDecision (else index 0); [payX] pays that much on a MayPayX ChooseNumberDecision
 * (clamped to the affordable max); SelectCardsDecisions take the maximum allowed selection.
 */
private fun GameTestDriver.resolveAllSozin(nameToChoose: String? = null, payX: Int? = null) {
    var guard = 0
    while (guard++ < 300) {
        val decision = pendingDecision
        when {
            decision == null && state.stack.isNotEmpty() -> bothPass()
            decision == null -> return
            decision is ChooseOptionDecision -> {
                val idx = nameToChoose?.let { decision.options.indexOf(it) }
                    ?.takeIf { it >= 0 } ?: 0
                submitDecision(decision.playerId, OptionChosenResponse(decision.id, idx))
            }
            decision is YesNoDecision -> submitYesNo(decision.playerId, true)
            decision is ChooseNumberDecision -> {
                val n = (payX ?: decision.maxValue).coerceIn(0, decision.maxValue)
                submitDecision(decision.playerId, NumberChosenResponse(decision.id, n))
            }
            decision is SelectCardsDecision -> {
                submitDecision(
                    decision.playerId,
                    CardsSelectedResponse(decision.id, decision.options.take(decision.maxSelections))
                )
            }
            else -> autoResolveDecision()
        }
    }
}

/** Advance to the next precombat main WITHOUT resolving whatever lands on the stack there. */
private fun GameTestDriver.advanceToNextMainNoResolve() {
    passPriorityUntil(Step.END, maxPasses = 400)
    passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 400)
}

/** Advance to the next precombat main and auto-resolve any saga chapter on the stack. */
private fun GameTestDriver.advanceToNextMainAndResolve() {
    advanceToNextMainNoResolve()
    resolveAllSozin()
}

/**
 * Drive the game forward — passing priority through turns and auto-resolving any forced non-option
 * decision (e.g. a single-legal-target choice) — until a card-name [ChooseOptionDecision] surfaces.
 * Passes priority even on an empty stack so lore accrues across turns until the chapter fires.
 */
private fun GameTestDriver.advanceUntilChooseOption(maxSteps: Int = 400): ChooseOptionDecision? {
    var guard = 0
    while (guard++ < maxSteps && !state.gameOver) {
        val decision = pendingDecision
        when {
            decision is ChooseOptionDecision -> return decision
            decision != null -> autoResolveDecision()
            else -> bothPass()
        }
    }
    return null
}
