package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtCombatDamageToPlayerComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [AffectsFilterResolver.matchesStatePredicateForProjection].
 *
 * Each test constructs a minimal battlefield, runs the filter via [AffectsFilter.Generic] with a
 * single state predicate, and asserts which entities match. Combinators (Or/And/Not) get dedicated
 * tests plus a regression test for HasCounter with the "+1/+1" / "-1/-1" shorthands.
 */
class AffectsFilterResolverStatePredicateTest : FunSpec({

    val resolver = AffectsFilterResolver()
    val playerA = EntityId.generate()
    val playerB = EntityId.generate()

    fun creature(
        owner: EntityId,
        name: String = "Test Creature",
        power: Int = 2,
        toughness: Int = 2,
        subtypes: Set<Subtype> = emptySet()
    ): CardComponent = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE), subtypes = subtypes),
        ownerId = owner,
        baseStats = CreatureStats(power, toughness)
    )

    fun equipmentCard(owner: EntityId, name: String = "Test Equipment"): CardComponent = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(
            cardTypes = setOf(CardType.ARTIFACT),
            subtypes = setOf(Subtype.EQUIPMENT)
        ),
        ownerId = owner
    )

    fun auraCard(owner: EntityId, name: String = "Test Aura"): CardComponent = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost(emptyList()),
        typeLine = TypeLine(
            cardTypes = setOf(CardType.ENCHANTMENT),
            subtypes = setOf(Subtype.AURA)
        ),
        ownerId = owner
    )

    /**
     * Build a battlefield containing [entities], each paired with its ComponentContainer.
     * Also adds both players as entities (so controller lookups succeed).
     */
    fun battlefield(entities: List<Pair<EntityId, ComponentContainer>>): GameState {
        var state = GameState()
            .withEntity(playerA, ComponentContainer())
            .withEntity(playerB, ComponentContainer())
        entities.forEach { (id, container) ->
            state = state.withEntity(id, container)
            val controller = container.get<ControllerComponent>()?.playerId ?: playerA
            state = state.addToZone(ZoneKey(controller, Zone.BATTLEFIELD), id)
        }
        return state
    }

    /** Build a battlefield ComponentContainer owned+controlled by [controller]. */
    fun container(
        controller: EntityId,
        card: CardComponent,
        vararg extras: Component
    ): ComponentContainer {
        var c = ComponentContainer()
            .with(card)
            .with(OwnerComponent(controller))
            .with(ControllerComponent(controller))
        extras.forEach { component ->
            @Suppress("UNCHECKED_CAST")
            c = c.copy(components = c.components + (component::class.qualifiedName!! to component))
        }
        return c
    }

    fun filterWith(predicate: StatePredicate): AffectsFilter =
        AffectsFilter.Generic(GroupFilter(GameObjectFilter(statePredicates = listOf(predicate))))

    // =========================================================================
    // Tap state
    // =========================================================================

    test("IsTapped matches only tapped permanents") {
        val tapped = EntityId.generate()
        val untapped = EntityId.generate()
        val state = battlefield(
            listOf(
                tapped to container(playerA, creature(playerA), TappedComponent),
                untapped to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, tapped, filterWith(StatePredicate.IsTapped))
        matched shouldContainExactlyInAnyOrder setOf(tapped)
    }

    test("IsUntapped matches only untapped permanents") {
        val tapped = EntityId.generate()
        val untapped = EntityId.generate()
        val state = battlefield(
            listOf(
                tapped to container(playerA, creature(playerA), TappedComponent),
                untapped to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, tapped, filterWith(StatePredicate.IsUntapped))
        matched shouldContainExactlyInAnyOrder setOf(untapped)
    }

    // =========================================================================
    // Combat predicates
    // =========================================================================

    test("IsAttacking matches only creatures with AttackingComponent") {
        val attacker = EntityId.generate()
        val bystander = EntityId.generate()
        val state = battlefield(
            listOf(
                attacker to container(playerA, creature(playerA), AttackingComponent(playerB)),
                bystander to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, attacker, filterWith(StatePredicate.IsAttacking))
        matched shouldContainExactlyInAnyOrder setOf(attacker)
    }

    test("IsBlocking matches only creatures with BlockingComponent") {
        val attacker = EntityId.generate()
        val blocker = EntityId.generate()
        val state = battlefield(
            listOf(
                attacker to container(playerA, creature(playerA), AttackingComponent(playerB)),
                blocker to container(playerB, creature(playerB), BlockingComponent(listOf(attacker)))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, blocker, filterWith(StatePredicate.IsBlocking))
        matched shouldContainExactlyInAnyOrder setOf(blocker)
    }

    test("IsBlocked matches attackers that have at least one declared blocker") {
        val blockedAttacker = EntityId.generate()
        val unblockedAttacker = EntityId.generate()
        val blocker = EntityId.generate()
        val state = battlefield(
            listOf(
                blockedAttacker to container(playerA, creature(playerA), AttackingComponent(playerB)),
                unblockedAttacker to container(playerA, creature(playerA), AttackingComponent(playerB)),
                blocker to container(playerB, creature(playerB), BlockingComponent(listOf(blockedAttacker)))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, blockedAttacker, filterWith(StatePredicate.IsBlocked))
        matched shouldContainExactlyInAnyOrder setOf(blockedAttacker)
    }

    test("IsUnblocked matches attackers with no blockers and excludes non-attackers") {
        val blockedAttacker = EntityId.generate()
        val unblockedAttacker = EntityId.generate()
        val notAttacking = EntityId.generate()
        val blocker = EntityId.generate()
        val state = battlefield(
            listOf(
                blockedAttacker to container(playerA, creature(playerA), AttackingComponent(playerB)),
                unblockedAttacker to container(playerA, creature(playerA), AttackingComponent(playerB)),
                notAttacking to container(playerA, creature(playerA)),
                blocker to container(playerB, creature(playerB), BlockingComponent(listOf(blockedAttacker)))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, unblockedAttacker, filterWith(StatePredicate.IsUnblocked))
        matched shouldContainExactlyInAnyOrder setOf(unblockedAttacker)
    }

    // =========================================================================
    // Board history predicates
    // =========================================================================

    test("EnteredThisTurn matches only entities with EnteredThisTurnComponent") {
        val fresh = EntityId.generate()
        val old = EntityId.generate()
        val state = battlefield(
            listOf(
                fresh to container(playerA, creature(playerA), EnteredThisTurnComponent),
                old to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, fresh, filterWith(StatePredicate.EnteredThisTurn))
        matched shouldContainExactlyInAnyOrder setOf(fresh)
    }

    test("WasDealtDamageThisTurn matches only damaged creatures") {
        val damaged = EntityId.generate()
        val healthy = EntityId.generate()
        val state = battlefield(
            listOf(
                damaged to container(playerA, creature(playerA), WasDealtDamageThisTurnComponent),
                healthy to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, damaged, filterWith(StatePredicate.WasDealtDamageThisTurn))
        matched shouldContainExactlyInAnyOrder setOf(damaged)
    }

    test("HasDealtDamage matches only creatures with HasDealtDamageComponent") {
        val attacker = EntityId.generate()
        val passive = EntityId.generate()
        val state = battlefield(
            listOf(
                attacker to container(playerA, creature(playerA), HasDealtDamageComponent),
                passive to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, attacker, filterWith(StatePredicate.HasDealtDamage))
        matched shouldContainExactlyInAnyOrder setOf(attacker)
    }

    test("HasDealtCombatDamageToPlayer matches only creatures that dealt combat damage to a player") {
        val connected = EntityId.generate()
        val other = EntityId.generate()
        val state = battlefield(
            listOf(
                connected to container(playerA, creature(playerA), HasDealtCombatDamageToPlayerComponent),
                other to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, connected, filterWith(StatePredicate.HasDealtCombatDamageToPlayer))
        matched shouldContainExactlyInAnyOrder setOf(connected)
    }

    // =========================================================================
    // Face-down / morph
    // =========================================================================

    test("IsFaceDown matches only face-down permanents") {
        val morphed = EntityId.generate()
        val normal = EntityId.generate()
        val state = battlefield(
            listOf(
                morphed to container(playerA, creature(playerA), FaceDownComponent),
                normal to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, morphed, filterWith(StatePredicate.IsFaceDown))
        matched shouldContainExactlyInAnyOrder setOf(morphed)
    }

    test("IsFaceUp matches only face-up permanents") {
        val morphed = EntityId.generate()
        val normal = EntityId.generate()
        val state = battlefield(
            listOf(
                morphed to container(playerA, creature(playerA), FaceDownComponent),
                normal to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, normal, filterWith(StatePredicate.IsFaceUp))
        matched shouldContainExactlyInAnyOrder setOf(normal)
    }

    test("HasMorphAbility matches creatures flagged with morph") {
        val morphCapable = EntityId.generate()
        val vanilla = EntityId.generate()
        val state = battlefield(
            listOf(
                morphCapable to container(playerA, creature(playerA), HasMorphAbilityComponent),
                vanilla to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, morphCapable, filterWith(StatePredicate.HasMorphAbility))
        matched shouldContainExactlyInAnyOrder setOf(morphCapable)
    }

    // =========================================================================
    // Counter predicates
    // =========================================================================

    test("HasAnyCounter matches any creature with at least one counter of any type") {
        val loyaltyOnly = EntityId.generate()
        val p1p1 = EntityId.generate()
        val empty = EntityId.generate()
        val emptyZeroEntry = EntityId.generate()
        val state = battlefield(
            listOf(
                loyaltyOnly to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.LOYALTY to 3))
                ),
                p1p1 to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                ),
                empty to container(playerA, creature(playerA)),
                emptyZeroEntry to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.CHARGE to 0))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(state, loyaltyOnly, filterWith(StatePredicate.HasAnyCounter))
        matched shouldContainExactlyInAnyOrder setOf(loyaltyOnly, p1p1)
    }

    test("HasCounter(LOYALTY) matches only creatures with that specific counter type") {
        val withLoyalty = EntityId.generate()
        val withP1P1 = EntityId.generate()
        val state = battlefield(
            listOf(
                withLoyalty to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.LOYALTY to 2))
                ),
                withP1P1 to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(state, withLoyalty, filterWith(StatePredicate.HasCounter("LOYALTY")))
        matched shouldContainExactlyInAnyOrder setOf(withLoyalty)
    }

    test("HasCounter(\"+1/+1\") matches creatures with +1/+1 counters — shorthand form") {
        val withP1P1 = EntityId.generate()
        val withLoyalty = EntityId.generate()
        val state = battlefield(
            listOf(
                withP1P1 to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                ),
                withLoyalty to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.LOYALTY to 3))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(state, withP1P1, filterWith(StatePredicate.HasCounter("+1/+1")))
        matched shouldContainExactlyInAnyOrder setOf(withP1P1)
    }

    test("HasCounter(\"-1/-1\") matches creatures with -1/-1 counters — shorthand form") {
        val withM1M1 = EntityId.generate()
        val withP1P1 = EntityId.generate()
        val state = battlefield(
            listOf(
                withM1M1 to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1))
                ),
                withP1P1 to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(state, withM1M1, filterWith(StatePredicate.HasCounter("-1/-1")))
        matched shouldContainExactlyInAnyOrder setOf(withM1M1)
    }

    test("HasCounter with unknown counter type string matches nothing (no accidental fallback)") {
        val withP1P1 = EntityId.generate()
        val state = battlefield(
            listOf(
                withP1P1 to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(state, withP1P1, filterWith(StatePredicate.HasCounter("NOT_A_REAL_COUNTER_TYPE_XYZ")))
        matched shouldBe emptySet()
    }

    // =========================================================================
    // HasGreatestPower
    // =========================================================================

    test("HasGreatestPower matches the highest-power creature the source's controller controls") {
        val smallYours = EntityId.generate()
        val bigYours = EntityId.generate()
        val opponentsEvenBigger = EntityId.generate()
        val state = battlefield(
            listOf(
                smallYours to container(playerA, creature(playerA, power = 2)),
                bigYours to container(playerA, creature(playerA, power = 5)),
                // Opponent's creature is bigger than any of yours but shouldn't factor
                // into the scope because HasGreatestPower is scoped per controller.
                opponentsEvenBigger to container(playerB, creature(playerB, power = 9))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, bigYours, filterWith(StatePredicate.HasGreatestPower))
        // `matched` contains every battlefield entity evaluated against the source's controller
        // (playerA), so opponent creatures won't match by filter — but the per-entity predicate
        // still uses that entity's own controller. Assert the two you-control outcomes are right.
        matched shouldContain bigYours
        matched shouldNotContain smallYours
    }

    test("HasGreatestPower allows ties — multiple creatures can share the greatest power") {
        val tieOne = EntityId.generate()
        val tieTwo = EntityId.generate()
        val small = EntityId.generate()
        val state = battlefield(
            listOf(
                tieOne to container(playerA, creature(playerA, power = 4)),
                tieTwo to container(playerA, creature(playerA, power = 4)),
                small to container(playerA, creature(playerA, power = 1))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, tieOne, filterWith(StatePredicate.HasGreatestPower))
        matched shouldContainExactlyInAnyOrder setOf(tieOne, tieTwo)
    }

    // =========================================================================
    // Equipment
    // =========================================================================

    test("IsEquipped matches only creatures with at least one Equipment attached") {
        val equipped = EntityId.generate()
        val unequipped = EntityId.generate()
        val enchanted = EntityId.generate()
        val equipment = EntityId.generate()
        val aura = EntityId.generate()

        val equippedContainer = container(playerA, creature(playerA))
            .with(AttachmentsComponent(listOf(equipment)))
        val enchantedContainer = container(playerA, creature(playerA))
            .with(AttachmentsComponent(listOf(aura)))

        val state = battlefield(
            listOf(
                equipped to equippedContainer,
                unequipped to container(playerA, creature(playerA)),
                enchanted to enchantedContainer,
                equipment to container(playerA, equipmentCard(playerA)),
                aura to container(playerA, auraCard(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(state, equipped, filterWith(StatePredicate.IsEquipped))
        matched shouldContainExactlyInAnyOrder setOf(equipped)
    }

    // =========================================================================
    // Combinators
    // =========================================================================

    test("Or matches entities that satisfy any sub-predicate") {
        val tapped = EntityId.generate()
        val attacking = EntityId.generate()
        val neither = EntityId.generate()
        val state = battlefield(
            listOf(
                tapped to container(playerA, creature(playerA), TappedComponent),
                attacking to container(playerA, creature(playerA), AttackingComponent(playerB)),
                neither to container(playerA, creature(playerA))
            )
        )
        val predicate = StatePredicate.Or(listOf(StatePredicate.IsTapped, StatePredicate.IsAttacking))
        val matched = resolver.resolveAffectedEntities(state, tapped, filterWith(predicate))
        matched shouldContainExactlyInAnyOrder setOf(tapped, attacking)
    }

    test("And matches entities that satisfy every sub-predicate") {
        val tappedAttacker = EntityId.generate()
        val tappedOnly = EntityId.generate()
        val attackingOnly = EntityId.generate()
        val state = battlefield(
            listOf(
                tappedAttacker to container(playerA, creature(playerA), TappedComponent, AttackingComponent(playerB)),
                tappedOnly to container(playerA, creature(playerA), TappedComponent),
                attackingOnly to container(playerA, creature(playerA), AttackingComponent(playerB))
            )
        )
        val predicate = StatePredicate.And(listOf(StatePredicate.IsTapped, StatePredicate.IsAttacking))
        val matched = resolver.resolveAffectedEntities(state, tappedAttacker, filterWith(predicate))
        matched shouldContainExactlyInAnyOrder setOf(tappedAttacker)
    }

    test("Not inverts its sub-predicate") {
        val tapped = EntityId.generate()
        val untapped = EntityId.generate()
        val state = battlefield(
            listOf(
                tapped to container(playerA, creature(playerA), TappedComponent),
                untapped to container(playerA, creature(playerA))
            )
        )
        val predicate = StatePredicate.Not(StatePredicate.IsTapped)
        val matched = resolver.resolveAffectedEntities(state, untapped, filterWith(predicate))
        matched shouldContainExactlyInAnyOrder setOf(untapped)
    }
})
