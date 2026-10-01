package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.MiglozMazeCrusher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Migloz, Maze Crusher (ONE #210) — {1}{R}{G} 4/4 Legendary Creature — Phyrexian Beast.
 *
 * Enters with five oil counters; three activated abilities each spend oil (1 / 2 / 3).
 */
class MiglozMazeCrusherScenarioTest : FunSpec({

    val evasionId = MiglozMazeCrusher.activatedAbilities[0].id
    val pumpId = MiglozMazeCrusher.activatedAbilities[1].id
    val destroyId = MiglozMazeCrusher.activatedAbilities[2].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MiglozMazeCrusher))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castMigloz(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val migloz = driver.putCardInHand(player, "Migloz, Maze Crusher")
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, migloz).error shouldBe null
        driver.bothPass()
        return migloz
    }

    test("enters with five oil counters") {
        val driver = newDriver()
        val migloz = castMigloz(driver)
        oil(driver, migloz) shouldBe 5
    }

    test("{1}, remove one oil: gains vigilance and menace until end of turn") {
        val driver = newDriver()
        val migloz = castMigloz(driver)
        driver.giveMana(driver.player1, Color.GREEN, 1)
        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = migloz, abilityId = evasionId))
        oil(driver, migloz) shouldBe 4
        driver.bothPass()
        driver.state.projectedState.hasKeyword(migloz, Keyword.VIGILANCE) shouldBe true
        driver.state.projectedState.hasKeyword(migloz, Keyword.MENACE) shouldBe true
    }

    test("{2}, remove two oil: gets +2/+2 until end of turn") {
        val driver = newDriver()
        val migloz = castMigloz(driver)
        driver.giveMana(driver.player1, Color.GREEN, 2)
        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = migloz, abilityId = pumpId))
        oil(driver, migloz) shouldBe 3
        driver.bothPass()
        driver.state.projectedState.getPower(migloz) shouldBe 6
        driver.state.projectedState.getToughness(migloz) shouldBe 6
    }

    test("{3}, remove three oil: destroys target artifact or enchantment") {
        val driver = newDriver()
        val migloz = castMigloz(driver)
        val enchantment = driver.putPermanentOnBattlefield(driver.player2, "Test Enchantment")
        driver.giveMana(driver.player1, Color.GREEN, 3)
        driver.submitSuccess(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = migloz,
                abilityId = destroyId,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
            )
        )
        oil(driver, migloz) shouldBe 2
        driver.bothPass()
        driver.getGraveyard(driver.player2).contains(enchantment) shouldBe true
    }

    test("can't target a creature with the destroy ability") {
        val driver = newDriver()
        val migloz = castMigloz(driver)
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.giveMana(driver.player1, Color.GREEN, 3)
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = migloz,
                abilityId = destroyId,
                targets = listOf(ChosenTarget.Permanent(bear)),
            )
        )
        oil(driver, migloz) shouldBe 5
    }

    test("can't activate without enough oil") {
        val driver = newDriver()
        val migloz = driver.putCreatureOnBattlefield(driver.player1, "Migloz, Maze Crusher")
        driver.addComponent(migloz, CountersComponent(mapOf(CounterType.OIL to 2)))
        val enchantment = driver.putPermanentOnBattlefield(driver.player2, "Test Enchantment")
        driver.giveMana(driver.player1, Color.GREEN, 3)
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = migloz,
                abilityId = destroyId,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
            )
        )
        oil(driver, migloz) shouldBe 2
        driver.findPermanent(driver.player2, "Test Enchantment") shouldBe enchantment
    }
})
