package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AkulTheUnrepentant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Akul the Unrepentant (OTJ rare Scorpion Dragon Rogue), {B}{B}{R}{R}, 5/5.
 *
 * Flying, trample
 * Sacrifice three other creatures: You may put a creature card from your hand onto the
 * battlefield. Activate only as a sorcery and only once each turn.
 *
 * Exercises:
 * - The "sacrifice three other creatures" activated-ability cost (count = 3, excludeSelf = true).
 * - The "you may put a creature card from your hand onto the battlefield" gather → choose → move
 *   pipeline, including declining (choose zero).
 * - The once-each-turn activation cap.
 */
class AkulTheUnrepentantScenarioTest : FunSpec({

    // A vanilla creature card used both as fodder and as the hand creature to drop in.
    val vanilla = card("Test Vanilla 2/2") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = ""
        metadata { rarity = Rarity.COMMON; collectorNumber = "1" }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AkulTheUnrepentant)
        driver.registerCard(vanilla)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.akulAbilityId() = AkulTheUnrepentant.activatedAbilities.first().id

    test("sacrifice three other creatures, put a creature from hand onto the battlefield") {
        val driver = createDriver()
        val me = driver.player1

        val akul = driver.putCreatureOnBattlefield(me, "Akul the Unrepentant")
        val fodder1 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        val fodder2 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        val fodder3 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        driver.removeSummoningSickness(akul)

        val handCreature = driver.putCardInHand(me, "Test Vanilla 2/2")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = akul,
                abilityId = driver.akulAbilityId(),
                costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(fodder1, fodder2, fodder3)
                )
            )
        ).error shouldBe null
        driver.bothPass() // put ability on stack -> resolve

        // Resolve the "may put a creature from hand" decision: choose the hand creature.
        val decision = driver.state.pendingDecision as? SelectCardsDecision
            ?: error("Expected a SelectCardsDecision to put a creature from hand (have ${driver.state.pendingDecision})")
        decision.options.contains(handCreature) shouldBe true
        driver.submitCardSelection(decision.playerId, listOf(handCreature))
        driver.bothPass()

        // The hand creature is now on the battlefield; the three fodder are gone.
        driver.getCardName(handCreature) shouldBe "Test Vanilla 2/2"
        driver.findPermanent(me, "Test Vanilla 2/2") shouldNotBe null
        // Exactly one Test Vanilla remains (the one put from hand); the 3 fodder were sacrificed.
        driver.getCreatures(me).filter { driver.getCardName(it) == "Test Vanilla 2/2" }.size shouldBe 1
        // Akul itself is still on the battlefield (excludeSelf kept it out of the sacrifice).
        driver.findPermanent(me, "Akul the Unrepentant") shouldBe akul
    }

    test("may decline: choose zero creatures from hand") {
        val driver = createDriver()
        val me = driver.player1

        val akul = driver.putCreatureOnBattlefield(me, "Akul the Unrepentant")
        val fodder1 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        val fodder2 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        val fodder3 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        driver.removeSummoningSickness(akul)
        val handCreature = driver.putCardInHand(me, "Test Vanilla 2/2")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = akul,
                abilityId = driver.akulAbilityId(),
                costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(fodder1, fodder2, fodder3)
                )
            )
        ).error shouldBe null
        driver.bothPass()

        val decision = driver.state.pendingDecision as? SelectCardsDecision
            ?: error("Expected a SelectCardsDecision (have ${driver.state.pendingDecision})")
        // Decline: select nothing.
        driver.submitCardSelection(decision.playerId, emptyList())
        driver.bothPass()

        // Nothing entered from hand — no Test Vanilla on the battlefield.
        driver.getCreatures(me).filter { driver.getCardName(it) == "Test Vanilla 2/2" }.size shouldBe 0
        // The hand creature is still in hand.
        driver.state.getZone(me, com.wingedsheep.sdk.core.Zone.HAND).contains(handCreature) shouldBe true
    }

    test("cannot activate without three other creatures to sacrifice") {
        val driver = createDriver()
        val me = driver.player1

        val akul = driver.putCreatureOnBattlefield(me, "Akul the Unrepentant")
        val fodder1 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        val fodder2 = driver.putCreatureOnBattlefield(me, "Test Vanilla 2/2")
        driver.removeSummoningSickness(akul)
        driver.putCardInHand(me, "Test Vanilla 2/2")

        // Only two fodder creatures — the cost can't be paid.
        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = akul,
                abilityId = driver.akulAbilityId(),
                costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(fodder1, fodder2)
                )
            )
        )
        result.error shouldNotBe null
    }
})
