package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m21.cards.ChandraFlamesCatalyst
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chandra, Flame's Catalyst (M21 #332).
 *
 *   −2: You may cast target red instant or sorcery card from your graveyard. If that spell would be
 *       put into your graveyard, exile it instead.
 *   −8: Discard your hand, then draw seven cards. Until end of turn, you may cast spells from your
 *       hand without paying their mana costs.
 *
 * The −8 is the new vocabulary (a whole-turn free-cast permission scoped to the hand); the −2 pins
 * the resolution-time graveyard cast that pays its cost and is exiled afterwards.
 */
class ChandraFlamesCatalystScenarioTest : ScenarioTestBase() {

    init {
        context("the −8") {

            test("discards the hand, draws seven, then every spell from hand this turn is free") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flame's Catalyst")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flame's Catalyst")!!
                setLoyalty(game, chandra, 8)
                activate(game, chandra, index = 2)
                game.resolveStack()

                withClue("the hand was discarded and seven cards drawn") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.handSize(1) shouldBe 7
                }
                withClue("Chandra went to 0 loyalty and left, but the permission stays") {
                    game.findPermanent("Chandra, Flame's Catalyst") shouldBe null
                }

                // No lands and an empty pool: each Bears is free or it can't be cast.
                val bears = game.findCardsInHand(1, "Grizzly Bears")
                for (id in bears.take(3)) {
                    game.execute(
                        CastSpell(game.player1Id, id, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
                    ).error shouldBe null
                    game.resolveStack()
                }
                withClue("three creature spells were cast free from hand in one turn") {
                    game.state.getBattlefield().count { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" } shouldBe 3
                }
                withClue("the free cast is still on offer for the rest of the turn") {
                    game.getLegalActions(1).any { it.actionType == "CastWithoutPayingManaCost" } shouldBe true
                }
            }
        }

        context("the −2") {

            test("casts a red instant from the graveyard, paying its cost, then exiles it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Flame's Catalyst")
                    .withCardInGraveyard(1, "Pyretic Ritual")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Flame's Catalyst")!!
                val ritual = game.findCardsInGraveyard(1, "Pyretic Ritual").single()
                activate(game, chandra, index = 1, targets = listOf(ChosenTarget.Card(ritual, game.player1Id, Zone.GRAVEYARD)))
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Pyretic Ritual resolved: {R}{R}{R} in the pool") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 3
                }
                withClue("its {1}{R} was paid: both Mountains are tapped") {
                    game.findPermanents("Mountain").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                withClue("it was exiled instead of going back to the graveyard") {
                    game.isInExile(1, "Pyretic Ritual") shouldBe true
                    game.findCardsInGraveyard(1, "Pyretic Ritual").size shouldBe 0
                }
                withClue("−2 took Chandra from 5 to 3") { loyalty(game, chandra) shouldBe 3 }
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ChandraFlamesCatalyst.activatedAbilities[index].id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.LOYALTY to amount)))
        }
    }
}
