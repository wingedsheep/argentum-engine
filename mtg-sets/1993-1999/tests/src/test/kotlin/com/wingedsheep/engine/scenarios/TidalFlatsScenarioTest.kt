package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.TidalFlats
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Tidal Flats (Fallen Empires).
 *
 * Two asymmetries make this card easy to get wrong, and both are pinned here: the toll is paid by
 * the *attacker's* controller, while the first strike goes to the *Tidal Flats controller's*
 * blockers — and "creatures you control blocking that creature" means the loop's current attacker,
 * not the enchantment.
 */
class TidalFlatsScenarioTest : FunSpec({

    val abilityId = TidalFlats.activatedAbilities.first().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TidalFlats)
        return driver
    }

    test("an attacker whose controller can't pay hands first strike to its blocker") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(alice, "Raging Goblin")
        driver.removeSummoningSickness(attacker)
        val flats = driver.putPermanentOnBattlefield(bob, "Tidal Flats")
        val blocker = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(attacker), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(blocker to listOf(attacker)))

        driver.giveMana(bob, Color.BLUE, 2)
        driver.submitSuccess(
            ActivateAbility(playerId = bob, sourceId = flats, abilityId = abilityId)
        )
        // Alice has no mana floating, so the {1} is unpayable and the rider lands unprompted.
        driver.bothPass()

        withClue("Bob's blocker picked up first strike") {
            projector.project(driver.state).hasKeyword(blocker, Keyword.FIRST_STRIKE) shouldBe true
        }
    }

    test("an attacker whose controller declines the {1} still hands the blocker first strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(alice, "Raging Goblin")
        driver.removeSummoningSickness(attacker)
        val flats = driver.putPermanentOnBattlefield(bob, "Tidal Flats")
        val blocker = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(attacker), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(blocker to listOf(attacker)))

        driver.giveMana(bob, Color.BLUE, 2)
        driver.submitSuccess(
            ActivateAbility(playerId = bob, sourceId = flats, abilityId = abilityId)
        )
        // The decline path is the one real games take, and it is *not* the unpayable path above:
        // Alice can afford the toll, so she is prompted, and the rider runs from the resumed
        // continuation rather than inline. That resume used to drop the ForEach loop's current
        // attacker, leaving "creatures you control blocking that creature" matching nothing.
        driver.giveColorlessMana(alice, 1)
        driver.bothPass()
        driver.submitYesNo(alice, false)

        withClue("declining the toll is the same as being unable to pay it") {
            projector.project(driver.state).hasKeyword(blocker, Keyword.FIRST_STRIKE) shouldBe true
        }
    }

    test("an attacker whose controller pays the {1} leaves the blocker ordinary") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(alice, "Raging Goblin")
        driver.removeSummoningSickness(attacker)
        val flats = driver.putPermanentOnBattlefield(bob, "Tidal Flats")
        val blocker = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(attacker), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(blocker to listOf(attacker)))

        driver.giveMana(bob, Color.BLUE, 2)
        driver.submitSuccess(
            ActivateAbility(playerId = bob, sourceId = flats, abilityId = abilityId)
        )
        driver.giveColorlessMana(alice, 1)
        driver.bothPass()
        driver.submitYesNo(alice, true)

        withClue("the toll was Alice's to pay, and she paid it") {
            projector.project(driver.state).hasKeyword(blocker, Keyword.FIRST_STRIKE) shouldBe false
        }
    }
})
