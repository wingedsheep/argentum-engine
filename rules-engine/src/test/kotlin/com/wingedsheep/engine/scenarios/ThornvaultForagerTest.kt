package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.ThornvaultForager
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ThornvaultForagerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThornvaultForager))
        return driver
    }

    test("Forage mana ability is unaffordable with empty graveyard and no Food") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, active, EnumerationMode.FULL)
        val foragerManaAbilities = actions.filter {
            val a = it.action
            a is ActivateAbility && a.sourceId == forager && it.isManaAbility
        }

        // Two mana abilities: {T}: Add {G}  and  {T}, Forage: Add two mana.
        // The Forage one must be unaffordable — empty graveyard, no Food.
        foragerManaAbilities.size shouldBe 2
        foragerManaAbilities.count { it.affordable } shouldBe 1
    }

    test("Forage mana ability is affordable when graveyard has 3+ cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forager = driver.putCreatureOnBattlefield(active, "Thornvault Forager")
        driver.replaceState(
            driver.state.updateEntity(forager) { it.without<SummoningSicknessComponent>() }
        )
        repeat(3) { driver.putCardInGraveyard(active, "Forest") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, active, EnumerationMode.FULL)
        val foragerManaAbilities = actions.filter {
            val a = it.action
            a is ActivateAbility && a.sourceId == forager && it.isManaAbility
        }

        foragerManaAbilities.size shouldBe 2
        foragerManaAbilities.all { it.affordable } shouldBe true
    }
})
