package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.MaxCounters
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * [MaxCounters] — "This permanent can't have more than N <kind> counters on it" (Rasputin
 * Dreamweaver), enforced at the one counter-placement chokepoint.
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | only the counters that fit are put on it | "a placement is cut to what fits" |
 * | at the limit, the placement doesn't happen | "nothing is added at the limit" |
 * | other kinds are unaffected | "other counter kinds are unaffected" |
 * | applies after placement replacements (a doubler can't push past it) | "a doubled entry is still capped" |
 * | proliferate is a placement too | "proliferate can't add past the limit" |
 * | read off projected statics: a granted limit counts | "a granted limit caps the holder" |
 * | more than N anyway → all but N removed as a state-based action (CR 704.5r) | "excess counters are removed as a state-based action" |
 */
class MaxCountersScenarioTest : ScenarioTestBase() {

    private val holder = card("Test Charge Limit") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 2
        toughness = 2
        staticAbility { ability = MaxCounters(CounterType.CHARGE, 3) }
    }

    private val entrant = card("Test Charge Entrant") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        replacementEffect(EntersWithCounters(counterType = CounterType.CHARGE, count = 3, selfOnly = true))
        staticAbility { ability = MaxCounters(CounterType.CHARGE, 3) }
    }

    private val doubler = card("Test Charge Doubler") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        replacementEffect(
            DoubleCounterPlacement(
                appliesTo = EventPattern.CounterPlacementEvent(CounterType.CHARGE, Recipient.CreatureYouControl)
            )
        )
    }

    private val granter = card("Test Plus Limit Granter") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantStaticAbility(MaxCounters(CounterType.PLUS_ONE_PLUS_ONE, 1), GroupFilter.AllCreaturesYouControl)
        }
    }

    private val chargeTwo = card("Test Charge Two") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.CHARGE, 2, t)
        }
    }

    private val plusFive = card("Test Plus Five") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 5, t)
        }
    }

    private val proliferate = card("Test Limit Proliferate") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.Proliferate() }
    }

    private fun TestGame.countersOn(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Holder", "Opponent")
        .withLandsOnBattlefield(1, "Island", 4)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.cast(name: String, target: EntityId? = null) {
        castSpell(1, name, targetId = target).error shouldBe null
        resolveStack()
    }

    init {
        listOf(holder, entrant, doubler, granter, chargeTwo, plusFive, proliferate).forEach { cardRegistry.register(it) }

        test("a placement is cut to what fits") {
            val game = board().withCardOnBattlefield(1, "Test Charge Limit")
                .withCardInHand(1, "Test Charge Two").withCardInHand(1, "Test Charge Two").build()
            val id = game.findPermanent("Test Charge Limit")!!
            game.cast("Test Charge Two", id)
            game.countersOn(id, CounterType.CHARGE) shouldBe 2
            game.cast("Test Charge Two", id)
            game.countersOn(id, CounterType.CHARGE) shouldBe 3
        }

        test("nothing is added at the limit") {
            val game = board().withCardOnBattlefield(1, "Test Charge Entrant")
                .withCardInHand(1, "Test Charge Two").build()
            val id = game.findPermanent("Test Charge Entrant")!!
            game.state = game.state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.CHARGE, 3)) }
            game.cast("Test Charge Two", id)
            game.countersOn(id, CounterType.CHARGE) shouldBe 3
        }

        test("other counter kinds are unaffected") {
            val game = board().withCardOnBattlefield(1, "Test Charge Limit")
                .withCardInHand(1, "Test Plus Five").build()
            val id = game.findPermanent("Test Charge Limit")!!
            game.cast("Test Plus Five", id)
            game.countersOn(id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
        }

        test("a doubled entry is still capped") {
            val game = board().withCardOnBattlefield(1, "Test Charge Doubler")
                .withCardInHand(1, "Test Charge Entrant").build()
            game.cast("Test Charge Entrant")
            val id = game.findPermanent("Test Charge Entrant")!!
            game.countersOn(id, CounterType.CHARGE) shouldBe 3
        }

        test("proliferate can't add past the limit") {
            val game = board().withCardOnBattlefield(1, "Test Charge Entrant")
                .withCardInHand(1, "Test Limit Proliferate").build()
            val id = game.findPermanent("Test Charge Entrant")!!
            game.state = game.state.updateEntity(id) {
                it.with(CountersComponent().withAdded(CounterType.CHARGE, 3).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
            game.castSpell(1, "Test Limit Proliferate").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(id)).error shouldBe null
            game.countersOn(id, CounterType.CHARGE) shouldBe 3
            game.countersOn(id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("excess counters are removed as a state-based action") {
            val game = board().withCardOnBattlefield(1, "Test Charge Limit").build()
            val id = game.findPermanent("Test Charge Limit")!!
            game.state = game.state.updateEntity(id) {
                it.with(CountersComponent().withAdded(CounterType.CHARGE, 5).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 5))
            }
            game.checkStateBasedActions()
            game.countersOn(id, CounterType.CHARGE) shouldBe 3
            game.countersOn(id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
        }

        test("a granted limit caps the holder") {
            val game = board().withCardOnBattlefield(1, "Test Plus Limit Granter")
                .withCardOnBattlefield(1, "Test Charge Limit")
                .withCardInHand(1, "Test Plus Five").build()
            val id = game.findPermanent("Test Charge Limit")!!
            game.cast("Test Plus Five", id)
            game.countersOn(id, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
