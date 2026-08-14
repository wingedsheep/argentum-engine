package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Marketback Walker (DFT #235).
 *
 * {X}{X} Artifact Creature — Construct 0/0
 * "This creature enters with X +1/+1 counters on it.
 *  {4}: Put a +1/+1 counter on this creature.
 *  When this creature dies, draw a card for each +1/+1 counter on it."
 *
 * The point of interest is the **doubled** {X}: the caster announces one X (CR 601.2b) and pays it
 * once per {X} symbol, so X=3 costs six mana while still yielding only three counters. That path —
 * `ManaCost.xCount` on a *card's* mana cost rather than an activated ability's — is what these
 * tests pin down, alongside the dies trigger reading counters via last-known information.
 */
class MarketbackWalkerScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("X=3 costs six mana and enters as a 3/3 with three +1/+1 counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val walker = driver.putCardInHand(player, "Marketback Walker")
        // {X}{X} with X=3 → 3 + 3 = 6 generic mana.
        driver.giveMana(player, Color.RED, 6)

        driver.castXSpell(player, walker, xValue = 3).isSuccess shouldBe true
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(walker) shouldBe true

        // Base 0/0 plus X counters — the announced X is used once, not twice.
        plusOneCounters(driver, walker) shouldBe 3
        val projected = projector.project(driver.state)
        projected.getPower(walker) shouldBe 3
        projected.getToughness(walker) shouldBe 3
    }

    test("X=3 cannot be paid with only five mana — both {X} symbols are charged") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val walker = driver.putCardInHand(player, "Marketback Walker")
        driver.giveMana(player, Color.RED, 5)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = walker,
                xValue = 3,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // Still in hand, nothing spent on a half-paid cast.
        driver.state.getZone(ZoneKey(player, Zone.HAND)).contains(walker) shouldBe true
    }

    test("X=0 makes a 0/0 that dies immediately to state-based actions and draws nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val walker = driver.putCardInHand(player, "Marketback Walker")
        driver.giveMana(player, Color.RED, 1)

        val handBefore = driver.getHand(player).size

        driver.castXSpell(player, walker, xValue = 0).isSuccess shouldBe true
        driver.bothPass()

        // 0/0 with no counters — SBAs bin it the moment it lands (CR 704.5f).
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(walker) shouldBe true

        // The dies trigger still fires; it just draws zero cards. Net hand change is
        // -1 (the Walker left) and +0 drawn.
        driver.bothPass()
        driver.getHand(player).size shouldBe handBefore - 1
    }

    test("dies trigger draws one card per +1/+1 counter, counting the {4}-ability counter") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val walker = driver.putCardInHand(player, "Marketback Walker")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        // 4 to cast at X=2, then 4 more for the activated ability, then {R} for the Bolt.
        driver.giveMana(player, Color.RED, 9)

        driver.castXSpell(player, walker, xValue = 2).isSuccess shouldBe true
        driver.bothPass()
        plusOneCounters(driver, walker) shouldBe 2

        // {4}: Put a +1/+1 counter on this creature → 3/3 with three counters.
        val addCounter = driver.cardRegistry.requireCard("Marketback Walker").activatedAbilities[0].id
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = walker,
                abilityId = addCounter,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        plusOneCounters(driver, walker) shouldBe 3

        val handBefore = driver.getHand(player).size

        // 3 damage to a 3/3 is lethal.
        driver.castSpell(player, bolt, targets = listOf(walker)).isSuccess shouldBe true
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(walker) shouldBe true

        // Dies trigger resolves: three counters → three cards. The Walker is already a graveyard
        // card with no counters by then, so the count comes from last-known information.
        driver.bothPass()
        driver.getHand(player).size shouldBe handBefore - 1 + 3
    }
})
