package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mor.cards.DoorOfDestinies
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Door of Destinies {4} — Artifact.
 *
 * "As this artifact enters, choose a creature type.
 *  Whenever you cast a spell of the chosen type, put a charge counter on this artifact.
 *  Creatures you control of the chosen type get +1/+1 for each charge counter on this artifact."
 *
 * Pins the cast trigger reading the as-enters choice (only spells of the chosen type add a counter),
 * the trigger resolving before the spell (ruling 2008-04-01: the creature enters already boosted),
 * and the anthem scaling live with the charge-counter count and touching only the chosen type.
 */
class DoorOfDestiniesScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DoorOfDestinies))
        return driver
    }

    fun chargeCounters(driver: GameTestDriver, door: EntityId): Int =
        driver.state.getEntity(door)?.get<CountersComponent>()
            ?.counters?.get(CounterType.CHARGE) ?: 0

    fun castDoorChoosing(driver: GameTestDriver, player: EntityId, creatureType: String): EntityId {
        driver.giveColorlessMana(player, 4)
        val card = driver.putCardInHand(player, "Door of Destinies")
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(creatureType)
        (index >= 0) shouldBe true
        driver.submitDecision(player, OptionChosenResponse(decision.id, index))

        return driver.findPermanent(player, "Door of Destinies")!!
    }

    fun castAndResolve(driver: GameTestDriver, player: EntityId, cardName: String, color: Color) {
        driver.giveMana(player, color, 1)
        driver.giveColorlessMana(player, 1)
        val card = driver.putCardInHand(player, cardName)
        driver.castSpell(player, card).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    test("casting a spell of the chosen type adds a charge counter, and the creature enters boosted") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val zombie = driver.putCreatureOnBattlefield(you, "Fear Creature")   // Zombie 2/2
        val soldier = driver.putCreatureOnBattlefield(you, "Banding Scout")  // Human Soldier 2/2
        val door = castDoorChoosing(driver, you, "Zombie")

        chargeCounters(driver, door) shouldBe 0
        projector.project(driver.state).getPower(zombie) shouldBe 2

        castAndResolve(driver, you, "Black Creature", Color.BLACK)          // Zombie spell
        chargeCounters(driver, door) shouldBe 1

        val newZombie = driver.findPermanent(you, "Black Creature")!!
        val projected = projector.project(driver.state)
        projected.getPower(zombie) shouldBe 3
        projected.getToughness(zombie) shouldBe 3
        projected.getPower(newZombie) shouldBe 3
        projected.getToughness(newZombie) shouldBe 3
        projected.getPower(soldier) shouldBe 2                               // not the chosen type
        projected.getToughness(soldier) shouldBe 2

        castAndResolve(driver, you, "Black Creature", Color.BLACK)
        chargeCounters(driver, door) shouldBe 2
        projector.project(driver.state).getPower(zombie) shouldBe 4
    }

    test("a spell not of the chosen type adds no counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val zombie = driver.putCreatureOnBattlefield(you, "Fear Creature")
        val door = castDoorChoosing(driver, you, "Zombie")

        castAndResolve(driver, you, "Banding Scout", Color.WHITE)            // Human Soldier spell
        chargeCounters(driver, door) shouldBe 0
        projector.project(driver.state).getPower(zombie) shouldBe 2
    }
})
