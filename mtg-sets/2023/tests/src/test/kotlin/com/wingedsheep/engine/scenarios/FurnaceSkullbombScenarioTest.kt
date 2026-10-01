package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.FurnaceSkullbomb
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Furnace Skullbomb (ONE #228) — {1} Artifact.
 *
 * "{1}, Sacrifice this artifact: Draw a card.
 *  {1}{R}, Sacrifice this artifact: Put two oil counters on target artifact or creature you
 *  control. Draw a card. Activate only as a sorcery."
 */
class FurnaceSkullbombScenarioTest : FunSpec({

    val drawAbility = FurnaceSkullbomb.activatedAbilities[0].id
    val oilAbility = FurnaceSkullbomb.activatedAbilities[1].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FurnaceSkullbomb))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    test("{1}, sacrifice: draw a card") {
        val driver = newDriver()
        val bomb = driver.putPermanentOnBattlefield(driver.player1, "Furnace Skullbomb")
        driver.giveMana(driver.player1, Color.RED, 1)
        val handBefore = driver.getHandSize(driver.player1)

        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = bomb, abilityId = drawAbility))
        driver.getGraveyardCardNames(driver.player1) shouldContain "Furnace Skullbomb"
        driver.bothPass()
        driver.getHandSize(driver.player1) shouldBe handBefore + 1
    }

    test("{1}{R}, sacrifice: two oil counters on a creature you control, then draw") {
        val driver = newDriver()
        val bomb = driver.putPermanentOnBattlefield(driver.player1, "Furnace Skullbomb")
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.giveMana(driver.player1, Color.RED, 2)
        val handBefore = driver.getHandSize(driver.player1)

        driver.submitSuccess(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = bomb,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(courser)),
            )
        )
        driver.getGraveyardCardNames(driver.player1) shouldContain "Furnace Skullbomb"
        driver.bothPass()
        oil(driver, courser) shouldBe 2
        driver.getHandSize(driver.player1) shouldBe handBefore + 1
    }

    test("oil ability can't target a creature an opponent controls") {
        val driver = newDriver()
        val bomb = driver.putPermanentOnBattlefield(driver.player1, "Furnace Skullbomb")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.giveMana(driver.player1, Color.RED, 2)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = bomb,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(theirs)),
            )
        )
        oil(driver, theirs) shouldBe 0
    }

    test("oil ability is sorcery-speed only") {
        val driver = newDriver()
        val bomb = driver.putPermanentOnBattlefield(driver.player1, "Furnace Skullbomb")
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.giveMana(driver.player1, Color.RED, 2)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = bomb,
                abilityId = oilAbility,
                targets = listOf(ChosenTarget.Permanent(courser)),
            )
        )
        oil(driver, courser) shouldBe 0

        // Same window, the unrestricted draw ability is fine — the rejection was the timing rule.
        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = bomb, abilityId = drawAbility))
        driver.getGraveyardCardNames(driver.player1) shouldContain "Furnace Skullbomb"
    }
})
