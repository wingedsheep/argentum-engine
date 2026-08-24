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
import com.wingedsheep.engine.state.components.battlefield.ReceivedCountersThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisTurnComponent
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
import io.kotest.assertions.withClue
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
            c = c.copy(components = c.components + (component::class.java to component))
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

    test("HasDealtDamage matches every creature with a marker, whatever turn it was stamped") {
        val thisTurn = EntityId.generate()
        val earlierTurn = EntityId.generate()
        val passive = EntityId.generate()
        val state = battlefield(
            listOf(
                thisTurn to container(playerA, creature(playerA), HasDealtDamageComponent(5)),
                earlierTurn to container(playerA, creature(playerA), HasDealtDamageComponent(3)),
                passive to container(playerA, creature(playerA))
            )
        ).copy(turnNumber = 5)
        val matched = resolver.resolveAffectedEntities(state, thisTurn, filterWith(StatePredicate.HasDealtDamage()))
        matched shouldContainExactlyInAnyOrder setOf(thisTurn, earlierTurn)
    }

    test("HasDealtDamage(thisTurnOnly) matches only creatures whose marker names the current turn") {
        val thisTurn = EntityId.generate()
        val earlierTurn = EntityId.generate()
        val passive = EntityId.generate()
        val state = battlefield(
            listOf(
                thisTurn to container(playerA, creature(playerA), HasDealtDamageComponent(5)),
                earlierTurn to container(playerA, creature(playerA), HasDealtDamageComponent(3)),
                passive to container(playerA, creature(playerA))
            )
        ).copy(turnNumber = 5)
        val matched = resolver.resolveAffectedEntities(
            state, thisTurn, filterWith(StatePredicate.HasDealtDamage(thisTurnOnly = true))
        )
        matched shouldContainExactlyInAnyOrder setOf(thisTurn)
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
    // Counter history — ReceivedCounterThisTurn
    // =========================================================================

    test("ReceivedCounterThisTurn with no narrowing matches any permanent with a recorded placement") {
        val marked = EntityId.generate()
        val markedTypeless = EntityId.generate()
        val unmarked = EntityId.generate()
        val state = battlefield(
            listOf(
                marked to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(counterTypes = setOf("stun"))
                ),
                // Unreachable in practice — `recordCounterPlacement` requires a counter kind, so
                // every stamped marker names at least one. Present here to pin the widest reading
                // to the recorded kinds rather than to marker presence, which is what keeps it
                // symmetric with the placer-scoped reading (both are `isNotEmpty()` over a set).
                markedTypeless to container(playerA, creature(playerA), ReceivedCountersThisTurnComponent()),
                unmarked to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state, marked, filterWith(StatePredicate.ReceivedCounterThisTurn())
        )
        matched shouldContainExactlyInAnyOrder setOf(marked)
    }

    test("ReceivedCounterThisTurn scoped to +1/+1 ignores other counter kinds") {
        val gotPlusOne = EntityId.generate()
        val gotStun = EntityId.generate()
        val state = battlefield(
            listOf(
                gotPlusOne to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(counterTypes = setOf("+1/+1"))
                ),
                gotStun to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(counterTypes = setOf("stun"))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state, gotPlusOne, filterWith(StatePredicate.ReceivedCounterThisTurn(counterType = "+1/+1"))
        )
        matched shouldContainExactlyInAnyOrder setOf(gotPlusOne)
    }

    test("ReceivedCounterThisTurn with placedByController ignores counters an opponent put on") {
        val youPlaced = EntityId.generate()
        val opponentPlaced = EntityId.generate()
        val state = battlefield(
            listOf(
                youPlaced to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(
                        counterTypes = setOf("+1/+1"),
                        typesFromController = setOf("+1/+1")
                    )
                ),
                // An opponent proliferating your creature records the kind but not the placer leg.
                opponentPlaced to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(counterTypes = setOf("+1/+1"))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state,
            youPlaced,
            filterWith(StatePredicate.ReceivedCounterThisTurn("+1/+1", placedByController = true))
        )
        matched shouldContainExactlyInAnyOrder setOf(youPlaced)
    }

    test("ReceivedCounterThisTurn still matches after the counters themselves are gone") {
        // The marker is stamped at placement time, so a creature whose +1/+1 counter was since
        // removed (or annihilated by a -1/-1 counter) keeps matching — "what you put on it this
        // turn", not "what is on it now". No CountersComponent at all here.
        val hadCounter = EntityId.generate()
        val neverHad = EntityId.generate()
        val state = battlefield(
            listOf(
                hadCounter to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(
                        counterTypes = setOf("+1/+1"),
                        typesFromController = setOf("+1/+1")
                    )
                ),
                neverHad to container(
                    playerA, creature(playerA),
                    CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state,
            hadCounter,
            filterWith(StatePredicate.ReceivedCounterThisTurn("+1/+1", placedByController = true))
        )
        // The creature that merely *has* a +1/+1 counter (e.g. it entered play with one on a
        // previous turn) carries no marker and must not match.
        matched shouldContainExactlyInAnyOrder setOf(hadCounter)
    }

    test("ReceivedCounterThisTurn narrowed by kind does not match a bare marker") {
        // The type-scoped reading has to go through the recorded kinds, so a placement path that
        // stamped no kind fails closed rather than satisfying "+1/+1 counters".
        val bare = EntityId.generate()
        val typed = EntityId.generate()
        val state = battlefield(
            listOf(
                bare to container(playerA, creature(playerA), ReceivedCountersThisTurnComponent()),
                typed to container(
                    playerA, creature(playerA),
                    ReceivedCountersThisTurnComponent(counterTypes = setOf("+1/+1"))
                )
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state, typed, filterWith(StatePredicate.ReceivedCounterThisTurn("+1/+1"))
        )
        matched shouldContainExactlyInAnyOrder setOf(typed)
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

    // =========================================================================
    // CouldNotHaveAttackedThisTurn (Season of the Witch)
    //
    // This branch has no card using it inside a continuous effect's `affects` filter, so nothing
    // else exercises it — and its whole job is to give the same answer PredicateEvaluator gives.
    // Each clause gets a case here so the two can't drift apart silently.
    // =========================================================================

    /** A battlefield where [active] holds the turn and reached a Declare Attackers Step. */
    fun battlefieldOnTurnOf(
        active: EntityId,
        entities: List<Pair<EntityId, ComponentContainer>>,
        declaredAttackers: Boolean = true
    ): GameState {
        var state = battlefield(entities).copy(activePlayerId = active)
        if (declaredAttackers) {
            state = state.updateEntity(active) { it.with(AttackersDeclaredThisTurnComponent) }
        }
        return state
    }

    test("CouldNotHaveAttackedThisTurn spares every creature the nonactive player controls") {
        val mine = EntityId.generate()
        val theirs = EntityId.generate()
        val state = battlefieldOnTurnOf(
            playerA,
            listOf(
                mine to container(playerA, creature(playerA)),
                theirs to container(playerB, creature(playerB))
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state, mine, filterWith(StatePredicate.CouldNotHaveAttackedThisTurn)
        )
        matched shouldContainExactlyInAnyOrder setOf(theirs)
    }

    test("CouldNotHaveAttackedThisTurn spares everyone when no Declare Attackers Step happened") {
        // False Peace / Fatespinner: the active player held the turn but never reached the step,
        // so nobody stayed home by choice.
        val mine = EntityId.generate()
        val theirs = EntityId.generate()
        val state = battlefieldOnTurnOf(
            playerA,
            listOf(
                mine to container(playerA, creature(playerA)),
                theirs to container(playerB, creature(playerB))
            ),
            declaredAttackers = false
        )
        val matched = resolver.resolveAffectedEntities(
            state, mine, filterWith(StatePredicate.CouldNotHaveAttackedThisTurn)
        )
        matched shouldContainExactlyInAnyOrder setOf(mine, theirs)
    }

    test("CouldNotHaveAttackedThisTurn spares a summoning-sick creature but not its neighbour") {
        val sick = EntityId.generate()
        val ready = EntityId.generate()
        val state = battlefieldOnTurnOf(
            playerA,
            listOf(
                sick to container(playerA, creature(playerA), EnteredThisTurnComponent),
                ready to container(playerA, creature(playerA))
            )
        )
        val matched = resolver.resolveAffectedEntities(
            state, ready, filterWith(StatePredicate.CouldNotHaveAttackedThisTurn)
        )
        matched shouldContainExactlyInAnyOrder setOf(sick)
    }

    test("CouldNotHaveAttackedThisTurn reads the projected controller, not the base one") {
        // Act of Treason: the base ControllerComponent still says playerB, but projection has
        // handed the creature to the active player, who could therefore have attacked with it.
        val stolen = EntityId.generate()
        val state = battlefieldOnTurnOf(
            playerA,
            listOf(stolen to container(playerB, creature(playerB)))
        )
        val projected = mapOf(
            stolen to MutableProjectedValues().apply { controllerId = playerA }
        )
        val matched = resolver.resolveAffectedEntities(
            state, stolen, filterWith(StatePredicate.CouldNotHaveAttackedThisTurn), projected
        )
        withClue("projection says the active player controls it, so staying home was a choice") {
            matched shouldNotContain stolen
        }
    }

    test("CouldNotHaveAttackedThisTurn spares a creature a projected effect stopped from attacking") {
        // Pacifism. cantAttack only ever comes from projection — there is no base flag for it.
        val pacified = EntityId.generate()
        val free = EntityId.generate()
        val state = battlefieldOnTurnOf(
            playerA,
            listOf(
                pacified to container(playerA, creature(playerA)),
                free to container(playerA, creature(playerA))
            )
        )
        val projected = mapOf(
            pacified to MutableProjectedValues().apply { cantAttack = true },
            free to MutableProjectedValues()
        )
        val matched = resolver.resolveAffectedEntities(
            state, free, filterWith(StatePredicate.CouldNotHaveAttackedThisTurn), projected
        )
        matched shouldContainExactlyInAnyOrder setOf(pacified)
    }
})
