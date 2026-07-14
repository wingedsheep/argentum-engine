package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD test for the [Costs.RemoveCounters] atom-based cost primitive.
 *
 * Scenario: Remove two +1/+1 counters from among creatures you control
 * as an activated ability cost.
 *
 * GIVEN the active player controls two creature permanents each with one +1/+1 counter
 * AND   the active player controls one non-creature artifact with two +1/+1 counters
 * AND   an ability with cost "Remove two +1/+1 counters from among creatures you control"
 *       is available (Costs.RemoveCounters)
 * WHEN  the active player activates the ability, choosing to remove one counter from
 *       each of the two creatures
 * THEN  the cost is accepted as paid
 * AND   exactly two counters are removed in total (one from each chosen creature)
 * AND   the non-creature artifact's counters are unchanged (filter restricted removal
 *       to creatures the player controls)
 * AND   the ability's effect is queued for resolution
 */
class RemoveCountersAtomCostTest : FunSpec({

    val removeCountersCard = card("Counter Spender") {
        manaCost = "{2}"
        typeLine = "Creature — Human Wizard"
        power = 2
        toughness = 2
        oracleText = "Remove two +1/+1 counters from among creatures you control: Draw a card."

        activatedAbility {
            cost = Costs.RemoveCounters(
                count = 2,
                counterType = "+1/+1",
                filter = GameObjectFilter.Creature
            )
            effect = Effects.DrawCards(1)
        }
    }

    val abilityId = removeCountersCard.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(removeCountersCard))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("check filter ability description") {
        // specific type >1
        removeCountersCard.activatedAbilities[0].cost.description shouldBe
            "Remove two +1/+1 counters from among creatures you control"
        // specific type singular
        Costs.RemoveCounters(count = 1, counterType = "+1/+1", filter = GameObjectFilter.Creature).description shouldBe
            "Remove a +1/+1 counter from a creature you control"
        // x counters specific type
        Costs.RemoveXCounters(counterType = "+1/+1", filter = GameObjectFilter.Creature).description shouldBe
            "Remove X +1/+1 counters from among creatures you control"
        // x counters any type
        Costs.RemoveXCounters().description shouldBe "Remove X counters from among permanents you control"
        // multi non-specific type
        Costs.RemoveCounters(count = 2).description shouldBe "Remove two counters from among permanents you control"
        // singular non-specific type
        Costs.RemoveCounters(count = 1).description shouldBe "Remove a counter from a permanent you control"
        // singular self non-specific type
        Costs.RemoveCounterFromSelf(null).description shouldBe "Remove a counter from this permanent"
        // singular self specific type
        Costs.RemoveCounterFromSelf("+1/+1").description shouldBe "Remove a +1/+1 counter from this permanent"
        // multi self specific type
        Costs.RemoveCounterFromSelf(count = 2, counterType = "+1/+1").description shouldBe
            "Remove two +1/+1 counters from this permanent"
    }

    test("pay cost by removing two +1/+1 counters distributed across two creatures") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        val source = driver.putCreatureOnBattlefield(controller, "Counter Spender")
        val creature1 = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        val creature2 = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        val nonCreature = driver.putCreatureOnBattlefield(controller, "Artifact Creature")

        driver.replaceState(
            driver.state
                .updateEntity(creature1) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                .updateEntity(creature2) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                .updateEntity(nonCreature) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }
        )

        val result = driver.submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(creature1, "+1/+1", 1),
                        DistributedCounterRemoval(creature2, "+1/+1", 1)
                    )
                )
            )
        )

        result.isSuccess shouldBe true

        val c1 = driver.state.getEntity(creature1)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        c1 shouldBe 0

        val c2 = driver.state.getEntity(creature2)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        c2 shouldBe 0

        val nc = driver.state.getEntity(nonCreature)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        nc shouldBe 2
    }

    test("activation rejected when filter-matching permanents have insufficient counters") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val source = driver.putCreatureOnBattlefield(controller, "Counter Spender")
        // "Plains" is a land, not a creature — it won't match filter = GameObjectFilter.Creature
        val land = driver.putLandOnBattlefield(controller, "Plains")
        driver.replaceState(
            driver.state.updateEntity(land) { c ->
                c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
            }
        )

        val result = driver.submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(land, "+1/+1", 2)
                    )
                )
            )
        )

        result.isSuccess shouldBe false
    }

    test("remove counters of any type (counterType = null, Tayam-style)") {
        val AnyTypeSpender = card("Any Type Spender") {
            manaCost = "{2}"
            typeLine = "Creature — Human Wizard"
            power = 2
            toughness = 2
            oracleText = "Remove two counters from among creatures you control: Draw a card."
            activatedAbility {
                cost = Costs.RemoveCounters(
                    count = 2,
                    counterType = null,
                    filter = GameObjectFilter.Creature
                )
                effect = Effects.DrawCards(1)
            }
        }

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(removeCountersCard, AnyTypeSpender))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val controller = driver.activePlayer!!
        val source = driver.putCreatureOnBattlefield(controller, "Any Type Spender")
        val target = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")
        driver.replaceState(
            driver.state.updateEntity(target) { c ->
                c.with(CountersComponent(mapOf(
                    CounterType.PLUS_ONE_PLUS_ONE to 1,
                    CounterType.STUN to 1,
                )))
            }
        )

        val anyTypeAbilityId = AnyTypeSpender.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = anyTypeAbilityId,
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(target, "+1/+1", 1),
                        DistributedCounterRemoval(target, "stun", 1),
                    )
                )
            )
        )

        result.isSuccess shouldBe true
        val after = driver.state.getEntity(target)?.get<CountersComponent>()
        after?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        after?.getCount(CounterType.STUN) shouldBe 0
    }

    test("remove counters from the source permanent through the shared atom") {
        val selfSpender = card("Self Counter Spender") {
            manaCost = "{2}"
            typeLine = "Artifact"
            oracleText = "Remove two charge counters from this permanent: Draw a card."
            activatedAbility {
                cost = Costs.RemoveCounterFromSelf("charge", 2)
                effect = Effects.DrawCards(1)
            }
        }
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(selfSpender))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val controller = driver.activePlayer!!
        val source = driver.putPermanentOnBattlefield(controller, "Self Counter Spender")
        driver.replaceState(
            driver.state.updateEntity(source) { c ->
                c.with(CountersComponent(mapOf(CounterType.CHARGE to 2)))
            }
        )

        val result = driver.submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = selfSpender.activatedAbilities[0].id
            )
        )

        result.isSuccess shouldBe true
        driver.state.getEntity(source)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 0
    }
})
