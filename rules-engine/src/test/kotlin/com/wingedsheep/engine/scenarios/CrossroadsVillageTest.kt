package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CrossroadsVillage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Crossroads Village — Land — Town
 * This land enters tapped. As it enters, choose a color.
 * {T}: Add one mana of the chosen color.
 */
class CrossroadsVillageTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CrossroadsVillage)
        return driver
    }

    test("enters tapped and prompts for a color, then taps for the chosen color") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val village = driver.putCardInHand(p1, "Crossroads Village")
        driver.playLand(p1, village)

        // "As it enters, choose a color."
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        driver.submitDecision(p1, ColorChosenResponse(decision.id, Color.GREEN))

        // It entered tapped.
        driver.state.getEntity(village)?.has<TappedComponent>() shouldBe true

        // Untap and tap it for mana of the chosen color (green).
        driver.untapPermanent(village)
        val ability = CrossroadsVillage.activatedAbilities[0].id
        driver.submit(ActivateAbility(playerId = p1, sourceId = village, abilityId = ability)).isSuccess shouldBe true

        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
        pool.green shouldBe 1
    }
})
