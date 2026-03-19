package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PhyrexianScripturesScenarioTest : ScenarioTestBase() {

    // Test card: returns any permanent card from graveyard to battlefield
    private val reanimatePermanent = card("Reanimate Permanent") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery"

        spell {
            val t = target(
                "permanent card in your graveyard",
                TargetObject(filter = TargetFilter(GameObjectFilter.Permanent, zone = Zone.GRAVEYARD))
            )
            effect = MoveToZoneEffect(t, Zone.BATTLEFIELD)
        }
    }

    init {
        cardRegistry.register(reanimatePermanent)
        context("Phyrexian Scriptures Saga") {

            test("Chapter I triggers on ETB: adds +1/+1 counter and makes creature artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Phyrexian Scriptures")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Phyrexian Scriptures
                game.castSpell(1, "Phyrexian Scriptures")
                game.resolveStack()

                // Phyrexian Scriptures should be on the battlefield with 1 lore counter
                val sagaId = game.findPermanent("Phyrexian Scriptures")
                sagaId shouldNotBe null
                val sagaEntity = game.state.getEntity(sagaId!!)!!
                sagaEntity.get<SagaComponent>() shouldNotBe null
                sagaEntity.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1

                // Chapter I should have triggered — we should have a decision for targeting
                // "up to one target creature"
                // The chapter trigger should be on the stack or pending
                // Auto-select the Grizzly Bears as target
                if (game.state.pendingDecision != null) {
                    val bearsId = game.findPermanent("Grizzly Bears")!!
                    game.selectTargets(listOf(bearsId))
                }

                // Resolve the chapter I ability
                game.resolveStack()

                // Grizzly Bears should now have a +1/+1 counter
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val bearsCounters = game.state.getEntity(bearsId)?.get<CountersComponent>()
                bearsCounters shouldNotBe null
                bearsCounters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

                // Grizzly Bears should be an artifact (via projected state)
                val projected = game.state.projectedState
                projected.hasType(bearsId, "ARTIFACT") shouldBe true
            }

            test("Chapter II triggers on next turn: destroys nonartifact creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Phyrexian Scriptures")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Phyrexian Scriptures — triggers Chapter I
                game.castSpell(1, "Phyrexian Scriptures")
                game.resolveStack()

                // Target Grizzly Bears with Chapter I
                if (game.state.pendingDecision != null) {
                    val bearsId = game.findPermanent("Grizzly Bears")!!
                    game.selectTargets(listOf(bearsId))
                }
                game.resolveStack()

                // Grizzly Bears is now an artifact creature
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasType(bearsId, "ARTIFACT") shouldBe true

                // Verify current state: lore = 1, chapter I triggered
                val sagaId = game.findPermanent("Phyrexian Scriptures")!!
                game.state.getEntity(sagaId)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1

                // Advance past current precombat main to postcombat main first
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Then advance to P2's precombat main
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Now we're on P2's turn
                game.state.activePlayerId shouldBe game.player2Id

                // Advance through P2's turn to P1's precombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Now we should be at P1's precombat main
                game.state.activePlayerId shouldBe game.player1Id

                // Now at P1's precombat main — lore counter should have been added (now 2)
                val sagaEntity = game.state.getEntity(sagaId)!!
                sagaEntity.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2

                // Chapter II should have triggered — resolve it
                game.resolveStack()

                // Hill Giant (nonartifact) should be destroyed
                game.isOnBattlefield("Hill Giant") shouldBe false

                // Grizzly Bears (now an artifact creature) should survive
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        context("Saga put onto battlefield without casting") {

            test("Saga entering battlefield via reanimate gets SagaComponent and lore counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reanimate Permanent")
                    .withCardInGraveyard(1, "Phyrexian Scriptures")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Reanimate Permanent targeting Phyrexian Scriptures in graveyard
                game.castSpellTargetingGraveyardCard(1, "Reanimate Permanent", 1, "Phyrexian Scriptures")
                game.resolveStack()

                // Phyrexian Scriptures should be on the battlefield
                val sagaId = game.findPermanent("Phyrexian Scriptures")
                sagaId shouldNotBe null

                // Should have SagaComponent and 1 lore counter (Rule 714.3a)
                val sagaEntity = game.state.getEntity(sagaId!!)!!
                sagaEntity.get<SagaComponent>() shouldNotBe null
                sagaEntity.get<SagaComponent>()!!.triggeredChapters shouldBe setOf(1)
                sagaEntity.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
            }
        }
    }
}
