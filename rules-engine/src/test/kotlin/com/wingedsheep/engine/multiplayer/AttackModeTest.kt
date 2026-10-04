package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.mechanics.combat.rules.AttackCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.AttackModeDefenderRule
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ProtectorComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * CR 802 (attack multiple players) / CR 803 (attack left / attack right). The lobby-selected
 * [AttackMode] constrains which opponents a creature may attack. The single source of truth is
 * [CombatDefenders.legalDefendingPlayers]; [AttackModeDefenderRule] enforces it at declaration.
 *
 * Turn order proceeds to each player's left, so for active player A in seat order [A, B, C, D]:
 * the player to A's left is B (next seat), the player to A's right is D (previous seat).
 */
class AttackModeTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Attack Mode Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry() = CardRegistry().also { it.register(bear) }

    fun initGame(players: Int, mode: AttackMode): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { "Attack Mode Bear" })
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..players).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0,
                attackMode = mode
            )
        )
        return result.state to result.playerIds
    }

    fun GameState.withBear(owner: EntityId): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = bear.name,
                name = bear.name,
                manaCost = bear.manaCost,
                typeLine = bear.typeLine,
                baseStats = bear.creatureStats,
                ownerId = owner
            ),
            OwnerComponent(owner),
            ControllerComponent(owner)
        )
        return withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id) to id
    }

    // ── legalDefendingPlayers: the seating math both call sites share ───────────

    test("MULTIPLE lets the active player attack every opponent (CR 802)") {
        val (state, p) = initGame(4, AttackMode.MULTIPLE) // [A, B, C, D], A active
        CombatDefenders.legalDefendingPlayers(state, p[0]) shouldBe setOf(p[1], p[2], p[3])
    }

    test("LEFT restricts to the next seat, RIGHT to the previous seat (CR 803)") {
        val (left, p) = initGame(4, AttackMode.LEFT)
        CombatDefenders.legalDefendingPlayers(left, p[0]) shouldBe setOf(p[1]) // B is to A's left

        val (right, q) = initGame(4, AttackMode.RIGHT)
        CombatDefenders.legalDefendingPlayers(right, q[0]) shouldBe setOf(q[3]) // D is to A's right
    }

    test("the seat helpers skip a player who has left the game (CR 800.4a)") {
        val (base, p) = initGame(4, AttackMode.LEFT)
        // B (A's left neighbour) leaves — A's left is now C.
        val state = base.updateEntity(p[1]) { it.with(PlayerLostComponent(LossReason.CONCESSION)) }
        CombatDefenders.legalDefendingPlayers(state, p[0]) shouldBe setOf(p[2])
    }

    test("in a two-player game every mode permits the sole opponent") {
        for (mode in AttackMode.entries) {
            val (state, p) = initGame(2, mode)
            CombatDefenders.legalDefendingPlayers(state, p[0]) shouldBe setOf(p[1])
        }
    }

    // ── AttackModeDefenderRule: the declaration-time enforcement ────────────────

    fun ruleFor(state: GameState, attacker: EntityId, attackingPlayer: EntityId, defender: EntityId): String? =
        AttackModeDefenderRule().check(
            AttackCheckContext(
                state = state,
                projected = state.projectedState,
                attackerId = attacker,
                attackingPlayer = attackingPlayer,
                cardRegistry = registry()
            ),
            defender
        )

    test("MULTIPLE allows attacking any opponent") {
        val (base, p) = initGame(4, AttackMode.MULTIPLE)
        val (state, atk) = base.withBear(p[0])
        ruleFor(state, atk, p[0], p[1]).shouldBeNull()
        ruleFor(state, atk, p[0], p[2]).shouldBeNull()
        ruleFor(state, atk, p[0], p[3]).shouldBeNull()
    }

    test("LEFT allows only the left neighbour and rejects the others") {
        val (base, p) = initGame(4, AttackMode.LEFT)
        val (state, atk) = base.withBear(p[0])
        ruleFor(state, atk, p[0], p[1]).shouldBeNull()       // B — to the left, allowed
        ruleFor(state, atk, p[0], p[2]).shouldNotBeNull()    // C — rejected
        ruleFor(state, atk, p[0], p[3]).shouldNotBeNull()    // D — rejected
    }

    /** A Siege on the battlefield under [controller]'s control, protected by [protector]. */
    fun GameState.withSiege(controller: EntityId, protector: EntityId): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        val container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "Attack Mode Siege",
                name = "Attack Mode Siege",
                manaCost = ManaCost.parse("{2}{W}"),
                typeLine = TypeLine(cardTypes = setOf(CardType.BATTLE), subtypes = setOf(Subtype.SIEGE)),
                ownerId = controller
            ),
            OwnerComponent(controller),
            ControllerComponent(controller),
            ProtectorComponent(protector)
        )
        return withEntity(id, container).addToZone(ZoneKey(controller, Zone.BATTLEFIELD), id) to id
    }

    test("LEFT keys a battle off its protector, not its controller (CR 310.9d)") {
        val (base, p) = initGame(4, AttackMode.LEFT)
        val (withBear, atk) = base.withBear(p[0])
        // A's own Siege, protected by B (A's left neighbour): A may attack it.
        val (s1, ownSiege) = withBear.withSiege(controller = p[0], protector = p[1])
        ruleFor(s1, atk, p[0], ownSiege).shouldBeNull()
        // B's Siege, protected by C: controlled by A's left neighbour, but C defends it — rejected.
        val (s2, neighbourSiege) = s1.withSiege(controller = p[1], protector = p[2])
        ruleFor(s2, atk, p[0], neighbourSiege).shouldNotBeNull()
    }

    test("RIGHT allows only the right neighbour and rejects the others") {
        val (base, p) = initGame(4, AttackMode.RIGHT)
        val (state, atk) = base.withBear(p[0])
        ruleFor(state, atk, p[0], p[3]).shouldBeNull()       // D — to the right, allowed
        ruleFor(state, atk, p[0], p[1]).shouldNotBeNull()    // B — rejected
        ruleFor(state, atk, p[0], p[2]).shouldNotBeNull()    // C — rejected
    }

    test("the client state names a left/right restriction and omits unrestricted attacks") {
        fun clientMode(mode: AttackMode): AttackMode? {
            val (state, p) = initGame(4, mode)
            val registry = registry()
            return ClientStateTransformer(registry, predicateEvaluator = PredicateEvaluator(cardRegistry = registry))
                .transform(state, p[0]).attackMode
        }
        clientMode(AttackMode.LEFT) shouldBe AttackMode.LEFT
        clientMode(AttackMode.RIGHT) shouldBe AttackMode.RIGHT
        clientMode(AttackMode.MULTIPLE).shouldBeNull()
    }
})
