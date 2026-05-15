package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD test for the cost primitive: "remove N counters of a given type from among
 * permanents matching a filter you control."
 *
 * Scenario: Remove two +1/+1 counters distributed across two artifacts you control.
 *
 * GIVEN the active player controls two artifact permanents each with one +1/+1 counter
 * AND   the active player controls one non-artifact creature with two +1/+1 counters
 * AND   an ability with cost "Remove two +1/+1 counters from among artifacts you control"
 *       is available
 * WHEN  the active player activates the ability, choosing to remove one counter from
 *       each of the two artifacts
 * THEN  the cost is accepted as paid
 * AND   exactly two counters are removed in total (one from each chosen artifact)
 * AND   the non-artifact creature's counters are unchanged (filter restricted removal
 *       to artifacts the player controls)
 * AND   the ability's effect is queued for resolution, confirming the cost-payment
 *       code path completed successfully
 */
class RemoveTwo11CountersFromAmongArtifactsYouControlCostTest : FunSpec({

    val ArtifactAbilitySource = card("Artifact Ability Source") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 4
        oracleText = "Remove two +1/+1 counters from among artifacts you control: Draw a card."

        activatedAbility {
            cost = AbilityCost.RemoveCountersFromAmongFilteredPermanents(
                counterType = "+1/+1",
                count = 2,
                filter = GameObjectFilter.Artifact
            )
            effect = Effects.DrawCards(1)
        }
    }

    val abilityId = ArtifactAbilitySource.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArtifactAbilitySource))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("pay cost by removing two +1/+1 counters distributed across two artifacts you control") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        // The source of the ability (also an artifact, but not chosen for counter removal)
        val source = driver.putCreatureOnBattlefield(controller, "Artifact Ability Source")
        // Two artifacts, each with exactly one +1/+1 counter — will be the cost targets
        val artifact1 = driver.putCreatureOnBattlefield(controller, "Artifact Creature")
        val artifact2 = driver.putCreatureOnBattlefield(controller, "Artifact Creature")
        // One non-artifact creature with two +1/+1 counters — must remain unchanged
        val nonArtifact = driver.putCreatureOnBattlefield(controller, "Centaur Courser")

        driver.replaceState(
            driver.state
                .updateEntity(artifact1) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                .updateEntity(artifact2) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                .updateEntity(nonArtifact) { c ->
                    c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }
        )

        // Activate the ability, distributing one counter removal to each chosen artifact
        val result = driver.submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    counterRemovals = mapOf(artifact1 to 1, artifact2 to 1)
                )
            )
        )

        // THEN the cost is accepted as paid and the ability is queued
        result.isSuccess shouldBe true

        // AND exactly one counter removed from each chosen artifact
        val artifact1Counters = driver.state.getEntity(artifact1)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        artifact1Counters shouldBe 0

        val artifact2Counters = driver.state.getEntity(artifact2)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        artifact2Counters shouldBe 0

        // AND the non-artifact creature's counters are unchanged
        val nonArtifactCounters = driver.state.getEntity(nonArtifact)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        nonArtifactCounters shouldBe 2
    }

    test("activation is rejected when filter-matching permanents have insufficient counters") {
        // GIVEN the source artifact is in play but no other artifact has +1/+1 counters,
        // AND a non-artifact creature has two +1/+1 counters (which must not satisfy the cost)
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val source = driver.putCreatureOnBattlefield(controller, "Artifact Ability Source")
        val nonArtifact = driver.putCreatureOnBattlefield(controller, "Centaur Courser")
        driver.replaceState(
            driver.state.updateEntity(nonArtifact) { c ->
                c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
            }
        )

        // WHEN the player attempts to activate the ability, distributing the removal onto
        // the non-artifact creature (the only entity with counters available)
        val result = driver.submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    counterRemovals = mapOf(nonArtifact to 2)
                )
            )
        )

        // THEN the engine rejects the activation: the filter restricts the cost to
        // artifacts, and no artifact has the required counters
        result.isSuccess shouldBe false
    }
})
