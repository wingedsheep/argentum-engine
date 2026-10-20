package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe

/**
 * `EventPattern.LandPlayedEvent.fromZone` — "whenever you / a player play(s) a land **from** zone X"
 * (Pia Nalaar, Consul of Revival; Rocco, Street Chef). Only the land-play special action (CR 305.1)
 * counts, and only when the land was played from that exact zone.
 */
class LandPlayedFromZoneTriggerTest : ScenarioTestBase() {

    private val youWatcher = card("Test Exile Land Watcher") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Whenever you play a land from exile, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.you.playsLandFrom(Zone.EXILE)
            effect = Effects.GainLife(1)
        }
    }

    private val anyWatcher = card("Test Any Exile Land Watcher") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Whenever a player plays a land from exile, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.anyPlayer.playsLandFrom(Zone.EXILE)
            effect = Effects.GainLife(1)
        }
    }

    private fun TestGame.grantPlay(card: EntityId, to: EntityId) {
        val (id, withId) = state.newEntity()
        state = withId.addMayPlayPermission(MayPlayPermission(id, setOf(card), to, timestamp = withId.timestamp))
    }

    init {
        cardRegistry.register(youWatcher)
        cardRegistry.register(anyWatcher)

        test("playing a land from exile triggers") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Exile Land Watcher")
                .withCardInExile(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = g.state.getExile(g.player1Id).single()
            g.grantPlay(forest, g.player1Id)

            g.execute(PlayLand(g.player1Id, forest)).error shouldBe null
            g.resolveStack()

            g.getLifeTotal(1) shouldBe 21
        }

        test("playing a land from hand does not trigger") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Exile Land Watcher")
                .withCardInHand(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            g.execute(PlayLand(g.player1Id, g.state.getHand(g.player1Id).single())).error shouldBe null
            g.resolveStack()

            g.getLifeTotal(1) shouldBe 20
        }

        test("playing a land from the graveyard does not trigger — the zone must match exactly") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Exile Land Watcher")
                .withCardInGraveyard(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = g.state.getGraveyard(g.player1Id).single()
            g.grantPlay(forest, g.player1Id)

            g.execute(PlayLand(g.player1Id, forest)).error shouldBe null
            g.resolveStack()

            g.getLifeTotal(1) shouldBe 20
        }

        test("an effect putting a land onto the battlefield from exile is not a land play") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Exile Land Watcher")
                .withCardInExile(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = g.state.getExile(g.player1Id).single()
            val result = services.effectExecutorRegistry.execute(
                g.state,
                Effects.PutOntoBattlefield(com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(forest)),
                com.wingedsheep.engine.handlers.EffectContext(sourceId = null, controllerId = g.player1Id)
            )
            result.error shouldBe null
            (result.events.filterIsInstance<com.wingedsheep.engine.core.LandPlayedEvent>()) shouldBe emptyList()
        }

        test("the you-scoped trigger ignores an opponent's exile land play; the any-player one sees it") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Exile Land Watcher")
                .withCardOnBattlefield(1, "Test Any Exile Land Watcher")
                .withCardInExile(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = g.state.getExile(g.player2Id).single()
            g.grantPlay(forest, g.player2Id)

            g.execute(PlayLand(g.player2Id, forest)).error shouldBe null
            g.resolveStack()

            g.getLifeTotal(1) shouldBe 21
        }

        test("fromZone and fromZoneOtherThan are mutually exclusive") {
            shouldThrow<IllegalArgumentException> {
                EventPattern.LandPlayedEvent(fromZoneOtherThan = Zone.HAND, fromZone = Zone.EXILE)
            }
            EventPattern.LandPlayedEvent(fromZone = Zone.EXILE).description shouldBe "you play a land from exile"
        }
    }
}
