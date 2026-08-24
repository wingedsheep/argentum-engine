package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.RaidingParty
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Raiding Party (Fallen Empires).
 *
 * Entirely a bookkeeping card: each player taps white creatures to spare Plains, the spared Plains
 * accumulate *across* players, and what is destroyed is the complement of that accumulated set.
 * The two cases that matter are a player who spares nothing (their Plains all die) and a player who
 * taps to spare some (exactly those survive).
 */
class RaidingPartyScenarioTest : FunSpec({

    val abilityId = RaidingParty.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RaidingParty)
        return driver
    }

    /** Answer both of a player's prompts: which creatures to tap, then which Plains to spare. */
    fun spare(driver: GameTestDriver, playerId: EntityId, tap: List<EntityId>, plains: List<EntityId>) {
        driver.submitCardSelection(playerId, tap)
        if (driver.state.pendingDecision != null) driver.submitCardSelection(playerId, plains)
    }

    test("Plains nobody spared are destroyed; spared ones survive") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val party = driver.putPermanentOnBattlefield(alice, "Raiding Party")
        val orc = driver.putCreatureOnBattlefield(alice, "Brassclaw Orcs")

        // Bob is the white player: one Savannah Lions to tap, three Plains, of which he can spare
        // at most two.
        val lions = driver.putCreatureOnBattlefield(bob, "Savannah Lions")
        driver.removeSummoningSickness(lions)
        val spared1 = driver.putLandOnBattlefield(bob, "Plains")
        val spared2 = driver.putLandOnBattlefield(bob, "Plains")
        val doomed = driver.putLandOnBattlefield(bob, "Plains")

        driver.submitSuccess(
            ActivateAbility(
                playerId = alice,
                sourceId = party,
                abilityId = abilityId,
                costPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(
                    sacrificedPermanents = listOf(orc)
                )
            )
        )
        driver.bothPass()

        // Alice, the active player, goes first and has no white creatures to tap.
        spare(driver, alice, tap = emptyList(), plains = emptyList())
        // Bob taps his Lions, which buys two Plains.
        spare(driver, bob, tap = listOf(lions), plains = listOf(spared1, spared2))

        withClue("the two spared Plains survived") {
            driver.state.getBattlefield().contains(spared1) shouldBe true
            driver.state.getBattlefield().contains(spared2) shouldBe true
        }
        withClue("the third, spared by nobody, was destroyed") {
            driver.state.getBattlefield().contains(doomed) shouldBe false
        }
        withClue("the Lions were tapped to pay for it") {
            driver.isTapped(lions) shouldBe true
        }
    }
})
