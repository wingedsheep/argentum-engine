package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.HourOfPromise
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hour of Promise — {4}{G} sorcery: "Search your library for up to two land cards, put them onto the
 * battlefield tapped, then shuffle. Then if you control three or more Deserts, create two 2/2 black
 * Zombie creature tokens."
 *
 * Pins the timing of the Desert count: it is checked after the search resumes, so a Desert found by
 * the spell itself counts toward the three (2017-07-14 ruling).
 */
class HourOfPromiseScenarioTest : FunSpec({

    val desert = card("Test Desert") {
        typeLine = "Land — Desert"
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HourOfPromise, desert))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.zombies(player: EntityId) =
        getCreatures(player).filter {
            state.getEntity(it)?.get<CardComponent>()?.typeLine?.hasSubtype(Subtype("Zombie")) == true
        }

    test("a Desert found by the search counts toward three Deserts") {
        val d = setup()
        val me = d.activePlayer!!
        d.putLandOnBattlefield(me, "Test Desert")
        d.putLandOnBattlefield(me, "Test Desert")
        val foundDesert = d.putCardOnTopOfLibrary(me, "Test Desert")
        val foundForest = d.putCardOnTopOfLibrary(me, "Forest")
        val hour = d.putCardInHand(me, "Hour of Promise")
        d.giveMana(me, Color.GREEN, 5)

        d.castSpell(me, hour).outcome shouldBe Outcome.Done
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(foundDesert, foundForest))

        d.state.getBattlefield().contains(foundDesert) shouldBe true
        d.state.getBattlefield().contains(foundForest) shouldBe true
        d.isTapped(foundDesert) shouldBe true
        d.isTapped(foundForest) shouldBe true
        d.zombies(me).size shouldBe 2
    }

    test("fewer than three Deserts after the search creates no Zombies") {
        val d = setup()
        val me = d.activePlayer!!
        d.putLandOnBattlefield(me, "Test Desert")
        d.putLandOnBattlefield(me, "Test Desert")
        val forestA = d.putCardOnTopOfLibrary(me, "Forest")
        val forestB = d.putCardOnTopOfLibrary(me, "Forest")
        val hour = d.putCardInHand(me, "Hour of Promise")
        d.giveMana(me, Color.GREEN, 5)

        d.castSpell(me, hour).outcome shouldBe Outcome.Done
        d.bothPass()

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(forestA, forestB))

        d.isTapped(forestA) shouldBe true
        d.isTapped(forestB) shouldBe true
        d.zombies(me).size shouldBe 0
    }
})
