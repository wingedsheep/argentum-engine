package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ody.cards.BarbarianRing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Barbarian Ring (ODY #313):
 * {T}: Add {R}. This land deals 1 damage to you.
 * Threshold — {R}, {T}, Sacrifice this land: It deals 2 damage to any target. Activate only if
 * there are seven or more cards in your graveyard.
 */
class BarbarianRingScenarioTest : FunSpec({

    val manaAbilityId = BarbarianRing.activatedAbilities[0].id
    val thresholdAbilityId = BarbarianRing.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BarbarianRing)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("{T}: adds {R} and deals 1 damage to its controller") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val ring = driver.putPermanentOnBattlefield(you, "Barbarian Ring")

        val result = driver.submit(ActivateAbility(playerId = you, sourceId = ring, abilityId = manaAbilityId))
        result.outcome shouldBe Outcome.Done

        driver.isTapped(ring) shouldBe true
        driver.state.getEntity(you)?.get<ManaPoolComponent>()?.red shouldBe 1
        driver.getLifeTotal(you) shouldBe 19
    }

    test("threshold ability can't be activated with six cards in graveyard") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val ring = driver.putPermanentOnBattlefield(you, "Barbarian Ring")
        repeat(6) { driver.putCardInGraveyard(you, "Mountain") }
        driver.giveMana(you, Color.RED, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = ring,
                abilityId = thresholdAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
            )
        )
        result.outcome shouldNotBe Outcome.Done
        driver.findPermanent(you, "Barbarian Ring") shouldBe ring
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("with threshold, {R}, {T}, sacrifice deals 2 damage to any target") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val ring = driver.putPermanentOnBattlefield(you, "Barbarian Ring")
        repeat(7) { driver.putCardInGraveyard(you, "Mountain") }
        driver.giveMana(you, Color.RED, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = ring,
                abilityId = thresholdAbilityId,
                targets = listOf(ChosenTarget.Player(opponent)),
            )
        )
        result.outcome shouldBe Outcome.Done
        driver.findPermanent(you, "Barbarian Ring") shouldBe null
        driver.getGraveyardCardNames(you).count { it == "Barbarian Ring" } shouldBe 1

        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 18
        driver.getLifeTotal(you) shouldBe 20
    }
})
