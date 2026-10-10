package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cns.cards.SelvalaExplorerReturned
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Selvala, Explorer Returned (CNS) — Parley: each player reveals their top card; for each nonland
 * card revealed, add {G} and gain 1 life; then each player draws a card.
 *
 * Pins that the count spans *every* player's library (not just the controller's), that lands are
 * excluded, and that the ability uses the stack (not a mana ability since the 605.1a update).
 */
class SelvalaExplorerReturnedScenarioTest : FunSpec({

    val abilityId = SelvalaExplorerReturned.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SelvalaExplorerReturned)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.greenInPool(player: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(player)?.get<ManaPoolComponent>()?.getAmount(Color.GREEN) ?: 0

    test("one nonland and one land revealed: one {G}, one life, each player draws their revealed card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val selvala = driver.putCreatureOnBattlefield(me, "Selvala, Explorer Returned")
        driver.removeSummoningSickness(selvala)

        val myTop = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val theirTop = driver.putCardOnTopOfLibrary(opponent, "Forest")
        val myHand = driver.getHandSize(me)
        val theirHand = driver.getHandSize(opponent)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = selvala, abilityId = abilityId))
        driver.state.stack.size shouldBe 1 // uses the stack
        driver.greenInPool(me) shouldBe 0
        driver.bothPass()

        driver.greenInPool(me) shouldBe 1
        driver.getLifeTotal(me) shouldBe 21
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getHandSize(me) shouldBe myHand + 1
        driver.getHandSize(opponent) shouldBe theirHand + 1
        driver.getHand(me) shouldContain myTop
        driver.getHand(opponent) shouldContain theirTop
    }

    test("nonland cards revealed from both libraries each count") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val selvala = driver.putCreatureOnBattlefield(me, "Selvala, Explorer Returned")
        driver.removeSummoningSickness(selvala)

        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(opponent, "Lightning Bolt")

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = selvala, abilityId = abilityId))
        driver.bothPass()

        driver.greenInPool(me) shouldBe 2
        driver.getLifeTotal(me) shouldBe 22
    }

    test("only lands revealed: no mana, no life, but each player still draws") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val selvala = driver.putCreatureOnBattlefield(me, "Selvala, Explorer Returned")
        driver.removeSummoningSickness(selvala)
        val theirHand = driver.getHandSize(opponent)

        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = selvala, abilityId = abilityId))
        driver.bothPass()

        driver.greenInPool(me) shouldBe 0
        driver.getLifeTotal(me) shouldBe 20
        driver.getHandSize(opponent) shouldBe theirHand + 1
    }
})
