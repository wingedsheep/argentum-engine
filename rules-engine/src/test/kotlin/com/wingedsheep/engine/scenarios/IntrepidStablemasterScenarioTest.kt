package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.IntrepidStablemaster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Intrepid Stablemaster (OTJ #169) — {1}{G} Human Scout, 2/2, Reach.
 *
 *   {T}: Add {G}.
 *   {T}: Add two mana of any one color. Spend this mana only to cast Mount or Vehicle spells.
 *
 * Exercises the restricted any-color mana ([ManaRestriction.SubtypeSpellsOnly] for Mount/Vehicle):
 * the two mana may pay for a Mount or Vehicle spell, but not for an unrelated spell.
 */
class IntrepidStablemasterScenarioTest : FunSpec({

    val testMount = CardDefinition.creature(
        name = "Test Mount",
        manaCost = ManaCost.parse("{R}{R}"),
        subtypes = setOf(Subtype("Mount")),
        power = 3,
        toughness = 3
    )
    val testVehicle = CardDefinition.artifact(
        name = "Test Vehicle",
        manaCost = ManaCost.parse("{R}{R}"),
        subtypes = setOf(Subtype.VEHICLE),
        oracleText = "Crew 1"
    )
    val testOgre = CardDefinition.creature(
        name = "Test Ogre",
        manaCost = ManaCost.parse("{R}{R}"),
        subtypes = setOf(Subtype("Ogre")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + IntrepidStablemaster + testMount + testVehicle + testOgre)
        return driver
    }

    val restrictedManaAbility = IntrepidStablemaster.activatedAbilities[1].id

    fun GameTestDriver.tapForRestrictedRed(p1: EntityId) {
        val stablemaster = putPermanentOnBattlefield(p1, "Intrepid Stablemaster")
        submit(ActivateAbility(p1, stablemaster, restrictedManaAbility))
        state.pendingDecision?.let { decision ->
            submitDecision(p1, ColorChosenResponse(decision.id, Color.RED))
        }
    }

    test("two restricted mana can pay for a Mount spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapForRestrictedRed(p1)

        val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()
        pool!!.restrictedMana.size shouldBe 2

        val mount = driver.putCardInHand(p1, "Test Mount")
        val result = driver.submit(
            CastSpell(playerId = p1, cardId = mount, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("two restricted mana can pay for a Vehicle spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapForRestrictedRed(p1)

        val vehicle = driver.putCardInHand(p1, "Test Vehicle")
        val result = driver.submit(
            CastSpell(playerId = p1, cardId = vehicle, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("two restricted mana cannot pay for a non-Mount, non-Vehicle spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.tapForRestrictedRed(p1)

        val ogre = driver.putCardInHand(p1, "Test Ogre")
        val result = driver.submit(
            CastSpell(playerId = p1, cardId = ogre, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }
})
