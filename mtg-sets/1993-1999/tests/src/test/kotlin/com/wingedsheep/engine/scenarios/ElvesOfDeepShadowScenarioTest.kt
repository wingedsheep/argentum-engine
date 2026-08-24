package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.ElvesOfDeepShadow
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ElvesOfDeepShadowScenarioTest : FunSpec({
    val manaAbilityId = ElvesOfDeepShadow.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ElvesOfDeepShadow)
        return driver
    }

    test("tapping Elves of Deep Shadow adds black mana and deals 1 damage to its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val elves = driver.putPermanentOnBattlefield(activePlayer, "Elves of Deep Shadow")
        val lifeBefore = driver.getLifeTotal(activePlayer)

        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = elves, abilityId = manaAbilityId)
        )

        result.isSuccess shouldBe true
        driver.isTapped(elves) shouldBe true
        driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()?.black shouldBe 1
        driver.getLifeTotal(activePlayer) shouldBe lifeBefore - 1
    }
})
