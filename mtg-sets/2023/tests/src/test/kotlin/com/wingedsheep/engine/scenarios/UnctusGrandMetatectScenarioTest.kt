package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.UnctusGrandMetatect
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unctus, Grand Metatect (ONE #75) — {1}{U}{U} 2/4 Legendary Artifact Creature — Phyrexian Vedalken.
 *
 * Other blue creatures you control have "Whenever this creature becomes tapped, draw a card, then
 * discard a card." Other artifact creatures you control get +1/+1. {U/P}: target creature you
 * control becomes a blue artifact until end of turn (sorcery speed).
 *
 * The grant's colour filter is the point: a green creature must not loot until it is turned blue.
 */
class UnctusGrandMetatectScenarioTest : FunSpec({

    val paintId = UnctusGrandMetatect.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UnctusGrandMetatect))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun creature(driver: GameTestDriver, name: String): EntityId =
        driver.putCreatureOnBattlefield(driver.player1, name).also { driver.removeSummoningSickness(it) }

    /** Attack with [attackers]; returns the number of triggers that went on the stack. */
    fun attack(driver: GameTestDriver, attackers: List<EntityId>): Int {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, attackers, driver.player2).error shouldBe null
        return driver.state.stack.size
    }

    /** Resolve every loot on the stack, discarding the first card each time. */
    fun resolveLoots(driver: GameTestDriver) {
        while (driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) {
            if (driver.state.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("another blue creature you control loots when it becomes tapped") {
        val driver = newDriver()
        creature(driver, "Unctus, Grand Metatect")
        val walker = creature(driver, "Island Walker")
        val handBefore = driver.getHandSize(driver.player1)
        val graveyardBefore = driver.getGraveyard(driver.player1).size

        attack(driver, listOf(walker)) shouldBe 1
        resolveLoots(driver)

        driver.getHandSize(driver.player1) shouldBe handBefore
        driver.getGraveyard(driver.player1).size shouldBe graveyardBefore + 1
    }

    test("a non-blue creature and Unctus itself do not have the loot trigger") {
        val driver = newDriver()
        val unctus = creature(driver, "Unctus, Grand Metatect")
        val bears = creature(driver, "Grizzly Bears")

        attack(driver, listOf(unctus, bears)) shouldBe 0
    }

    test("{U/P} makes a creature a blue artifact: it gets +1/+1 and loots when tapped") {
        val driver = newDriver()
        creature(driver, "Unctus, Grand Metatect")
        val bears = creature(driver, "Grizzly Bears")
        driver.state.projectedState.getPower(bears) shouldBe 2

        driver.giveMana(driver.player1, Color.BLUE, 1)
        driver.submitSuccess(
            ActivateAbility(driver.player1, driver.findPermanent(driver.player1, "Unctus, Grand Metatect")!!, paintId,
                targets = listOf(ChosenTarget.Permanent(bears)))
        )
        driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 3

        val graveyardBefore = driver.getGraveyard(driver.player1).size
        attack(driver, listOf(bears)) shouldBe 1
        resolveLoots(driver)
        driver.getGraveyard(driver.player1).size shouldBe graveyardBefore + 1
    }

    test("an opponent's blue creature does not get the trigger") {
        val driver = newDriver()
        creature(driver, "Unctus, Grand Metatect")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Island Walker")

        driver.passPriorityUntil(Step.UPKEEP) // opponent's turn
        driver.removeSummoningSickness(theirs)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player2, listOf(theirs), driver.player1).error shouldBe null
        driver.state.stack.size shouldBe 0
    }
})
