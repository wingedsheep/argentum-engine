package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.KravensCats
import com.wingedsheep.mtg.sets.definitions.spm.cards.TheSpotsPortal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * The Spot's Portal — {2}{B} Instant (SPM #68).
 *
 * "Put target creature on the bottom of its owner's library. You lose 2 life unless you control a
 *  Villain."
 *
 * The tuck is unconditional; the 2-life loss is gated on NOT controlling a Villain, evaluated on
 * projected battlefield state at resolution.
 */
class TheSpotsPortalScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TheSpotsPortal)
        driver.registerCard(KravensCats)
        return driver
    }

    test("tucks target creature to the bottom of its owner's library; caster loses 2 life with no Villain") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val libBefore = driver.state.getLibrary(opponent).size

        val portal = driver.putCardInHand(caster, "The Spot's Portal")
        driver.giveMana(caster, Color.BLACK, 3) // {2}{B}
        driver.castSpell(caster, portal, targets = listOf(victim))
        driver.bothPass() // resolve The Spot's Portal

        // Creature left the battlefield and is now on the bottom of its owner's (opponent's) library.
        driver.findPermanent(opponent, "Savannah Lions").shouldBeNull()
        val lib = driver.state.getLibrary(opponent)
        lib.size shouldBe libBefore + 1
        driver.state.getEntity(lib.last())?.get<CardComponent>()?.name shouldBe "Savannah Lions"

        // Caster controls no Villain, so they lose 2 life.
        driver.getLifeTotal(caster) shouldBe 18
    }

    test("controlling a Villain avoids the 2-life loss") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Caster controls a Villain (Kraven's Cats is a Cat Villain).
        driver.putCreatureOnBattlefield(caster, "Kraven's Cats")
        val victim = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val portal = driver.putCardInHand(caster, "The Spot's Portal")
        driver.giveMana(caster, Color.BLACK, 3) // {2}{B}
        driver.castSpell(caster, portal, targets = listOf(victim))
        driver.bothPass() // resolve The Spot's Portal

        // Creature is still tucked...
        driver.findPermanent(opponent, "Savannah Lions").shouldBeNull()
        driver.state.getEntity(driver.state.getLibrary(opponent).last())
            ?.get<CardComponent>()?.name shouldBe "Savannah Lions"

        // ...but the caster controls a Villain, so no life is lost.
        driver.getLifeTotal(caster) shouldBe 20
    }
})
