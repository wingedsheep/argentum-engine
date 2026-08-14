package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.TheLegendOfYangchen
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for The Legend of Yangchen // Avatar Yangchen (TLA #27), a transforming Saga DFC.
 *
 * Front {3}{W}{W} Enchantment — Saga:
 *   I — Starting with you, each player chooses up to one permanent with mana value 3 or greater from
 *       among permanents your opponents control. Exile those permanents.
 *   II — You may have target opponent draw three cards. If you do, draw three cards.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 * Back — Avatar Yangchen (4/5 Legendary Creature, Flying): whenever you cast your second spell each
 *   turn, airbend up to one other target nonland permanent (exile it; owner may recast for {2}).
 *
 * Chapter I pins the shared-pool per-player choose: the pool is the Saga controller's opponents'
 * MV≥3 permanents for every chooser (so the caster's own MV≥3 permanents and MV<2 permanents are
 * never offered), the caster picks first, then the opponent picks from the *remainder* (no
 * double-choice), and both picks are exiled. The saga wiring (lore, chapter III transform) mirrors
 * The Legend of Kuruk / The Rise of Sozin; the back face reuses the second-spell trigger + airbend.
 */
class TheLegendOfYangchenScenarioTest : FunSpec({

    // Vanilla bears whose mana value is fixed by their (all-generic) mana cost, so the MV≥3 pool
    // filter is exact.
    fun bear(name: String, mv: Int): CardDefinition = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{$mv}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    val mvTwo = bear("MV Two Bear", 2)
    val mvThree = bear("MV Three Bear", 3)
    val mvFour = bear("MV Four Bear", 4)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheLegendOfYangchen, mvTwo, mvThree, mvFour))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.giveSagaMana(playerId: EntityId) {
        giveColorlessMana(playerId, 3)
        giveMana(playerId, Color.WHITE, 2)
    }

    fun GameTestDriver.castYangchen(caster: EntityId) {
        val saga = putCardInHand(caster, "The Legend of Yangchen")
        giveSagaMana(caster)
        castSpell(caster, saga)
    }

    test("chapter I: you and the opponent each exile one of the opponent's MV>=3 permanents") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls two eligible (MV 3, MV 4) and one ineligible (MV 2) permanent.
        val oppThree = driver.putCreatureOnBattlefield(opp, "MV Three Bear")
        val oppFour = driver.putCreatureOnBattlefield(opp, "MV Four Bear")
        val oppTwo = driver.putCreatureOnBattlefield(opp, "MV Two Bear")
        // My own MV≥3 permanent: the pool is my *opponents'* permanents, so it must not be offered.
        val myThree = driver.putCreatureOnBattlefield(me, "MV Three Bear")

        driver.castYangchen(me)

        // Chapter I resolves on entry (lore 1) and pauses for my choice first ("starting with you").
        val mine = driver.advanceToDecision() as SelectCardsDecision
        withClue("the caster chooses first (APNAP starting with you)") { mine.playerId shouldBe me }
        withClue("both MV≥3 opponent permanents are eligible") {
            mine.options shouldContain oppThree
            mine.options shouldContain oppFour
        }
        withClue("the MV<3 opponent permanent is ineligible") { mine.options shouldNotContain oppTwo }
        withClue("my own permanent is not in the pool (only opponents' permanents)") {
            mine.options shouldNotContain myThree
        }
        driver.submitCardSelection(me, listOf(oppThree))

        // Then the opponent chooses from what's left — and can't repeat my pick.
        val theirs = driver.advanceToDecision() as SelectCardsDecision
        withClue("the opponent chooses next") { theirs.playerId shouldBe opp }
        withClue("a permanent already chosen can't be double-chosen") {
            theirs.options shouldNotContain oppThree
        }
        withClue("the still-eligible MV 4 permanent remains selectable") {
            theirs.options shouldContain oppFour
        }
        driver.submitCardSelection(opp, listOf(oppFour))
        driver.drainYangchen()

        val battlefield = driver.state.getBattlefield().toSet()
        withClue("both chosen permanents were exiled to their owner") {
            driver.getExile(opp) shouldContain oppThree
            driver.getExile(opp) shouldContain oppFour
            battlefield shouldNotContain oppThree
            battlefield shouldNotContain oppFour
        }
        withClue("the ineligible MV 2 permanent is untouched") {
            battlefield shouldContain oppTwo
        }
        withClue("my own MV 3 permanent is untouched") {
            battlefield shouldContain myThree
        }
    }

    test("chapter II: accepting has the opponent draw three, then you draw three") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castYangchen(me)
        driver.drainYangchen() // chapter I (empty pool — nobody controls MV≥3 permanents)

        // Accrue lore to 2 → chapter II. Stop on the may (yes/no) decision.
        val may = driver.advanceUntilYesNo()
        may.shouldNotBeNull()
        withClue("the caster makes the 'you may' decision") { may.playerId shouldBe me }

        val myHandBefore = driver.getHandSize(me)
        val oppHandBefore = driver.getHandSize(opp)
        driver.submitYesNo(may.playerId, true)
        driver.drainYangchen()

        withClue("target opponent drew three") { driver.getHandSize(opp) shouldBe oppHandBefore + 3 }
        withClue("then you drew three") { driver.getHandSize(me) shouldBe myHandBefore + 3 }
    }

    test("chapter II: declining draws no cards for either player") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castYangchen(me)
        driver.drainYangchen()

        val may = driver.advanceUntilYesNo()
        may.shouldNotBeNull()
        val myHandBefore = driver.getHandSize(me)
        val oppHandBefore = driver.getHandSize(opp)
        driver.submitYesNo(may.playerId, false)
        driver.drainYangchen()

        withClue("declining draws nothing for either player") {
            driver.getHandSize(me) shouldBe myHandBefore
            driver.getHandSize(opp) shouldBe oppHandBefore
        }
    }

    test("chapter III transforms into Avatar Yangchen, whose second spell airbends a nonland permanent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castYangchen(me)
        driver.drainYangchen()

        // Accrue lore to 3 over my next turns → chapter III exiles and returns transformed.
        var guard = 0
        while (driver.findPermanent(me, "Avatar Yangchen") == null && guard++ < 12) {
            driver.advanceToNextMainAndResolve()
        }

        val yangchen = driver.findPermanent(me, "Avatar Yangchen")
        withClue("chapter III returns the Saga transformed into Avatar Yangchen") {
            yangchen shouldNotBe null
        }
        withClue("Avatar Yangchen is a 4/5 flying creature under your control") {
            driver.state.projectedState.isCreature(yangchen!!) shouldBe true
            driver.state.projectedState.getPower(yangchen) shouldBe 4
            driver.state.projectedState.getToughness(yangchen) shouldBe 5
            driver.state.projectedState.hasKeyword(yangchen, Keyword.FLYING) shouldBe true
        }

        // I'm in my precombat main with Avatar Yangchen in play. Cast two spells; the second triggers
        // the airbend of an opponent's nonland permanent.
        val victim = driver.putCreatureOnBattlefield(opp, "MV Three Bear")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 2)

        val spell1 = driver.putCardInHand(me, "MV Two Bear")
        driver.castSpell(me, spell1)
        driver.bothPass() // first spell resolves — no trigger

        val spell2 = driver.putCardInHand(me, "MV Two Bear")
        driver.castSpell(me, spell2)
        // Second spell fires Avatar Yangchen's airbend trigger, which asks for its target.
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        withClue("the airbend target is chosen by Yangchen's controller") {
            targetDecision.playerId shouldBe me
        }
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass() // resolve the airbend trigger (then the second spell)

        withClue("the airbent permanent is exiled to its owner") {
            driver.getExile(opp) shouldContain victim
            driver.state.getBattlefield().toSet() shouldNotContain victim
        }
        withClue("its owner may recast it from exile for a fixed {2}") {
            val grant = driver.state.getEntity(victim)?.get<PlayWithFixedAlternativeManaCostComponent>()
            grant.shouldNotBeNull()
            grant.controllerId shouldBe opp
            grant.fixedCost shouldBe ManaCost.parse("{2}")
        }
    }
})

