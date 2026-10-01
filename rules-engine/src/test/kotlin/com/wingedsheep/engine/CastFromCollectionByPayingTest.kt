package com.wingedsheep.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.costs.CostAtomAmounts
import com.wingedsheep.engine.mechanics.cost.PlayerCounterPayment
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.engine.state.ZoneKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain as shouldContainText

/**
 * Resolution-time casting for a substitute cost — `Effects.CastFromCollectionByPaying`, the
 * `alternativeCost` of [CastFromCollectionWithoutPayingCostEffect] — and the cost amount that prices
 * it, "an amount of {E} equal to its mana value" (`DynamicAmounts.sourceManaValue()` on a
 * player-counter cost, read off the spell being cast).
 *
 * Rules pinned here: the substitute is an alternative cost, so the mana cost is not paid
 * (CR 118.9); a cost that can't be paid means the spell can't be cast (CR 601.2h) and the card stays
 * where it is; a spell cast while another object resolves ignores its card type's timing (the
 * rulings on every "cast it during resolution" card); the cast handler, not the executor, charges
 * the cost.
 * Card-level coverage lives in `AmpedRaptorScenarioTest`.
 */
class CastFromCollectionByPayingTest : FunSpec({

    val energyEqualToManaValue = Costs.additional.PayPlayerCounters(CounterType.ENERGY, DynamicAmounts.sourceManaValue())

    // "Exile cards from the top of your library until you exile a nonland card. Cast that card by
    // paying an amount of {E} equal to its mana value rather than paying its mana cost." No "may":
    // the executor's own affordability gate is what's under test.
    val Caster = card("Energy Cast Test") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val (nonland, exiled) = gatherUntilMatch(GameObjectFilter.Nonland)
                exile(exiled)
                run(Effects.CastFromCollectionByPaying(nonland, energyEqualToManaValue))
            }
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Caster))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.energy(player: EntityId = player1): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.setEnergy(n: Int, player: EntityId = player1) = replaceState(
        state.updateEntity(player) { it.with(CountersComponent(mapOf(CounterType.ENERGY to n))) }
    )

    fun GameTestDriver.castCaster() {
        val caster = putCardInHand(player1, "Energy Cast Test")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, caster).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.isExiled(cardId: EntityId) = cardId in state.getZone(ZoneKey(player1, Zone.EXILE))

    test("the card is cast for energy equal to its mana value, paying no mana") {
        val d = newDriver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser") // {2}{G}, mana value 3
        d.setEnergy(5)

        d.castCaster()

        d.energy() shouldBe 2
        d.getStackSpellNames() shouldBe listOf("Centaur Courser")
        // The substitute cost was owed by that one cast; nothing of it rides the spell onward.
        d.state.getEntity(courser)?.get<PlayWithAdditionalCostComponent>() shouldBe null
        d.state.getEntity(courser)?.get<PlayWithoutPayingCostComponent>() shouldBe null
        d.bothPass()
        d.findPermanent(d.player1, "Centaur Courser") shouldNotBe null
    }

    test("lands exiled on the way are skipped; a sorcery-speed card is cast mid-resolution of an instant") {
        val d = newDriver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        d.putCardOnTopOfLibrary(d.player1, "Mountain")
        d.setEnergy(3)
        d.passPriorityUntil(Step.BEGIN_COMBAT)

        d.castCaster()

        d.energy() shouldBe 0
        d.getStackSpellNames() shouldBe listOf("Centaur Courser")
        d.getExileCardNames(d.player1) shouldContain "Mountain"
        d.isExiled(courser) shouldBe false
    }

    test("exactly enough energy is enough") {
        val d = newDriver()
        d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt") // mana value 1, needs a target
        d.setEnergy(1)

        d.castCaster()

        // The bolt's target is chosen as it is cast; the energy goes with the cast, not before.
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.energy() shouldBe 0
        d.getStackSpellNames() shouldBe listOf("Lightning Bolt")
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("too little energy: nothing is cast, nothing is spent, and the card stays in exile ungranted") {
        val d = newDriver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        d.setEnergy(2)

        d.castCaster()

        d.energy() shouldBe 2
        d.getStackSpellNames() shouldBe emptyList()
        d.isExiled(courser) shouldBe true
        val card = d.state.getEntity(courser)!!
        card.get<PlayWithAdditionalCostComponent>() shouldBe null
        card.get<PlayWithoutPayingCostComponent>() shouldBe null
        d.state.mayPlayPermissions.none { courser in it.cardIds } shouldBe true
        // And it can't be cast from exile later this turn.
        d.legalActions(d.player1).none { (it.action as? CastSpell)?.cardId == courser } shouldBe true
    }

    test("the cast handler charges the cost: a stamped card can't be cast without the energy") {
        val d = newDriver()
        val courser = d.putCardInExile(d.player1, "Centaur Courser")
        d.setEnergy(2)
        // The state the executor builds for the cast, with one energy too few.
        val (permId, withPermission) = d.state.newEntity()
        d.replaceState(
            withPermission.addMayPlayPermission(
                MayPlayPermission(permId, setOf(courser), d.player1, timestamp = withPermission.timestamp)
            ).updateEntity(courser) {
                it.with(PlayWithoutPayingCostComponent(d.player1))
                    .with(PlayWithAdditionalCostComponent(d.player1, listOf(energyEqualToManaValue)))
            }
        )

        val result = d.submitExpectFailure(CastSpell(d.player1, courser))
        (result.error ?: "") shouldContainText "energy"
        d.energy() shouldBe 2
        d.isExiled(courser) shouldBe true

        // With the energy, the same cast goes through and spends exactly the mana value.
        d.setEnergy(3)
        d.submit(CastSpell(d.player1, courser)).error shouldBe null
        d.energy() shouldBe 0
    }

    test("'equal to its mana value' is priced off the spell, and fails closed with no spell to read") {
        val d = newDriver()
        val courser = d.putCardInExile(d.player1, "Centaur Courser")
        val amount = CostAtom.PayPlayerCounters.SOURCE_MANA_VALUE

        CostAtomAmounts.evaluate(d.state, amount, sourceId = courser) shouldBe 3
        CostAtomAmounts.evaluate(d.state, amount) shouldBe CostAtomAmounts.UNPRICEABLE

        d.setEnergy(100)
        // On an activated ability's cost there is no spell to price it against: unpayable, not free.
        PlayerCounterPayment.canAffordAbility(
            d.state, d.player1, AbilityCost.Atom(CostAtom.PayPlayerCounters(CounterType.ENERGY, amount))
        ) shouldBe false
        PlayerCounterPayment.canAffordSpell(d.state, d.player1, listOf(energyEqualToManaValue), courser) shouldBe true
    }

    test("the SDK shape: an alternative cost replaces the mana cost and reads as the Oracle text does") {
        val effect = Effects.CastFromCollectionByPaying(
            com.wingedsheep.sdk.dsl.CollectionSlot("card"), energyEqualToManaValue
        ) as CastFromCollectionWithoutPayingCostEffect
        effect.description shouldBe
            "Cast that card by paying an amount of energy counters equal to its mana value rather than paying its mana cost"
        shouldThrow<IllegalArgumentException> {
            CastFromCollectionWithoutPayingCostEffect(from = "card", payManaCost = true, alternativeCost = energyEqualToManaValue)
        }
        // A pipeline-only amount still isn't a cost.
        shouldThrow<IllegalArgumentException> {
            CostAtom.PayPlayerCounters(CounterType.ENERGY, DynamicAmounts.manaValueOf("card"))
        }
    }
})
