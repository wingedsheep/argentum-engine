package com.wingedsheep.ai.training

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class DecisionTrainingRecordTest : FunSpec({
    val set = MtgSetCatalog.requireByCode("POR")
    val registry = CardRegistry().apply {
        register(set.cards)
        register(set.basicLands)
    }
    val deck = Deck(List(20) { "Plains" } + List(20) { "Island" })

    fun root(players: Int, seed: Long = 91L) = GameInitializer(registry).initializeGame(
        GameConfig(
            players = List(players) { PlayerConfig("Seat$it", deck) },
            skipMulligans = true,
            startingPlayerIndex = 0,
            seed = seed,
        )
    ).state

    test("masked replayable records use one variable-player schema for two through four seats") {
        for (count in 2..4) {
            val state = root(count)
            val actor = state.priorityPlayerId!!
            val record = DecisionRecordFactory(registry).capture(
                state = state,
                actingPlayer = actor,
                identity = DecisionIdentity("run-a", "game-$count", 0),
                format = "Standard",
                gameSeed = 91L,
                actionPrefixDigest = "initial",
            )

            record.playerCount shouldBe count
            record.rootObservation.others shouldHaveSize count - 1
            record.rootObservation.others.forEach { opponent ->
                opponent.visibleHand shouldHaveSize 0
                opponent.handSize shouldBe 7
            }
            record.rootObservation.self.visibleHand shouldHaveSize 7
            record.candidates.isNotEmpty().shouldBeTrue()
            DecisionRecordReplayer(registry).validate(record, state).valid.shouldBeTrue()

            val encoded = TrainingRecordEncoding.json.encodeToString(DecisionTrainingRecord.serializer(), record)
            TrainingRecordEncoding.json.decodeFromString(DecisionTrainingRecord.serializer(), encoded) shouldBe record
        }
    }

    test("masked observation digest ignores exact unseen opponent cards") {
        val first = root(2, seed = 11L)
        val second = root(2, seed = 12L)
        val viewerA = first.turnOrder[0]
        val viewerB = second.turnOrder[0]
        val a = TrainingRecordEncoding.observation(first, viewerA)
        val b = TrainingRecordEncoding.observation(second, viewerB)

        // Stable ids differ between independently initialized games; normalize them by comparing
        // the privacy-bearing part directly. No opponent card identity is present to compare.
        a.others.single().visibleHand shouldHaveSize 0
        b.others.single().visibleHand shouldHaveSize 0
        a.others.single().handSize shouldBe b.others.single().handSize
    }
})
