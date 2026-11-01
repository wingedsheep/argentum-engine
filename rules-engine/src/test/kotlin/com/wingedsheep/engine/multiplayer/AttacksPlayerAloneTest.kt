package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * CR 506.6: "A creature attacks a player alone if it's declared as an attacker attacking a player
 * during the declare attackers step and no other creature is declared as an attacker attacking
 * that player." — [AttackPredicate.AloneAgainstPlayer], counted per defending player, unlike
 * CR 506.5's whole-combat [AttackPredicate.Alone].
 *
 * Three players so two attackers can each be alone against a different player. The two-player
 * planeswalker/battle split is covered by the FRA cards' scenario tests.
 */
class AttacksPlayerAloneTest : FunSpec({

    // "Whenever a creature you control attacks a player alone, you gain 1 life." — ANY binding,
    // so the predicate must be read per attacker, not off this (non-attacking) source.
    val watcher = card("Lone Watcher") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl())
                .attacks(setOf(AttackPredicate.AloneAgainstPlayer))
            effect = Effects.GainLife(1)
        }
    }
    // "Whenever this creature attacks a player alone, you gain 1 life." — SELF binding.
    val loner = card("Lone Duelist") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Samurai"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks(setOf(AttackPredicate.AloneAgainstPlayer))
            effect = Effects.GainLife(1)
        }
    }
    val bear = CardDefinition.creature(
        name = "Filler Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry() = CardRegistry().also {
        it.register(watcher); it.register(loner); it.register(bear)
    }

    fun GameState.withPermanent(def: CardDefinition, owner: EntityId): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = def.name,
                name = def.name,
                manaCost = def.manaCost,
                typeLine = def.typeLine,
                baseStats = def.creatureStats,
                ownerId = owner
            ),
            OwnerComponent(owner),
            ControllerComponent(owner)
        )
        return withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id) to id
    }

    class Board(val state: GameState, val players: List<EntityId>, val creatures: List<EntityId>)

    /** Player 1 (active) controls [first] plus two Filler Bears, and optionally the Lone Watcher. */
    fun setup(first: CardDefinition = bear, withWatcher: Boolean = true): Board {
        val deck = Deck(cards = List(40) { "Filler Bear" })
        val init = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..3).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        val players = init.playerIds
        var state = init.state
        val creatures = mutableListOf<EntityId>()
        for (def in listOf(first, bear, bear)) {
            val (s, id) = state.withPermanent(def, players[0])
            state = s
            creatures += id
        }
        if (withWatcher) state = state.withPermanent(watcher, players[0]).first
        state = state.copy(phase = Phase.COMBAT, step = Step.DECLARE_ATTACKERS).withPriority(players[0])
        return Board(state, players, creatures)
    }

    /**
     * Declare, then return who each trigger put on the stack is about: its triggering attacker
     * (ANY binding) or, for a SELF trigger, which carries none, its source.
     */
    fun Board.declare(attacks: Map<EntityId, EntityId>): Pair<List<EntityId?>, AttackersDeclaredEvent> {
        val result = ActionProcessor(registry()).process(state, DeclareAttackers(players[0], attacks)).result
        result.error shouldBe null
        val after = result.newState
        val triggered = after.stack.mapNotNull { after.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
        val event = result.events.filterIsInstance<AttackersDeclaredEvent>().single()
        return triggered.map { it.triggerContext?.triggeringEntityId ?: it.sourceId } to event
    }

    test("each creature alone against its own player attacks that player alone") {
        val b = setup()
        val (a, c) = b.creatures[0] to b.creatures[1]
        val (triggered, event) = b.declare(mapOf(a to b.players[1], c to b.players[2]))

        event.attackersAloneAgainstPlayer shouldBe setOf(a, c)
        triggered.shouldContainExactlyInAnyOrder(a, c)
    }

    test("two creatures attacking the same player — neither is alone; the third, elsewhere, is") {
        val b = setup()
        val (a, c, d) = b.creatures
        val (triggered, event) = b.declare(mapOf(a to b.players[1], c to b.players[1], d to b.players[2]))

        event.attackersAloneAgainstPlayer shouldBe setOf(d)
        triggered.shouldContainExactlyInAnyOrder(d)
    }

    test("all attackers on one player — no trigger") {
        val b = setup()
        val (a, c) = b.creatures[0] to b.creatures[1]
        val (triggered, event) = b.declare(mapOf(a to b.players[2], c to b.players[2]))

        event.attackersAloneAgainstPlayer.shouldBeEmpty()
        triggered.shouldBeEmpty()
    }

    test("a lone attacker in the whole combat attacks a player alone too") {
        val b = setup()
        val a = b.creatures[0]
        val (triggered, _) = b.declare(mapOf(a to b.players[1]))

        triggered.shouldContainExactlyInAnyOrder(a)
    }

    test("SELF binding: the creature triggers when it alone attacks its player, beside another attack") {
        val b = setup(first = loner, withWatcher = false)
        val (duelist, other) = b.creatures[0] to b.creatures[1]
        val (triggered, _) = b.declare(mapOf(duelist to b.players[1], other to b.players[2]))

        triggered.shouldContainExactlyInAnyOrder(duelist)
    }

    test("SELF binding: no trigger when another creature attacks the same player") {
        val b = setup(first = loner, withWatcher = false)
        val (duelist, other) = b.creatures[0] to b.creatures[1]
        val (triggered, _) = b.declare(mapOf(duelist to b.players[1], other to b.players[1]))

        triggered.shouldBeEmpty()
    }
})
