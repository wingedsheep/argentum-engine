package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — Phase 5: combined combat (CR 805.10).
 *
 * The active team is the attacking team and the nonactive team the defending team (805.10a). The
 * active team makes one combined attack whose creatures may be controlled by either teammate, each
 * aimed at an opposing-team player (805.10b); a creature never attacks a teammate. The defending
 * team makes one combined block in which a creature controlled by either defender may block any
 * attacker aimed at any member of that team (805.10d). Damage applies to the shared team total.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order: p0,p1 = team 0 (active);
 * p2,p3 = team 1 (defending).
 */
class TwoHeadedGiantCombatTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Combat Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry() = CardRegistry().also { it.register(bear) }

    fun init2hg(): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { bear.name })
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = Format.TwoHeadedGiant(),
                players = (1..4).map { PlayerConfig("Player $it", deck) },
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    /** Put a 2/2 bear on [owner]'s battlefield (untapped, not summoning sick). If [attacking] is
     *  set, it enters already declared as an attacker against that defender. */
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

    test("a creature attacks the opposing team, never a teammate (CR 805.10b)") {
        val (base, p) = init2hg()
        val (s1, atk) = base.withBear(p[0])
        val state = s1.copy(step = Step.DECLARE_ATTACKERS, phase = Phase.COMBAT).withPriority(p[0])
        val proc = ActionProcessor(registry())

        // Attacking the teammate (p1) is illegal.
        proc.process(state, DeclareAttackers(p[0], mapOf(atk to p[1]))).result.isSuccess.shouldBeFalse()
        // Attacking an opposing-team player (p2) is legal.
        proc.process(state, DeclareAttackers(p[0], mapOf(atk to p[2]))).result.isSuccess.shouldBeTrue()
    }

    test("the combined attack may include creatures controlled by BOTH active-team members (CR 805.10b)") {
        val (base, p) = init2hg()
        val (s1, atk0) = base.withBear(p[0])
        val (s2, atk1) = s1.withBear(p[1]) // the teammate's creature
        val state = s2.copy(step = Step.DECLARE_ATTACKERS, phase = Phase.COMBAT).withPriority(p[0])
        val proc = ActionProcessor(registry())

        // The active player declares one combined attack including the teammate's creature.
        val result = proc.process(
            state, DeclareAttackers(p[0], mapOf(atk0 to p[2], atk1 to p[3]))
        ).result
        result.isSuccess.shouldBeTrue()
        result.newState.getEntity(atk1)!!.get<AttackingComponent>()!!.defenderId shouldBe p[3]
    }

    test("the whole nonactive team defends — even an un-attacked teammate is a defending player (CR 805.10a)") {
        val (base, p) = init2hg()
        val (state, _) = base.withBear(p[0], attacking = p[2]) // p0 attacks p2 only
        // Both members of team 1 are defending players, including the un-attacked p3.
        CombatDefenders.defendingPlayers(state) shouldBe setOf(p[2], p[3])
        CombatDefenders.isDefendingPlayer(state, p[3]).shouldBeTrue()
        // Nobody on the attacking team is a defender.
        CombatDefenders.isDefendingPlayer(state, p[1]).shouldBeFalse()
    }

    test("a defending creature may block an attacker aimed at its TEAMMATE (CR 805.10d)") {
        val (base, p) = init2hg()
        val (s1, atk) = base.withBear(p[0], attacking = p[2]) // p0's attacker aimed at p2
        val (s2, blkP3) = s1.withBear(p[3])                   // blocker controlled by p3 (p2's teammate)
        var state = s2.updateEntity(p[0]) { it.with(AttackersDeclaredThisCombatComponent) }
        state = state.copy(step = Step.DECLARE_BLOCKERS, phase = Phase.COMBAT).withPriority(p[3])
        val proc = ActionProcessor(registry())

        // p3 blocks an attacker that is attacking p2 — legal in 2HG (would be illegal in FFA, 509.1b).
        val result = proc.process(state, DeclareBlockers(p[3], mapOf(blkP3 to listOf(atk)))).result
        result.isSuccess.shouldBeTrue()
    }

    test("a member of the attacking team cannot declare blockers (CR 805.10a)") {
        val (base, p) = init2hg()
        val (s1, atk) = base.withBear(p[0], attacking = p[2]) // team 0 (active) attacks p2
        val (s2, blkP1) = s1.withBear(p[1])                   // p1 is on the active (attacking) team
        var state = s2.updateEntity(p[0]) { it.with(AttackersDeclaredThisCombatComponent) }
        state = state.copy(step = Step.DECLARE_BLOCKERS, phase = Phase.COMBAT).withPriority(p[1])
        val proc = ActionProcessor(registry())

        // p1 is on the attacking team, so it may not block (the active team never blocks).
        proc.process(state, DeclareBlockers(p[1], mapOf(blkP1 to listOf(atk)))).result.isSuccess.shouldBeFalse()
    }
})
