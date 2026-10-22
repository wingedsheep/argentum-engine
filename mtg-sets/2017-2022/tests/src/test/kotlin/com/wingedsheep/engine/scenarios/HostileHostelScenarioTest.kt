package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Hostile Hostel // Creeping Inn (MID).
 *
 * Pins the soul-counter flip (remove the counters, transform, then untap) and Creeping Inn's
 * attack trigger: X counts every creature card exiled *with this permanent* — earlier exiles
 * included, unrelated exiled cards not — and declining the exile does nothing.
 */
class HostileHostelScenarioTest : ScenarioTestBase() {

    private val soulAbilityId
        get() = cardRegistry.getCard("Hostile Hostel")!!.script.activatedAbilities[1].id

    private fun builder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Hostile Hostel")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.name(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.soulCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SOUL) ?: 0

    private fun TestGame.seedSoulCounters(id: EntityId, count: Int) {
        state = state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.SOUL to count))) }
    }

    private fun TestGame.activateSacrificingBears(hostel: EntityId) {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = hostel,
                abilityId = soulAbilityId,
                costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(findPermanent("Grizzly Bears")!!)
                ),
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    private fun TestGame.exiledCard(cardName: String): EntityId =
        state.getExile(player1Id).single { name(it) == cardName }

    init {
        context("Hostile Hostel") {

            test("below three soul counters it just gains a counter and stays a tapped land") {
                val game = builder().build()
                val hostel = game.findPermanent("Hostile Hostel")!!

                game.activateSacrificingBears(hostel)

                game.soulCounters(hostel) shouldBe 1
                game.name(hostel) shouldBe "Hostile Hostel"
                game.state.getEntity(hostel)!!.has<TappedComponent>() shouldBe true
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
            }

            test("the third soul counter removes the counters, transforms it and untaps it") {
                val game = builder().build()
                val hostel = game.findPermanent("Hostile Hostel")!!
                game.seedSoulCounters(hostel, 2)

                game.activateSacrificingBears(hostel)

                game.name(hostel) shouldBe "Creeping Inn"
                game.soulCounters(hostel) shouldBe 0
                withClue("it untaps after transforming") {
                    game.state.getEntity(hostel)!!.has<TappedComponent>() shouldBe false
                }
            }

            test("attacking, X counts every creature card exiled with it, including the one just exiled") {
                val game = builder()
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInExile(1, "Wind Drake")
                    .withCardInExile(1, "Raging Goblin")
                    .build()
                val hostel = game.findPermanent("Hostile Hostel")!!
                // Wind Drake was exiled with it earlier; the Raging Goblin was exiled by something else.
                val drake = game.exiledCard("Wind Drake")
                game.state = game.state.updateEntity(hostel) { it.with(LinkedExileComponent(listOf(drake))) }
                game.seedSoulCounters(hostel, 2)
                game.activateSacrificingBears(hostel)
                game.name(hostel) shouldBe "Creeping Inn"

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Creeping Inn" to 2)).error shouldBe null
                game.resolveStack()

                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("any creature card in your graveyard may be exiled") {
                    decision.options shouldContainExactlyInAnyOrder listOf(giant, bears)
                }
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(hostel)!!.get<LinkedExileComponent>()!!.exiledIds shouldContain giant
                withClue("X = 2: Wind Drake and Hill Giant, not the unrelated Raging Goblin") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("declining the exile does nothing") {
                val game = builder().withCardInGraveyard(1, "Hill Giant").build()
                val hostel = game.findPermanent("Hostile Hostel")!!
                game.seedSoulCounters(hostel, 2)
                game.activateSacrificingBears(hostel)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Creeping Inn" to 2)).error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.findCardsInGraveyard(1, "Hill Giant").size shouldBe 1
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
