package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.SpireOfIndustry
import com.wingedsheep.mtg.sets.definitions.mh1.cards.TalismanOfCuriosity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spire of Industry (AER #184)
 * Land
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add one mana of any color. Activate only if you control an artifact.
 *
 * Pins the activation restriction on a mana ability: without an artifact the coloured ability is
 * refused (no tap, no life paid); with one it adds the chosen colour and costs 1 life.
 */
class SpireOfIndustryScenarioTest : FunSpec({

    val colorlessAbilityId = SpireOfIndustry.activatedAbilities[0].id
    val anyColorAbilityId = SpireOfIndustry.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SpireOfIndustry)
        driver.registerCard(TalismanOfCuriosity)
        return driver
    }

    test("the {C} ability works without an artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val spire = driver.putPermanentOnBattlefield(you, "Spire of Industry")

        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = spire, abilityId = colorlessAbilityId))

        driver.state.getEntity(you)?.get<ManaPoolComponent>()?.colorless shouldBe 1
        driver.getLifeTotal(you) shouldBe 20
    }

    test("the any-color ability cannot be activated without an artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val spire = driver.putPermanentOnBattlefield(you, "Spire of Industry")

        driver.submitExpectFailure(
            ActivateAbility(playerId = you, sourceId = spire, abilityId = anyColorAbilityId, manaColorChoice = Color.RED)
        )

        driver.isTapped(spire) shouldBe false
        driver.getLifeTotal(you) shouldBe 20
        driver.state.getEntity(you)?.get<ManaPoolComponent>()?.total shouldBe 0
    }

    test("with an artifact, the any-color ability adds the chosen colour and costs 1 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val spire = driver.putPermanentOnBattlefield(you, "Spire of Industry")
        driver.putPermanentOnBattlefield(you, "Talisman of Curiosity")

        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = spire, abilityId = anyColorAbilityId, manaColorChoice = Color.RED)
        )

        driver.isTapped(spire) shouldBe true
        driver.getLifeTotal(you) shouldBe 19
        val pool = driver.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.red shouldBe 1
        pool.total shouldBe 1
    }
})
