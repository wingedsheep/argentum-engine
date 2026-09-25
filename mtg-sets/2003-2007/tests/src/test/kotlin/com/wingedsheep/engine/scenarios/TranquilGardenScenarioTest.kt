package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.TranquilGarden
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tranquil Garden (CHK #284):
 * {T}: Add {C}.
 * {T}: Add {G} or {W}. This land doesn't untap during your next untap step.
 */
class TranquilGardenScenarioTest : FunSpec({

    val colorlessAbilityId = TranquilGarden.activatedAbilities[0].id
    val firstColorAbilityId = TranquilGarden.activatedAbilities[1].id
    val secondColorAbilityId = TranquilGarden.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TranquilGarden)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun advanceToNextMainOf(driver: GameTestDriver, player: EntityId) {
        do {
            driver.passPriorityUntil(Step.END)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (driver.activePlayer != player)
    }

    test("the coloured ability adds {G} and skips the controller's next untap step only") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(player, "Tranquil Garden")

        driver.submit(ActivateAbility(playerId = player, sourceId = land, abilityId = firstColorAbilityId))
            .outcome shouldBe Outcome.Done
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.green shouldBe 1
        driver.isTapped(land) shouldBe true

        advanceToNextMainOf(driver, player)
        driver.isTapped(land) shouldBe true

        advanceToNextMainOf(driver, player)
        driver.isTapped(land) shouldBe false
    }

    test("the other colour option also freezes the land") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(player, "Tranquil Garden")

        driver.submit(ActivateAbility(playerId = player, sourceId = land, abilityId = secondColorAbilityId))
            .outcome shouldBe Outcome.Done
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.white shouldBe 1

        advanceToNextMainOf(driver, player)
        driver.isTapped(land) shouldBe true
    }

    test("the colourless ability untaps normally") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(player, "Tranquil Garden")

        driver.submit(ActivateAbility(playerId = player, sourceId = land, abilityId = colorlessAbilityId))
            .outcome shouldBe Outcome.Done
        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.colorless shouldBe 1

        advanceToNextMainOf(driver, player)
        driver.isTapped(land) shouldBe false
    }
})
