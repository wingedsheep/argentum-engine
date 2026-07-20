package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.BannerOfKinship
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Banner of Kinship {5} — Artifact.
 *
 * "As this artifact enters, choose a creature type. This artifact enters with a fellowship counter
 *  on it for each creature you control of the chosen type.
 *  Creatures you control of the chosen type get +1/+1 for each fellowship counter on this artifact."
 *
 * The load-bearing ordering claim: the as-enters *choice* must land before the as-enters *count* is
 * evaluated, otherwise the dynamic count filters on an unset chosen type and the Banner arrives
 * blank. These tests pin the count against boards where the two candidate types have different
 * populations, so a mis-ordered implementation cannot pass by coincidence.
 */
class BannerOfKinshipScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BannerOfKinship))
        return driver
    }

    fun fellowshipCounters(driver: GameTestDriver, banner: EntityId): Int =
        driver.state.getEntity(banner)?.get<CountersComponent>()
            ?.counters?.get(CounterType.FELLOWSHIP) ?: 0

    /**
     * Cast the Banner and answer the as-enters creature-type choice with [creatureType].
     * Returns the Banner's battlefield entity id.
     */
    fun castBannerChoosing(driver: GameTestDriver, player: EntityId, creatureType: String): EntityId {
        driver.giveColorlessMana(player, 5)
        val card = driver.putCardInHand(player, "Banner of Kinship")
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(creatureType)
        (index >= 0) shouldBe true // the chosen type is offered
        driver.submitDecision(player, OptionChosenResponse(decision.id, index))

        return driver.findPermanent(player, "Banner of Kinship")!!
    }

    test("enters with one fellowship counter per creature of the chosen type, and anthems only those") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        // Three Zombies and one Human Soldier — so "Zombie" and "Human" give different counts.
        val zombieA = driver.putCreatureOnBattlefield(you, "Fear Creature")     // Zombie 2/2
        val zombieB = driver.putCreatureOnBattlefield(you, "Black Creature")    // Zombie 2/2
        val zombieC = driver.putCreatureOnBattlefield(you, "Black Creature")    // Zombie 2/2
        val soldier = driver.putCreatureOnBattlefield(you, "Banding Scout")     // Human Soldier 2/2

        val banner = castBannerChoosing(driver, you, "Zombie")

        fellowshipCounters(driver, banner) shouldBe 3

        val projected = projector.project(driver.state)
        listOf(zombieA, zombieB, zombieC).forEach { zombie ->
            projected.getPower(zombie) shouldBe 5      // 2 + 3
            projected.getToughness(zombie) shouldBe 5  // 2 + 3
        }
        projected.getPower(soldier) shouldBe 2         // not a Zombie — unaffected
        projected.getToughness(soldier) shouldBe 2
    }

    test("counts only creatures YOU control, and the count is a snapshot taken on entry") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Fear Creature")       // your Zombie
        driver.putCreatureOnBattlefield(opponent, "Black Creature") // opponent's Zombie — not counted
        driver.putCreatureOnBattlefield(opponent, "Black Creature")

        val banner = castBannerChoosing(driver, you, "Zombie")
        fellowshipCounters(driver, banner) shouldBe 1

        // A Zombie arriving later does NOT add a counter (CR 614.1c — the replacement effect only
        // applies while the Banner itself is entering), but it DOES get the live anthem.
        val lateZombie = driver.putCreatureOnBattlefield(you, "Black Creature")
        fellowshipCounters(driver, banner) shouldBe 1

        val projected = projector.project(driver.state)
        projected.getPower(lateZombie) shouldBe 3      // 2 + 1
        projected.getToughness(lateZombie) shouldBe 3
    }

    test("no creatures of the chosen type: enters with no counters and grants nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val zombie = driver.putCreatureOnBattlefield(you, "Fear Creature")
        val banner = castBannerChoosing(driver, you, "Goblin")

        fellowshipCounters(driver, banner) shouldBe 0

        val projected = projector.project(driver.state)
        projected.getPower(zombie) shouldBe 2
        projected.getToughness(zombie) shouldBe 2
    }

    test("the anthem is sized live: removing the Banner's counters shrinks it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val zombie = driver.putCreatureOnBattlefield(you, "Fear Creature")
        driver.putCreatureOnBattlefield(you, "Black Creature")
        val banner = castBannerChoosing(driver, you, "Zombie")
        fellowshipCounters(driver, banner) shouldBe 2

        projector.project(driver.state).getPower(zombie) shouldBe 4 // 2 + 2

        driver.replaceState(
            driver.state.updateEntity(banner) { it.with(CountersComponent(mapOf(CounterType.FELLOWSHIP to 1))) }
        )
        projector.project(driver.state).getPower(zombie) shouldBe 3 // 2 + 1
    }
})
