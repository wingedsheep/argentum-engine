package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cns.cards.PredatorsHowl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PredatorsHowlScenarioTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + PredatorsHowl)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun castHowl(driver: GameTestDriver, player: EntityId) {
        val howl = driver.putCardInHand(player, "Predator's Howl")
        driver.giveMana(player, Color.GREEN, 4)
        driver.castSpell(player, howl).outcome shouldBe Outcome.Done
    }

    fun checkWolves(driver: GameTestDriver, player: EntityId, count: Int) {
        val tokens = driver.state.getBattlefield(player)
            .filter { driver.state.getEntity(it)?.get<TokenComponent>() != null }
        tokens.size shouldBe count
        tokens.forEach {
            val projected = driver.state.projectedState
            projected.getPower(it) shouldBe 2
            projected.getToughness(it) shouldBe 2
            projected.getColors(it) shouldBe setOf(Color.GREEN.name)
            projected.hasSubtype(it, "Wolf") shouldBe true
        }
    }

    test("creates one Wolf when no creature died this turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        castHowl(driver, you)
        driver.bothPass().outcome shouldBe Outcome.Done
        checkWolves(driver, you, 1)
    }

    test("an opponent's creature dying after casting turns on morbid at resolution") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        castHowl(driver, you)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass().outcome shouldBe Outcome.Done
        driver.state.getGraveyard(opponent).contains(victim) shouldBe true
        checkWolves(driver, you, 0)
        driver.bothPass().outcome shouldBe Outcome.Done
        checkWolves(driver, you, 3)
    }

    test("a creature in the graveyard from a previous turn does not enable morbid") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val victim = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass().outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        castHowl(driver, caster)
        driver.bothPass().outcome shouldBe Outcome.Done
        checkWolves(driver, caster, 1)
    }
})
