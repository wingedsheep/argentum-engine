package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.SoulRansom
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private data class SoulRansomBoard(
    val driver: GameTestDriver,
    val p1: EntityId,
    val p2: EntityId,
    val creature: EntityId,
    val aura: EntityId,
)

/**
 * Soul Ransom (GTC #198)
 *
 * "Enchant creature / You control enchanted creature. / Discard two cards: This Aura's controller
 *  sacrifices it, then draws two cards. Only your opponents may activate this ability."
 *
 * The ransom is paid by the opponent but its proceeds go to the Aura's controller: the activator
 * discards, the Aura's controller sacrifices it and draws, and the stolen creature goes home.
 */
class SoulRansomScenarioTest : FunSpec({

    val abilityId = SoulRansom.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SoulRansom)
        return driver
    }

    /** p1 steals p2's Centaur Courser with Soul Ransom. */
    fun stolenBoard(): SoulRansomBoard {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p2, "Centaur Courser")
        val aura = driver.putCardInHand(p1, "Soul Ransom")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.giveMana(p1, Color.BLACK, 2)
        driver.castSpell(p1, aura, listOf(creature)).error shouldBe null
        driver.bothPass()

        driver.state.projectedState.getController(creature) shouldBe p1
        return SoulRansomBoard(driver, p1, p2, creature, aura)
    }

    test("an opponent discards two cards; the Aura's controller sacrifices it and draws two") {
        val (driver, p1, p2, creature, aura) = stolenBoard()

        val discardA = driver.putCardInHand(p2, "Grizzly Bears")
        val discardB = driver.putCardInHand(p2, "Grizzly Bears")
        val p1HandBefore = driver.getHandSize(p1)
        val p2HandBefore = driver.getHandSize(p2)

        // Hand priority to the opponent with an empty stack.
        driver.passPriority(p1)
        driver.priorityPlayer shouldBe p2
        driver.legalActions(p2).map { it.action }.any {
            it is ActivateAbility && it.sourceId == aura && it.abilityId == abilityId
        } shouldBe true

        val result = driver.submit(
            ActivateAbility(
                playerId = p2,
                sourceId = aura,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(discardA, discardB)),
            )
        )
        result.outcome shouldBe Outcome.Done
        driver.bothPass()

        // The opponent paid two cards and drew nothing.
        driver.getHandSize(p2) shouldBe p2HandBefore - 2
        driver.state.getGraveyard(p2).containsAll(listOf(discardA, discardB)) shouldBe true
        // The Aura's controller lost it and drew two.
        driver.state.getGraveyard(p1).contains(aura) shouldBe true
        driver.getHandSize(p1) shouldBe p1HandBefore + 2
        // With the Aura gone, the creature returns to its owner.
        driver.state.projectedState.getController(creature) shouldBe p2
    }

    test("the Aura's controller can't activate the ransom") {
        val (driver, p1, _, _, aura) = stolenBoard()

        val a = driver.putCardInHand(p1, "Grizzly Bears")
        val b = driver.putCardInHand(p1, "Grizzly Bears")

        driver.legalActions(p1).map { it.action }.any {
            it is ActivateAbility && it.sourceId == aura
        } shouldBe false
        driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = aura,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(a, b)),
            )
        ).error shouldNotBe null
    }
})
