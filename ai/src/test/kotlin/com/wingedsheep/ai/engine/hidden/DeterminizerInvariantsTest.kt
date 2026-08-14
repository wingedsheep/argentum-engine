package com.wingedsheep.ai.engine.hidden

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.model.GameRng
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DeterminizerInvariantsTest : ScenarioTestBase() {

    init {
        test("sampling preserves structure and everything visible to the viewer") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Forest")
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Forest")
                .build()
            val before = game.state
            val sampled = Determinizer(cardRegistry).sample(
                before,
                game.player1Id,
                OpponentModel.IdentityPermutation,
                GameRng.seeded(991L),
            )

            sampled.entities.keys shouldBe before.entities.keys
            sampled.zones.keys shouldBe before.zones.keys
            sampled.zones.forEach { (key, ids) ->
                ids.shouldContainExactlyInAnyOrder(before.zones.getValue(key))
            }

            val transformer = ClientStateTransformer(cardRegistry)
            val visibleBefore = transformer.transform(before, game.player1Id)
            val visibleAfter = transformer.transform(sampled, game.player1Id)
            visibleAfter.cards shouldBe visibleBefore.cards
            visibleAfter.zones.map { it.zoneId to it.size } shouldBe
                visibleBefore.zones.map { it.zoneId to it.size }
        }

        test("the same seed samples the same world and a different seed can change it") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Craw Wurm")
                .build()
            val determinizer = Determinizer(cardRegistry)

            val first = determinizer.sample(
                game.state, game.player1Id, OpponentModel.IdentityPermutation, GameRng.seeded(7L)
            )
            val replay = determinizer.sample(
                game.state, game.player1Id, OpponentModel.IdentityPermutation, GameRng.seeded(7L)
            )
            first shouldBe replay
            val worlds = (7L..22L).map { seed ->
                hiddenNames(
                    determinizer.sample(
                        game.state,
                        game.player1Id,
                        OpponentModel.IdentityPermutation,
                        GameRng.seeded(seed),
                    ),
                    game.player2Id,
                )
            }
            worlds.distinct().size shouldNotBe 1
        }

        test("an individually revealed hand card is pinned") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears")
                .build()
            val revealedId = game.state.getHand(game.player2Id).first()
            val before = game.state.updateEntity(revealedId) {
                it.with(RevealedToComponent.to(game.player1Id))
            }

            val sampled = Determinizer(cardRegistry).sample(
                before,
                game.player1Id,
                OpponentModel.IdentityPermutation,
                GameRng.seeded(4L),
            )

            sampled.getEntity(revealedId)?.get<CardComponent>() shouldBe
                before.getEntity(revealedId)?.get<CardComponent>()
        }

        test("known decklists subtract visible cards before sampling the hidden remainder") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Forest")
                .withCardInHand(2, "Mountain")
                .withCardInLibrary(2, "Grizzly Bears")
                .build()

            val sampled = Determinizer(cardRegistry).sample(
                game.state,
                game.player1Id,
                OpponentModel.KnownDecklist(
                    mapOf("Forest" to 1, "Mountain" to 1, "Grizzly Bears" to 1)
                ),
                GameRng.seeded(11L),
            )

            hiddenNames(sampled, game.player2Id).sorted() shouldBe
                listOf("Grizzly Bears", "Mountain")
        }

        test("a publicly revealed top card stays pinned in the top library slot") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Goblin Spy")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears")
                .build()
            val topId = game.state.getLibrary(game.player2Id).first()
            val topCard = game.state.getEntity(topId)!!.require<CardComponent>()

            val sampled = Determinizer(cardRegistry).sample(
                game.state,
                game.player1Id,
                OpponentModel.IdentityPermutation,
                GameRng.seeded(81L),
            )

            sampled.getLibrary(game.player2Id).first() shouldBe topId
            sampled.getEntity(topId)!!.require<CardComponent>() shouldBe topCard
        }

        test("the viewer does not retain knowledge of their own library order") {
            val game = scenario()
                .withPlayers()
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Grizzly Bears")
                .build()
            val originalOrder = game.state.getLibrary(game.player1Id)

            val sampledOrders = (1L..16L).map { seed ->
                Determinizer(cardRegistry).sample(
                    game.state,
                    game.player1Id,
                    OpponentModel.IdentityPermutation,
                    GameRng.seeded(seed),
                ).getLibrary(game.player1Id)
            }

            sampledOrders.distinct().size shouldNotBe 1
            sampledOrders.first().shouldContainExactlyInAnyOrder(originalOrder)
        }
    }

    private fun hiddenNames(
        state: com.wingedsheep.engine.state.GameState,
        opponentId: com.wingedsheep.sdk.model.EntityId,
    ): List<String> = (state.getHand(opponentId) + state.getLibrary(opponentId)).map {
        state.getEntity(it)!!.require<CardComponent>().name
    }
}
