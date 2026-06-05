package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scg.cards.PutridRaptor
import com.wingedsheep.mtg.sets.definitions.scg.cards.RavenGuildInitiate
import com.wingedsheep.mtg.sets.definitions.scg.cards.SkirkVolcanist
import com.wingedsheep.mtg.sets.definitions.scg.cards.ZombieCutthroat
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.costs.PayCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the non-mana morph costs, which turn the creature face up through the shared
 * [CostPaymentService][com.wingedsheep.engine.mechanics.cost.CostPaymentService] (phase 2 of the
 * PayCost-payment unification — see `backlog/paycost-payment-unification.md`).
 *
 * Each non-mana morph cost now pauses for the cost-specific decision (yes/no for life, card
 * selection for discard, battlefield targeting for sacrifice / return / tap) and only flips the
 * creature face up once the cost is actually paid. Declining or being unable to pay leaves the
 * creature face down. This also unlocks the [PayCost.Tap] / [PayCost.Choice] / [PayCost.OwnManaCost]
 * morph costs the old hand-rolled switch rejected.
 */
class MorphCostPaymentTest : FunSpec({

    // A creature with a (newly supported) Tap morph cost — "Morph—Tap an untapped land you control."
    val tapMorphTester = card("Tap Morph Tester") {
        manaCost = "{2}"
        typeLine = "Creature — Wizard"
        power = 2
        toughness = 2
        morphCost = PayCost.Tap(GameObjectFilter.Land, count = 1)
    }

    // A plain Bird for Raven Guild Initiate's "return a Bird" cost to target.
    val testBird = card("Test Falcon") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
    }

    val allCards = TestCards.all + listOf(
        SkirkVolcanist, PutridRaptor, RavenGuildInitiate, ZombieCutthroat, tapMorphTester, testBird
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Morph>().firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morphAbility != null) {
                c = c.with(MorphDataComponent(morphAbility.morphCost, cardDef.name))
            }
            c
        })
        removeSummoningSickness(creatureId)
        return creatureId
    }

    fun GameTestDriver.beginTurn(): EntityId {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        return activePlayer!!
    }

    test("pay life turns the creature face up and loses the life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val cutthroat = driver.putFaceDownCreature(player, "Zombie Cutthroat")

        // The pay-life morph cost now prompts before the flip.
        driver.submit(TurnFaceUp(playerId = player, sourceId = cutthroat)).error shouldBe null
        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>() shouldBe FaceDownComponent

        driver.submitYesNo(player, true)

        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>() shouldBe null
        driver.assertLifeTotal(player, 15)
    }

    test("declining the life payment leaves the creature face down") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val cutthroat = driver.putFaceDownCreature(player, "Zombie Cutthroat")

        driver.submit(TurnFaceUp(playerId = player, sourceId = cutthroat))
        driver.submitYesNo(player, false)

        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        driver.assertLifeTotal(player, 20)
    }

    test("cannot turn face up when life is below the morph cost (CR 119.4)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val cutthroat = driver.putFaceDownCreature(player, "Zombie Cutthroat")
        driver.setLifeTotal(player, 4) // less than the 5-life morph cost

        driver.submit(TurnFaceUp(playerId = player, sourceId = cutthroat)).isSuccess shouldBe false
        driver.state.getEntity(cutthroat)?.get<FaceDownComponent>() shouldBe FaceDownComponent
    }

    test("discard a Zombie card to turn face up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val raptor = driver.putFaceDownCreature(player, "Putrid Raptor")
        val zombieInHand = driver.putCardInHand(player, "Putrid Raptor") // a Zombie card

        driver.submit(TurnFaceUp(playerId = player, sourceId = raptor)).error shouldBe null
        driver.submitCardSelection(player, listOf(zombieInHand))

        driver.state.getEntity(raptor)?.get<FaceDownComponent>() shouldBe null
        driver.getGraveyard(player).contains(zombieInHand) shouldBe true
        driver.getHand(player).contains(zombieInHand) shouldBe false
    }

    test("return a Bird you control to turn face up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val initiate = driver.putFaceDownCreature(player, "Raven Guild Initiate")
        val bird = driver.putCreatureOnBattlefield(player, "Test Falcon")

        driver.submit(TurnFaceUp(playerId = player, sourceId = initiate)).error shouldBe null
        driver.submitCardSelection(player, listOf(bird))

        driver.state.getEntity(initiate)?.get<FaceDownComponent>() shouldBe null
        driver.getHand(player).contains(bird) shouldBe true
    }

    test("sacrifice two Mountains to turn face up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val volcanist = driver.putFaceDownCreature(player, "Skirk Volcanist")
        val m1 = driver.putLandOnBattlefield(player, "Mountain")
        val m2 = driver.putLandOnBattlefield(player, "Mountain")

        driver.submit(TurnFaceUp(playerId = player, sourceId = volcanist)).error shouldBe null
        // Battlefield targeting for the two Mountains; the flip happens before the
        // "when turned face up" damage trigger goes on the stack.
        driver.submitCardSelection(player, listOf(m1, m2))

        driver.state.getEntity(volcanist)?.get<FaceDownComponent>() shouldBe null
        driver.getGraveyard(player).contains(m1) shouldBe true
        driver.getGraveyard(player).contains(m2) shouldBe true
    }

    test("tap a permanent to turn face up — newly unlocked Tap morph cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.beginTurn()

        val tester = driver.putFaceDownCreature(player, "Tap Morph Tester")
        val land = driver.putLandOnBattlefield(player, "Forest")

        driver.submit(TurnFaceUp(playerId = player, sourceId = tester)).error shouldBe null
        driver.submitCardSelection(player, listOf(land))

        driver.state.getEntity(tester)?.get<FaceDownComponent>() shouldBe null
        driver.state.getEntity(land)?.get<TappedComponent>() shouldNotBe null
    }
})
