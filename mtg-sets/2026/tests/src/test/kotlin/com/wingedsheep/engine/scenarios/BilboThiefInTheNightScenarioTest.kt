package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BilboThiefInTheNight
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Bilbo, Thief in the Night — {1}{U} Legendary Creature — Halfling Rogue (The Hobbit #33).
 *
 *  - "Spells you cast from anywhere other than your hand cost {1} less to cast."
 *  - "Whenever Bilbo attacks, you may cast an artifact, instant, or sorcery spell from your
 *    graveyard. If an instant or sorcery spell cast this way would be put into your graveyard,
 *    exile it instead."
 *
 * The second ability is a turn-long `MayCastFromGraveyard` grant (the Forgotten Cellar path) plus
 * the new `exileCastThisWay = INSTEAD_OF_GRAVEYARD` cast-this-way rider. These tests pin the three things that could
 * silently regress: the zone scope of the cost reduction, the attack gate on the permission, and the
 * fact that the exile rider follows the *authorizing grant* rather than the graveyard zone.
 */
class BilboThiefInTheNightScenarioTest : FunSpec({

    // A plain {2} sorcery with no targets — the cost reduction and the exile rider are both easier
    // to assert without a targeting decision in the way.
    val relic = card("Bilbo Test Relic Sorcery") {
        manaCost = "{2}"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell { effect = Effects.GainLife(3) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BilboThiefInTheNight, relic))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    /**
     * Bilbo on the battlefield, attacking, trigger resolved, then on to postcombat main.
     *
     * The step advance is not incidental: the grant respects normal timing, so a *sorcery* in the
     * graveyard is only castable in a main phase. That is a visible consequence of modelling the
     * printed resolve-time offer as a turn-long grant — see the card's implementation notes.
     */
    fun attackWithBilbo(driver: GameTestDriver, you: EntityId): EntityId {
        val bilbo = driver.putCreatureOnBattlefield(you, "Bilbo, Thief in the Night")
        driver.removeSummoningSickness(bilbo)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bilbo), driver.getOpponent(you)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        return bilbo
    }

    test("a graveyard spell cast off the attack trigger costs {1} less and is exiled, not re-buried") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        attackWithBilbo(driver, you)

        // {2} sorcery reduced to {1}: one generic mana is exactly enough.
        driver.giveColorlessMana(you, 1)
        val lifeBefore = driver.getLifeTotal(you)
        driver.castSpell(you, sorcery).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.getLifeTotal(you) shouldBe lifeBefore + 3
        // "would be put into your graveyard, exile it instead"
        driver.getExile(you).contains(sorcery) shouldBe true
        driver.getGraveyard(you).contains(sorcery) shouldBe false
    }

    test("without the attack trigger there is no permission to cast from the graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        val bilbo = driver.putCreatureOnBattlefield(you, "Bilbo, Thief in the Night")
        driver.removeSummoningSickness(bilbo)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.giveColorlessMana(you, 5)
        driver.castSpell(you, sorcery).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(you).contains(sorcery) shouldBe true
    }

    test("the same spell cast from hand is neither discounted nor exiled") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sorcery = driver.putCardInHand(you, "Bilbo Test Relic Sorcery")
        attackWithBilbo(driver, you)

        // One mana is NOT enough from hand — the reduction excludes the hand.
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, sorcery).outcome shouldNotBe Outcome.Done

        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, sorcery).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        // The exile rider rides the graveyard-cast grant, so a hand cast is buried normally.
        driver.getGraveyard(you).contains(sorcery) shouldBe true
        driver.getExile(you).contains(sorcery) shouldBe false
    }

    test("the grant authorizes only one graveyard cast per turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val first = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        val second = driver.putCardInGraveyard(you, "Bilbo Test Relic Sorcery")
        attackWithBilbo(driver, you)

        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, first).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.giveColorlessMana(you, 5)
        driver.castSpell(you, second).outcome shouldNotBe Outcome.Done
        driver.getGraveyard(you).contains(second) shouldBe true
    }
})
