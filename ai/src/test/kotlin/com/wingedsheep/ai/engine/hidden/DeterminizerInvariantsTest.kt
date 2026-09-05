package com.wingedsheep.ai.engine.hidden

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.hidden.HiddenSlotRewrite
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DeterminizerInvariantsTest : ScenarioTestBase() {

    init {
        test("sampling retains the pre-batching assignment and library sequence") {
            val game = scenario().withPlayers().withRngSeed(883L)
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Mountain").withCardInLibrary(1, "Hill Giant")
                .withCardInHand(2, "Mountain").withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Craw Wurm")
                .build()
            val revealed = game.state.getHand(game.player2Id).first()
            val before = game.state.updateEntity(revealed) { it.with(RevealedToComponent.to(game.player1Id)) }
            val originalEntities = before.entities.toMap()
            val originalZones = before.zones.mapValues { it.value.toList() }
            val models = listOf(
                emptyMap<EntityId, OpponentModel>(),
                mapOf(
                    game.player1Id to OpponentModel.KnownDecklist(
                        mapOf("Forest" to 1, "Mountain" to 1, "Hill Giant" to 1),
                    ),
                    game.player2Id to OpponentModel.KnownDecklist(
                        mapOf("Mountain" to 1, "Hill Giant" to 1, "Grizzly Bears" to 1, "Craw Wurm" to 1),
                    ),
                ),
            )
            val sampler = Determinizer(cardRegistry)
            val signature = buildString {
                for (model in models) for (viewer in before.turnOrder) for (seed in 1L..16L) {
                    val world = sampler.sample(before, viewer, model, GameRng.seeded(seed))
                    world.copy(entities = before.entities, zones = before.zones) shouldBe before
                    before.entities shouldBe originalEntities
                    before.zones shouldBe originalZones
                    world.getEntity(revealed) shouldBe before.getEntity(revealed)
                    append(viewer.value).append('/').append(seed).append(':')
                    world.entities.forEach { (id, components) ->
                        append(id.value).append('=').append(components.get<CardComponent>()?.name).append(';')
                    }
                    append(world.zones).append('\n')
                    val retainedEntities = world.entities.toMap()
                    sampler.sample(world, viewer, model, GameRng.seeded(seed + 100))
                    world.entities shouldBe retainedEntities
                }
            }
            // Captured from the per-card-copy implementation at upstream 12589ae4a2.
            java.security.MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) } shouldBe
                "fe26af39d1fb97c30edbebf726115360a34a390af7d4c15e9c00a80b3ca8620b"
        }

        test("library membership lookup retains duplicate occurrences and repeated player traversal") {
            val game = scenario().withPlayers()
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Hill Giant")
                .build()
            val libraryKey = ZoneKey(game.player2Id, Zone.LIBRARY)
            val library = game.state.getLibrary(game.player2Id)
            val duplicated = library + library.first()
            val source = game.state.copy(
                zones = game.state.zones + (libraryKey to duplicated),
                turnOrder = game.state.turnOrder + game.player2Id,
            )
            val sampler = Determinizer(cardRegistry)
            for (seed in 1L..16L) {
                val world = sampler.sample(source, game.player1Id, OpponentModel.IdentityPermutation, GameRng.seeded(seed))
                world.getLibrary(game.player2Id).shouldContainExactlyInAnyOrder(duplicated)
                world.entities.keys.toList() shouldBe source.entities.keys.toList()
                source.getLibrary(game.player2Id) shouldBe duplicated
            }
        }

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

        test("a Mind Rot pause pins referenced hidden hand slots while unrelated library slots remain sampleable") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Mind Rot")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Swamp")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Craw Wurm")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Mountain")
                .build()
            game.castSpellTargetingPlayer(1, "Mind Rot", 2).error shouldBe null
            game.resolveStack()

            val source = game.state
            val decision = source.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val referencedHandId = decision.options.first()
            val referencedHandCard = source.getEntity(referencedHandId)!!.require<CardComponent>()
            val unrelatedLibraryId = source.getLibrary(game.player2Id).first()
            val originalLibraryCard = source.getEntity(unrelatedLibraryId)!!.require<CardComponent>()
            val determinizer = Determinizer(cardRegistry)

            val sampled = (1L..32L).map { seed ->
                determinizer.sample(
                    source,
                    game.player1Id,
                    OpponentModel.IdentityPermutation,
                    GameRng.seeded(seed),
                )
            }

            sampled.forEach {
                it.getEntity(referencedHandId)!!.require<CardComponent>() shouldBe referencedHandCard
            }
            sampled.any {
                it.getEntity(unrelatedLibraryId)!!.require<CardComponent>() != originalLibraryCard
            } shouldBe true
        }

        test("a live Monstrous Emergence keeps its chosen hidden hand card fixed while other slots sample") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Monstrous Emergence")
                .withCardInHand(2, "Craw Wurm")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Mountain")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val spellId = game.state.getHand(game.player2Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Monstrous Emergence"
            }
            val chosenHandId = game.state.getHand(game.player2Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Craw Wurm"
            }
            val targetId = game.findPermanent("Grizzly Bears")!!

            game.execute(
                CastSpell(
                    game.player2Id,
                    spellId,
                    listOf(ChosenTarget.Permanent(targetId)),
                    additionalCostPayment = AdditionalCostPayment(beheldCards = listOf(chosenHandId)),
                ),
            ).error shouldBe null
            val source = game.state
            val chosenHandCard = source.getEntity(chosenHandId)!!.require<CardComponent>()
            val unrelatedLibraryId = source.getLibrary(game.player2Id).first()
            val originalLibraryCard = source.getEntity(unrelatedLibraryId)!!.require<CardComponent>()

            val sampled = (1L..32L).map { seed ->
                Determinizer(cardRegistry).sample(
                    source,
                    game.player1Id,
                    OpponentModel.IdentityPermutation,
                    GameRng.seeded(seed),
                )
            }

            sampled.forEach {
                it.getEntity(chosenHandId)!!.require<CardComponent>() shouldBe chosenHandCard
            }
            sampled.any {
                it.getEntity(unrelatedLibraryId)!!.require<CardComponent>() != originalLibraryCard
            } shouldBe true
        }

        test("an incomplete shared pin analysis leaves every hidden candidate identity unchanged") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Mountain")
                .build()
            val source = game.state
            val candidateIds = source.getHand(game.player2Id) + source.getLibrary(game.player2Id)
            val candidateIdentities = candidateIds.associateWith {
                source.getEntity(it)!!.require<CardComponent>()
            }
            val determinizer = Determinizer(cardRegistry, Visibility(cardRegistry)) {
                HiddenSlotRewrite.IdentitySensitiveInFlightPins.Incomplete("forced for test")
            }

            val sampled = determinizer.sample(
                source,
                game.player1Id,
                OpponentModel.IdentityPermutation,
                GameRng.seeded(913L),
            )

            candidateIdentities.forEach { (entityId, identity) ->
                sampled.getEntity(entityId)!!.require<CardComponent>() shouldBe identity
            }
            sampled shouldBe source
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
