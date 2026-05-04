package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the persist keyword (CR 702.79).
 *
 * Persist: "When this creature is put into a graveyard from the battlefield, if it had no
 * -1/-1 counters on it, return it to the battlefield under its owner's control with a
 * -1/-1 counter on it."
 *
 * Tests use [TestCards.SafeholdElite] as a generic persist creature and Lightning Bolt as
 * the removal — a 2/2 dies to 3 damage via state-based actions.
 */
class PersistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Mountain" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("persist returns the creature with a -1/-1 counter when it dies with no -1/-1 counters") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Safehold Elite on caster's battlefield; caster bolts their own creature.
        driver.putCreatureOnBattlefield(caster, "Safehold Elite")
        val elite = driver.findPermanent(caster, "Safehold Elite")
        elite.shouldNotBeNull()

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)

        driver.castSpell(caster, bolt, listOf(elite)).isSuccess shouldBe true
        driver.bothPass() // resolve Lightning Bolt (3 damage → lethal)

        // The persist trigger should now be on the stack.
        driver.stackSize shouldBeGreaterThanOrEqual 1

        // Resolve the persist trigger.
        driver.bothPass()

        // Safehold Elite is back on the battlefield under its owner's control.
        val returned = driver.findPermanent(caster, "Safehold Elite")
        returned.shouldNotBeNull()

        // And it has exactly one -1/-1 counter on it.
        val counters = driver.state.getEntity(returned)?.get<CountersComponent>()
        counters.shouldNotBeNull()
        counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
    }

    test("persist does not fire when the creature already has a -1/-1 counter at time of death") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Safehold Elite")
        val elite = driver.findPermanent(caster, "Safehold Elite")
        elite.shouldNotBeNull()

        // Put a -1/-1 counter on it before it dies.
        driver.replaceState(
            driver.state.updateEntity(elite) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
            }
        )

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)

        driver.castSpell(caster, bolt, listOf(elite)).isSuccess shouldBe true
        driver.bothPass() // resolve Lightning Bolt

        // No persist trigger should fire. Safehold Elite stays in the graveyard.
        driver.findPermanent(caster, "Safehold Elite") shouldBe null
        driver.getGraveyardCardNames(caster) shouldContain "Safehold Elite"
    }

    test("a creature returned by persist cannot persist a second time (it now has a -1/-1 counter)") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Safehold Elite")
        val elite = driver.findPermanent(caster, "Safehold Elite")
        elite.shouldNotBeNull()

        // First kill — persist fires.
        val bolt1 = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt1, listOf(elite)).isSuccess shouldBe true
        driver.bothPass() // resolve Lightning Bolt
        driver.bothPass() // resolve persist trigger

        val returned = driver.findPermanent(caster, "Safehold Elite")
        returned.shouldNotBeNull()

        // Second kill — persist does NOT fire (now has a -1/-1 counter).
        val bolt2 = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt2, listOf(returned)).isSuccess shouldBe true
        driver.bothPass() // resolve Lightning Bolt

        driver.findPermanent(caster, "Safehold Elite") shouldBe null
        driver.getGraveyardCardNames(caster).count { it == "Safehold Elite" } shouldBe 1
    }

    test("persist does not fire on tokens (Rule 704.5d — tokens cease to exist)") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Spawn Safehold Elite as a token (has TokenComponent). Persist is granted via base
        // keywords on the card definition, but 702.79b suppresses the return for tokens.
        driver.putCreatureOnBattlefield(caster, "Safehold Elite")
        val tokenId = driver.findPermanent(caster, "Safehold Elite")
        tokenId.shouldNotBeNull()
        driver.replaceState(
            driver.state.updateEntity(tokenId) { c -> c.with(TokenComponent) }
        )

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)

        driver.castSpell(caster, bolt, listOf(tokenId)).isSuccess shouldBe true
        driver.bothPass() // resolve Lightning Bolt

        // The token should be gone — 704.5s removed it from the graveyard, and persist
        // should not have fired.
        driver.findPermanent(caster, "Safehold Elite") shouldBe null
        driver.state.getBattlefield().contains(tokenId) shouldBe false
    }
})