// --- Drive helpers -----------------------------------------------------------------------------

/** Pass priority (resolving the stack) until a decision surfaces; returns it (or null). */
private fun GameTestDriver.advanceToDecision(maxPasses: Int = 60): com.wingedsheep.engine.core.PendingDecision? {
    var guard = 0
    while (pendingDecision == null && guard++ < maxPasses) bothPass()
    return pendingDecision
}

/**
 * Drain the stack, auto-answering incidental decisions: a chapter-II yes/no defaults to [may];
 * any card selection (e.g. an empty chapter-I pool) takes nothing.
 */
private fun GameTestDriver.drainYangchen(may: Boolean = true) {
    var guard = 0
    while (guard++ < 200) {
        val decision = pendingDecision
        when {
            decision == null && state.stack.isNotEmpty() -> bothPass()
            decision == null -> return
            decision is YesNoDecision -> submitYesNo(decision.playerId, may)
            decision is SelectCardsDecision -> submitCardSelection(decision.playerId, emptyList())
            else -> autoResolveDecision()
        }
    }
}

/**
 * Advance through turns (accruing lore) until chapter II's "you may" decision surfaces, auto-resolving
 * any incidental decision along the way. Passes priority even on an empty stack so lore accrues.
 */
private fun GameTestDriver.advanceUntilYesNo(maxSteps: Int = 500): YesNoDecision? {
    var guard = 0
    while (guard++ < maxSteps && !state.gameOver) {
        val decision = pendingDecision
        when {
            decision is YesNoDecision -> return decision
            decision != null -> autoResolveDecision()
            else -> bothPass()
        }
    }
    return null
}

/** Advance to my next precombat main and resolve any saga chapter that lands on the stack. */
private fun GameTestDriver.advanceToNextMainAndResolve() {
    passPriorityUntil(Step.END, maxPasses = 400)
    passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 400)
    drainYangchen(may = false)
}
