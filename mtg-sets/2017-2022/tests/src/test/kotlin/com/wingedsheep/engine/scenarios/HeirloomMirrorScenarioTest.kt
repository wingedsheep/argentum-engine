package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Heirloom Mirror // Inherited Fiend (MID).
 *
 * Pins the ritual-counter countdown (draw, mill, add a counter; at three remove them all and
 * transform, staying tapped) and the Fiend's exile-and-grow ability.
 */
class HeirloomMirrorScenarioTest : ScenarioTestBase() {

    private val mirrorAbilityId
        get() = cardRegistry.getCard("Heirloom Mirror")!!.script.activatedAbilities[0].id

    private val fiendAbilityId
        get() = cardRegistry.getCard("Inherited Fiend")!!.script.activatedAbilities[0].id

    private fun builder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Heirloom Mirror")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Island")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.name(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    private fun TestGame.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun TestGame.activateMirror(mirror: EntityId) {
        val bears = findCardsInHand(1, "Grizzly Bears").single()
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = mirror,
                abilityId = mirrorAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(bears)),
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        context("Heirloom Mirror") {

            test("draws, mills and adds a ritual counter, paying 1 life and a discard") {
                val game = builder().build()
                val mirror = game.findPermanent("Heirloom Mirror")!!
                val handBefore = game.handSize(1)

                game.activateMirror(mirror)

                game.getLifeTotal(1) shouldBe 19
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("discarded one, drew one") { game.handSize(1) shouldBe handBefore }
                withClue("one card drawn, the other milled") {
                    game.state.getGraveyard(game.player1Id).size shouldBe 2
                    game.state.getLibrary(game.player1Id).size shouldBe 0
                }
                game.counters(mirror, CounterType.RITUAL) shouldBe 1
                game.name(mirror) shouldBe "Heirloom Mirror"
            }

            test("the third ritual counter removes them and transforms it, still tapped") {
                val game = builder().build()
                val mirror = game.findPermanent("Heirloom Mirror")!!
                game.state = game.state.updateEntity(mirror) {
                    it.with(CountersComponent(mapOf(CounterType.RITUAL to 2)))
                }

                game.activateMirror(mirror)

                game.name(mirror) shouldBe "Inherited Fiend"
                game.counters(mirror, CounterType.RITUAL) shouldBe 0
                withClue("nothing untaps it") {
                    game.state.getEntity(mirror)!!.has<TappedComponent>() shouldBe true
                }
            }
        }

        context("Inherited Fiend") {
            test("exiles a creature card from any graveyard and puts a +1/+1 counter on itself") {
                val game = builder().withCardInGraveyard(2, "Hill Giant").build()
                val mirror = game.findPermanent("Heirloom Mirror")!!
                game.state = game.state.updateEntity(mirror) {
                    it.with(CountersComponent(mapOf(CounterType.RITUAL to 2)))
                }
                game.activateMirror(mirror)
                game.name(mirror) shouldBe "Inherited Fiend"

                val giant = game.findCardsInGraveyard(2, "Hill Giant").single()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = mirror,
                        abilityId = fiendAbilityId,
                        targets = listOf(ChosenTarget.Card(giant, game.player2Id, Zone.GRAVEYARD)),
                    )
                )
                withClue("Fiend activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.state.getExile(game.player2Id).contains(giant) shouldBe true
                game.counters(mirror, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }
    }
}
