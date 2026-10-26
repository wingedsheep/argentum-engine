package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mechanic-level tests for a may-play permission's `entersTapped` rider
 * ([com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect.entersTapped]): a land played
 * or a permanent spell cast *through* the permission enters tapped when it matches the rider's
 * filter. The rider belongs to the permission the play goes through, so a rider-free route to the
 * same card (a plain grant, Crucible of Worlds) plays it untapped, and a card cast from hand never
 * uses the grant at all.
 */
class MayPlayEntersTappedRiderTest : ScenarioTestBase() {

    private val bear = card("Rider Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val escaper = card("Rider Test Escaper") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.escape("{1}", Costs.additional.ExileOtherCards(1)))
    }
    private val smash = CardDefinition.modalDoubleFacedLand(
        frontFace = card("Rider Test Smash") {
            manaCost = "{1}{R}"
            typeLine = "Sorcery"
            spell { effect = Effects.GainLife(1) }
        },
        backFace = card("Rider Test Pass") { typeLine = "Land" },
    )
    private val crucible = card("Rider Test Crucible") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility { ability = MayPlayLandsFromGraveyard }
    }

    private fun TestGame.graveyardCard(name: String): EntityId =
        state.getGraveyard(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.exiledCard(name: String): EntityId =
        state.getExile(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.handCard(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.grant(cardId: EntityId, entersTapped: GameObjectFilter?) {
        state = state.addMayPlayPermission(
            MayPlayPermission(
                id = EntityId.generate(),
                cardIds = setOf(cardId),
                controllerId = player1Id,
                entersTapped = entersTapped,
                timestamp = state.timestamp,
            )
        )
    }

    private fun TestGame.isTapped(name: String) =
        state.getEntity(findPermanent(name)!!)?.has<TappedComponent>() == true

    private fun TestGame.castAndResolve(cardId: EntityId) {
        execute(CastSpell(player1Id, cardId)).error shouldBe null
        resolveStack()
    }

    private fun board(
        graveyard: List<String> = emptyList(),
        exile: List<String> = emptyList(),
        hand: List<String> = emptyList(),
        battlefield: List<String> = emptyList(),
    ): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        graveyard.forEach { builder = builder.withCardInGraveyard(1, it) }
        exile.forEach { builder = builder.withCardInExile(1, it) }
        hand.forEach { builder = builder.withCardInHand(1, it) }
        battlefield.forEach { builder = builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    init {
        cardRegistry.register(listOf(bear, escaper, smash, crucible))

        context("a permanent spell cast through the permission") {
            test("enters tapped when the rider's filter matches it") {
                val game = board(graveyard = listOf(bear.name))
                game.grant(game.graveyardCard(bear.name), GameObjectFilter.Any)
                game.castAndResolve(game.graveyardCard(bear.name))
                game.isTapped(bear.name) shouldBe true
            }

            test("enters untapped when the rider names only lands") {
                val game = board(exile = listOf(bear.name))
                game.grant(game.exiledCard(bear.name), GameObjectFilter.Land)
                game.castAndResolve(game.exiledCard(bear.name))
                game.isTapped(bear.name) shouldBe false
            }

            test("enters untapped when a rider-free permission also covers the card") {
                val game = board(graveyard = listOf(bear.name))
                val bearId = game.graveyardCard(bear.name)
                game.grant(bearId, GameObjectFilter.Any)
                game.grant(bearId, null)
                game.castAndResolve(bearId)
                withClue("the player casts it through the plain permission") {
                    game.isTapped(bear.name) shouldBe false
                }
            }

            test("enters untapped when the player casts it by its own graveyard route instead") {
                val game = board(graveyard = listOf(escaper.name, bear.name))
                val escaperId = game.graveyardCard(escaper.name)
                game.grant(escaperId, GameObjectFilter.Any)
                game.execute(
                    CastSpell(
                        game.player1Id, escaperId,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.ESCAPE,
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(game.graveyardCard(bear.name))),
                    )
                ).error shouldBe null
                game.resolveStack()
                withClue("an escape cast never used the grant, so its rider doesn't bind") {
                    game.isTapped(escaper.name) shouldBe false
                }
            }

            test("cast from hand never goes through a grant on the card") {
                val game = board(hand = listOf(bear.name))
                game.grant(game.handCard(bear.name), GameObjectFilter.Any)
                game.castAndResolve(game.handCard(bear.name))
                game.isTapped(bear.name) shouldBe false
            }
        }

        context("a land played through the permission") {
            test("enters tapped from a graveyard") {
                val game = board(graveyard = listOf("Island"))
                val islandId = game.graveyardCard("Island")
                game.grant(islandId, GameObjectFilter.Any)
                game.execute(PlayLand(game.player1Id, islandId)).error shouldBe null
                game.isTapped("Island") shouldBe true
            }

            test("enters tapped from exile under a land rider") {
                val game = board(exile = listOf("Island"))
                val islandId = game.exiledCard("Island")
                game.grant(islandId, GameObjectFilter.Land)
                game.execute(PlayLand(game.player1Id, islandId)).error shouldBe null
                game.isTapped("Island") shouldBe true
            }

            test("a modal DFC played as its land back face is judged by that face") {
                val game = board(exile = listOf(smash.name))
                val smashId = game.exiledCard(smash.name)
                game.grant(smashId, GameObjectFilter.Land)
                game.execute(PlayLand(game.player1Id, smashId, asBackFace = true)).error shouldBe null
                withClue("the sorcery front isn't a land, but the land face played is") {
                    game.isTapped("Rider Test Pass") shouldBe true
                }
            }

            test("enters untapped when Crucible-style permission could play it instead") {
                val game = board(graveyard = listOf("Island"), battlefield = listOf(crucible.name))
                val islandId = game.graveyardCard("Island")
                game.grant(islandId, GameObjectFilter.Any)
                game.execute(PlayLand(game.player1Id, islandId)).error shouldBe null
                game.isTapped("Island") shouldBe false
            }
        }
    }
}
