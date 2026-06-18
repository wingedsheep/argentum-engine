package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.DeathcapGlade
import com.wingedsheep.mtg.sets.definitions.vow.cards.DreamrootCascade
import com.wingedsheep.mtg.sets.definitions.vow.cards.SundownPass
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the three Crimson Vow / Secrets of Strixhaven "slow lands":
 * Deathcap Glade (B/G), Dreamroot Cascade (G/U), Sundown Pass (R/W).
 *
 * Each enters tapped unless you control two or more *other* lands. The condition is
 * modeled as "controlled lands (including the entering land) >= 3".
 */
class SlowLandsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + DeathcapGlade + DreamrootCascade + SundownPass)
        return driver
    }

    test("Deathcap Glade enters tapped with one other land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(p1, "Forest")
        val glade = driver.putCardInHand(p1, "Deathcap Glade")
        driver.playLand(p1, glade).isSuccess shouldBe true

        driver.state.getEntity(glade)?.has<TappedComponent>() shouldBe true
    }

    test("Deathcap Glade enters untapped with two other lands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(p1, "Forest")
        driver.putLandOnBattlefield(p1, "Swamp")
        val glade = driver.putCardInHand(p1, "Deathcap Glade")
        driver.playLand(p1, glade).isSuccess shouldBe true

        driver.state.getEntity(glade)?.has<TappedComponent>() shouldBe false
    }

    test("Deathcap Glade enters tapped as the first land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glade = driver.putCardInHand(p1, "Deathcap Glade")
        driver.playLand(p1, glade).isSuccess shouldBe true

        driver.state.getEntity(glade)?.has<TappedComponent>() shouldBe true
    }

    test("Deathcap Glade taps for black and green") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val black = driver.putPermanentOnBattlefield(p1, "Deathcap Glade")
        driver.submit(ActivateAbility(p1, black, DeathcapGlade.activatedAbilities[0].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.black shouldBe 1

        val green = driver.putPermanentOnBattlefield(p1, "Deathcap Glade")
        driver.submit(ActivateAbility(p1, green, DeathcapGlade.activatedAbilities[1].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.green shouldBe 1
    }

    test("Dreamroot Cascade enters untapped with two other lands and taps for green and blue") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(p1, "Forest")
        driver.putLandOnBattlefield(p1, "Island")
        val cascade = driver.putCardInHand(p1, "Dreamroot Cascade")
        driver.playLand(p1, cascade).isSuccess shouldBe true
        driver.state.getEntity(cascade)?.has<TappedComponent>() shouldBe false

        val green = driver.putPermanentOnBattlefield(p1, "Dreamroot Cascade")
        driver.submit(ActivateAbility(p1, green, DreamrootCascade.activatedAbilities[0].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.green shouldBe 1

        val blue = driver.putPermanentOnBattlefield(p1, "Dreamroot Cascade")
        driver.submit(ActivateAbility(p1, blue, DreamrootCascade.activatedAbilities[1].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.blue shouldBe 1
    }

    test("Dreamroot Cascade enters tapped with only one other land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(p1, "Forest")
        val cascade = driver.putCardInHand(p1, "Dreamroot Cascade")
        driver.playLand(p1, cascade).isSuccess shouldBe true

        driver.state.getEntity(cascade)?.has<TappedComponent>() shouldBe true
    }

    test("Sundown Pass enters untapped with two other lands and taps for red and white") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(p1, "Mountain")
        driver.putLandOnBattlefield(p1, "Plains")
        val pass = driver.putCardInHand(p1, "Sundown Pass")
        driver.playLand(p1, pass).isSuccess shouldBe true
        driver.state.getEntity(pass)?.has<TappedComponent>() shouldBe false

        val red = driver.putPermanentOnBattlefield(p1, "Sundown Pass")
        driver.submit(ActivateAbility(p1, red, SundownPass.activatedAbilities[0].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.red shouldBe 1

        val white = driver.putPermanentOnBattlefield(p1, "Sundown Pass")
        driver.submit(ActivateAbility(p1, white, SundownPass.activatedAbilities[1].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.white shouldBe 1
    }

    test("Sundown Pass enters tapped with only one other land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(p1, "Mountain")
        val pass = driver.putCardInHand(p1, "Sundown Pass")
        driver.playLand(p1, pass).isSuccess shouldBe true

        driver.state.getEntity(pass)?.has<TappedComponent>() shouldBe true
    }
})
