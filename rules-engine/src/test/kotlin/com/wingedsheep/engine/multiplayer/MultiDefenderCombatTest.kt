package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Phase 1.1 of `backlog/multiplayer.md` — multi-defender combat. In a Free-for-All game the
 * active player can attack several opponents in one combat (CR 802.2); each defending player
 * declares blockers for the attackers aimed at them, in APNAP order (CR 509.1 / 101.4), and a
 * player can only block attackers aimed at them (CR 509.1b).
 */
class MultiDefenderCombatTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Combat Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry() = CardRegistry().also { it.register(bear) }

    fun initGame(players: Int): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { "Combat Test Bear" })
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                players = (1..players).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    /** Put a 2/2 bear on [owner]'s battlefield. If [attacking] is set, it enters already
     *  declared as an attacker against that defender. */
    fun GameState.withBear(owner: EntityId, attacking: EntityId? = null): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        var container = ComponentContainer.of(
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
        if (attacking != null) container = container.with(AttackingComponent(defenderId = attacking))
        val next = withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id)
        return next to id
    }

    test("CombatDefenders reports the right defending players in APNAP order") {
        val (base, players) = initGame(4) // [A, B, C, D], A active
        val (s1, _) = base.withBear(players[0], attacking = players[2]) // A attacks C
        val (s2, _) = s1.withBear(players[0], attacking = players[1])   // A attacks B
        val state = s2

        CombatDefenders.defendingPlayers(state) shouldBe setOf(players[1], players[2])
        CombatDefenders.defendingPlayersInApnapOrder(state) shouldContainExactly
            listOf(players[1], players[2])
        CombatDefenders.isDefendingPlayer(state, players[1]).shouldBeTrue()
        CombatDefenders.isDefendingPlayer(state, players[3]).shouldBeFalse()
    }

    /** Build a four-player board where A attacks B and C, then enter DECLARE_BLOCKERS with the
     *  first defender holding priority (exactly as TurnManager sets it up). */
    fun combatState(): Triple<GameState, List<EntityId>, Map<String, EntityId>> {
        val (base, players) = initGame(4)
        val (s1, atkB) = base.withBear(players[0], attacking = players[1])
        val (s2, atkC) = s1.withBear(players[0], attacking = players[2])
        val (s3, blkB) = s2.withBear(players[1])
        val (s4, blkC) = s3.withBear(players[2])
        var state = s4.updateEntity(players[0]) { it.with(AttackersDeclaredThisCombatComponent) }
        state = state.copy(step = Step.DECLARE_BLOCKERS, phase = com.wingedsheep.sdk.core.Phase.COMBAT)
        val first = CombatDefenders.defendingPlayersInApnapOrder(state).first()
        state = state.withPriority(first)
        return Triple(state, players, mapOf("atkB" to atkB, "atkC" to atkC, "blkB" to blkB, "blkC" to blkC))
    }

    test("a defender can only block an attacker that is attacking them (CR 509.1b)") {
        val (state, players, ids) = combatState()
        val processor = ActionProcessor(registry())

        // players[1] (B) tries to block the attacker aimed at players[2] (C) — illegal.
        val illegal = processor.process(
            state, DeclareBlockers(players[1], mapOf(ids["blkB"]!! to listOf(ids["atkC"]!!)))
        ).result
        illegal.isSuccess.shouldBeFalse()

        // players[1] blocks the attacker aimed at them — legal.
        val legal = processor.process(
            state, DeclareBlockers(players[1], mapOf(ids["blkB"]!! to listOf(ids["atkB"]!!)))
        ).result
        legal.isSuccess.shouldBeTrue()
    }

    test("both defenders declare blockers (APNAP) and damage lands on the right players") {
        val (initial, players, ids) = combatState()
        val processor = ActionProcessor(registry())
        var state = initial

        fun life(p: EntityId) = state.getEntity(p)?.get<LifeTotalComponent>()?.life

        // players[1] (B) blocks the attacker aimed at them; players[2] (C) lets theirs through.
        state = processor.process(
            state, DeclareBlockers(players[1], mapOf(ids["blkB"]!! to listOf(ids["atkB"]!!)))
        ).result.newState

        // While players[2] (C) still owes a block declaration, they cannot pass priority.
        processor.process(state, PassPriority(players[2])).result.isSuccess.shouldBeFalse()

        // Drive the rest of combat: declare C's (empty) blocks, then pass through to damage.
        val playersWhoDeclared = mutableSetOf<EntityId>()
        var safety = 0
        while (state.step != Step.POSTCOMBAT_MAIN && !state.gameOver) {
            check(++safety < 200) { "combat never resolved (stuck at ${state.step})" }
            val prio = state.priorityPlayerId ?: break
            val needsBlock = state.step == Step.DECLARE_BLOCKERS &&
                prio != state.activePlayerId &&
                CombatDefenders.isDefendingPlayer(state, prio) &&
                state.getEntity(prio)?.has<BlockersDeclaredThisCombatComponent>() != true
            val action = if (needsBlock) {
                playersWhoDeclared.add(prio)
                DeclareBlockers(prio, emptyMap())
            } else {
                PassPriority(prio)
            }
            val result = processor.process(state, action).result
            check(result.isSuccess || result.isPaused) { "action $action failed: ${result.error}" }
            state = result.newState
        }

        // Both defenders ended up declaring (B before the loop, C inside it) and combat resolved.
        playersWhoDeclared shouldContainExactly setOf(players[2])
        // C took 2 from the unblocked attacker; B took none (they blocked theirs).
        life(players[2]) shouldBe 18
        life(players[1]) shouldBe 20
        life(players[0]) shouldBe 20
    }
})
