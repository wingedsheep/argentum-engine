package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.PlayerLeftGameEvent
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameProcessor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.PlayerLeftGameComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phase 1.2 of `backlog/multiplayer.md` — "leaving the game" (CR 800.4a–c). When a player
 * loses in a multiplayer pod the game must continue for the others with everything the
 * leaver brought removed: their owned objects leave every zone, their stack objects vanish,
 * effects giving them control of an object end (so a stolen creature reverts to its owner),
 * and priority/turn-order skip them.
 */
class LeaveTheGameTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Leave Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry(): CardRegistry = CardRegistry().also { it.register(bear) }

    fun initGame(playerCount: Int): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { "Leave Test Bear" })
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..playerCount).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    /** Put a vanilla creature on the battlefield, owned by [owner] and controlled by [controller]. */
    fun GameState.withCreature(owner: EntityId, controller: EntityId = owner): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "Leave Test Bear",
                name = "Leave Test Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                typeLine = TypeLine.parse("Creature — Bear"),
                ownerId = owner
            ),
            ControllerComponent(controller)
        )
        val next = withEntity(id, container)
            .addToZone(ZoneKey(controller, Zone.BATTLEFIELD), id)
        return next to id
    }

    fun GameState.controlEffect(target: EntityId, newController: EntityId): GameState {
        val fx = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.CONTROL,
                modification = SerializableModification.ChangeController(newController),
                affectedEntities = setOf(target)
            ),
            duration = Duration.Permanent,
            sourceId = null,
            controllerId = newController,
            timestamp = timestamp
        )
        return copy(floatingEffects = floatingEffects + fx)
    }

    test("conceding in a four-player pod marks the player left and continues the game") {
        val (initial, players) = initGame(4)
        val processor = ActionProcessor(registry())

        val result = processor.process(initial, Concede(players[1])).result
        result.isSuccess.shouldBeTrue()
        val state = result.newState

        state.gameOver shouldBe false
        state.getEntity(players[1])?.has<PlayerLostComponent>() shouldBe true
        state.getEntity(players[1])?.has<PlayerLeftGameComponent>() shouldBe true
        state.activePlayers shouldContainExactly listOf(players[0], players[2], players[3])
        state.getOpponents(players[0]) shouldContainExactly listOf(players[2], players[3])
        result.events.filterIsInstance<PlayerLeftGameEvent>().single().playerId shouldBe players[1]
    }

    test("conceding in a two-player game still ends the game with the other player winning") {
        val (initial, players) = initGame(2)
        val processor = ActionProcessor(registry())

        val state = processor.process(initial, Concede(players[1])).result.newState
        state.gameOver shouldBe true
        state.winnerId shouldBe players[0]
    }

    test("the leaver's owned objects leave the game (CR 800.4a)") {
        val (base, players) = initGame(4)
        val (withCreature, creatureId) = base.withCreature(owner = players[1])
        // Sanity: the creature and the leaver's hand exist before they leave.
        withCreature.getEntity(creatureId) shouldNotBe null
        withCreature.getZone(players[1], Zone.HAND).isEmpty() shouldBe false

        val (afterLeave, _) = PlayerLeavesGameProcessor.process(
            withCreature, players[1], GameEndReason.CONCESSION
        ).let { it.newState to it.events }

        afterLeave.getEntity(creatureId).shouldBeNull()
        afterLeave.getBattlefield() shouldNotContain creatureId
        afterLeave.getZone(players[1], Zone.HAND) shouldContainExactly emptyList()
        afterLeave.getZone(players[1], Zone.LIBRARY) shouldContainExactly emptyList()
    }

    test("a creature the leaver stole reverts to its owner (control effect ends, CR 800.4a)") {
        val (base, players) = initGame(4)
        // players[0] owns the creature; players[1] controls it via a theft effect.
        val (withCreature, creatureId) = base.withCreature(owner = players[0], controller = players[0])
        val withTheft = withCreature.controlEffect(target = creatureId, newController = players[1])
        withTheft.projectedState.getController(creatureId) shouldBe players[1]

        val afterLeave = PlayerLeavesGameProcessor.process(
            withTheft, players[1], GameEndReason.CONCESSION
        ).newState

        // The creature is still in the game (owned by players[0]) and back under their control.
        afterLeave.getEntity(creatureId) shouldNotBe null
        afterLeave.floatingEffects.any {
            (it.effect.modification as? SerializableModification.ChangeController)?.newControllerId == players[1]
        }.shouldBe(false)
        afterLeave.projectedState.getController(creatureId) shouldBe players[0]
    }

    test("a creature the leaver owns but another player controls leaves the game (CR 800.4a)") {
        val (base, players) = initGame(4)
        // players[1] owns the creature; players[0] controls it via theft.
        val (withCreature, creatureId) = base.withCreature(owner = players[1], controller = players[1])
        val withTheft = withCreature.controlEffect(target = creatureId, newController = players[0])

        val afterLeave = PlayerLeavesGameProcessor.process(
            withTheft, players[1], GameEndReason.CONCESSION
        ).newState

        // Owned by the leaver, so it leaves the game even though players[0] controlled it.
        afterLeave.getEntity(creatureId).shouldBeNull()
    }

    test("the leaver's spells and abilities on the stack cease to exist (CR 800.4a)") {
        val (base, players) = initGame(4)
        // A triggered ability the leaver controls, on the stack, not represented by a card.
        val abilityId = EntityId.generate()
        val state = base
            .withEntity(
                abilityId,
                ComponentContainer.of(
                    TriggeredAbilityOnStackComponent(
                        sourceId = abilityId,
                        sourceName = "Leave Test Bear",
                        controllerId = players[1],
                        effect = com.wingedsheep.sdk.scripting.effects.DrawCardsEffect(1),
                        description = "Draw a card"
                    )
                )
            )
            .pushToStack(abilityId)

        val afterLeave = PlayerLeavesGameProcessor.process(
            state, players[1], GameEndReason.CONCESSION
        ).newState

        afterLeave.getEntity(abilityId).shouldBeNull()
        afterLeave.stack shouldNotContain abilityId
    }

    test("combat continues when the leaver's attacker is removed (its blocker stops blocking it)") {
        val (base, players) = initGame(4)
        // players[1] attacks with a creature; players[0] blocks it.
        val attackerId = EntityId.generate()
        val attacker = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "Leave Test Bear",
                name = "Leave Test Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                typeLine = TypeLine.parse("Creature — Bear"),
                ownerId = players[1]
            ),
            ControllerComponent(players[1]),
            AttackingComponent(defenderId = players[0])
        )
        val (withBlocker, blockerId) = base.withCreature(owner = players[0])
        val state = withBlocker
            .withEntity(attackerId, attacker)
            .addToZone(ZoneKey(players[1], Zone.BATTLEFIELD), attackerId)
            .updateEntity(blockerId) { it.with(BlockingComponent(listOf(attackerId))) }

        val afterLeave = PlayerLeavesGameProcessor.process(
            state, players[1], GameEndReason.CONCESSION
        ).newState

        afterLeave.getEntity(attackerId).shouldBeNull()
        // The blocker no longer references the departed attacker (combat can proceed).
        afterLeave.getEntity(blockerId)?.has<BlockingComponent>() shouldBe false
    }

    test("priority held by the leaver passes to the next player still in the game (CR 800.4a)") {
        val (base, players) = initGame(4)
        // Give players[1] priority, then mark them lost (priority is still on them at this point).
        val state = base.withPriority(players[1]).updateEntity(players[1]) {
            it.with(PlayerLostComponent(com.wingedsheep.engine.state.components.player.LossReason.CONCESSION))
        }
        state.priorityPlayerId shouldBe players[1]

        val afterLeave = PlayerLeavesGameProcessor.process(
            state, players[1], GameEndReason.CONCESSION
        ).newState
        afterLeave.priorityPlayerId shouldBe players[2]
    }

    test("the active player conceding hands the turn forward without stalling") {
        val (initial, players) = initGame(4)
        val processor = ActionProcessor(registry())

        // players[0] is the active player and concedes on their own turn.
        var state = processor.process(initial, Concede(players[0])).result.newState
        state.gameOver shouldBe false

        // Drive the game forward; it must reach players[1]'s turn without deadlocking on the
        // departed active player (CR 800.4j: their turn finishes with priority delegated).
        var safety = 0
        while (state.activePlayerId == players[0] && !state.gameOver) {
            check(++safety < 500) { "stuck at step ${state.step} with priority ${state.priorityPlayerId}" }
            val prio = state.priorityPlayerId ?: break
            // The departed player must never be handed priority.
            prio shouldNotBe players[0]
            val result = processor.process(
                state, com.wingedsheep.engine.core.PassPriority(prio)
            ).result
            check(result.isSuccess || result.isPaused) { "action failed: ${result.error}" }
            state = result.newState
        }
        state.activePlayerId shouldBe players[1]
    }
})
