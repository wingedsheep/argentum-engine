package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.StallionOfAshmouth
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stallion of Ashmouth (SOI #136) — {3}{B} 3/3 Nightmare Horse.
 *
 *   Delirium — {1}{B}: This creature gets +1/+1 until end of turn. Activate only if there are four
 *   or more card types among cards in your graveyard.
 */
class StallionOfAshmouthScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun pump(player: EntityId, stallion: EntityId) = ActivateAbility(
        playerId = player,
        sourceId = stallion,
        abilityId = StallionOfAshmouth.activatedAbilities.single().id,
        paymentStrategy = PaymentStrategy.FromPool,
    )

    test("without delirium the ability can't be activated") {
        val (driver, player) = newGame()
        val stallion = driver.putCreatureOnBattlefield(player, "Stallion of Ashmouth")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.giveMana(player, Color.BLACK, 2)

        driver.submitExpectFailure(pump(player, stallion))
        driver.state.projectedState.getPower(stallion) shouldBe 3
    }

    test("with delirium, {1}{B} gives it +1/+1 until end of turn") {
        val (driver, player) = newGame()
        val stallion = driver.putCreatureOnBattlefield(player, "Stallion of Ashmouth")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.putCardInGraveyard(player, "Ornithopter")     // artifact
        driver.giveMana(player, Color.BLACK, 2)

        driver.submitSuccess(pump(player, stallion))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(stallion) shouldBe 4
        driver.state.projectedState.getToughness(stallion) shouldBe 4
    }
})
