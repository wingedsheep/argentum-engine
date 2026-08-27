package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Two-Headed Giant — team priority (CR 805.5).
 *
 * > **805.5.** Teams have priority, not individual players.
 * > **805.5a** A player may cast a spell, activate an ability, or take a special action when their
 * > team has priority.
 *
 * [GameState.priorityPlayerId] stays a single seat — the *baton*: who the server nudges, whose
 * auto-pass runs, which board the UI focuses. Permission to act is [GameState.hasPriority], which
 * admits the baton holder's whole team. The practical difference this buys is the one the rules
 * describe and a table takes for granted: you cast a creature, your partner enchants it, and you
 * act on the enchanted creature — without the baton having to make a lap of the table between
 * every link.
 *
 * The phase still advances only once **every** seat has passed ([GameState.allPlayersPassed]), so
 * nobody's window is spent by a teammate; team priority widens who *may* act, it never takes a
 * window away.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order: p0,p1 = team 0 (starting);
 * p2,p3 = team 1.
 */
class TwoHeadedGiantTeamPriorityTest : FunSpec({

    /** Free so the test never has to model mana — this is about who may act, not what it costs. */
    val sprout = CardDefinition.creature(
        name = "Team Priority Sprout",
        manaCost = ManaCost.parse("{0}"),
        subtypes = setOf(Subtype("Elemental")),
        power = 1,
        toughness = 1,
    )

    /** A free instant, so a teammate can respond while a spell is still on the stack. */
    val hunch = CardDefinition.instant(
        name = "Team Priority Hunch",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = DrawCardsEffect(1, EffectTarget.Controller)),
    )

    fun registry() = CardRegistry().also { it.register(listOf(sprout, hunch)) }

    fun boot(format: Format, teams: List<List<Int>>?, playerCount: Int = 4):
        Triple<GameState, List<EntityId>, ActionProcessor> {
        val deck = Deck(cards = List(20) { sprout.name } + List(20) { hunch.name })
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = format,
                players = (1..playerCount).map { PlayerConfig("Player $it", deck) },
                teams = teams,
                startingPlayerIndex = 0,
                skipMulligans = true,
                // Pinned: the chain test needs two Sprouts in p0's opening hand and one in p1's;
                // an unseeded shuffle misses that roughly one run in twenty.
                seed = 2026_08_27L,
            )
        )
        return Triple(result.state, result.playerIds, ActionProcessor(registry()))
    }

    fun boot2hg() = boot(Format.TwoHeadedGiant(), listOf(listOf(0, 1), listOf(2, 3)))

    /** Advance to the starting team's precombat main phase with an empty stack. */
    fun toPrecombatMain(start: GameState, proc: ActionProcessor): GameState {
        var state = start
        var guard = 0
        while (!(state.phase == Phase.PRECOMBAT_MAIN && state.step == Step.PRECOMBAT_MAIN && state.stack.isEmpty())) {
            check(guard++ < 200) { "never reached precombat main (step=${state.step})" }
            val prio = state.priorityPlayerId ?: error("no priority holder before precombat main")
            state = proc.process(state, PassPriority(prio)).result.newState
        }
        return state
    }

    /** The first [cardName] in [player]'s hand. */
    fun GameState.handCard(player: EntityId, cardName: String): EntityId =
        getZone(ZoneKey(player, Zone.HAND)).first { id ->
            getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.cardDefinitionId == cardName
        }

    fun GameState.battlefieldCount(player: EntityId, cardName: String): Int =
        getBattlefield().count { id ->
            val c = getEntity(id) ?: return@count false
            c.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == player &&
                c.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.cardDefinitionId == cardName
        }

    /** Pass priority round-robin until the stack empties (resolving whatever is on it). */
    fun resolveStack(start: GameState, proc: ActionProcessor): GameState {
        var state = start
        var guard = 0
        while (state.stack.isNotEmpty()) {
            check(guard++ < 50) { "stack never emptied" }
            val prio = state.priorityPlayerId ?: error("no priority holder with a non-empty stack")
            state = proc.process(state, PassPriority(prio)).result.newState
        }
        return state
    }

    test("hasPriority admits the baton holder's whole team, and nobody else (CR 805.5)") {
        val (booted, p, proc) = boot2hg()
        val state = toPrecombatMain(booted, proc)

        state.priorityPlayerId shouldBe p[0]
        state.hasPriority(p[0]).shouldBeTrue()
        state.hasPriority(p[1]).shouldBeTrue()   // teammate of the baton holder
        state.hasPriority(p[2]).shouldBeFalse()  // opposing team
        state.hasPriority(p[3]).shouldBeFalse()
        state.priorityTeam shouldBe listOf(p[0], p[1])
    }

    test("a free-for-all pod is untouched: priority is still one seat") {
        val (booted, p, proc) = boot(Format.Standard, teams = null, playerCount = 4)
        val state = toPrecombatMain(booted, proc)

        state.hasPriority(state.priorityPlayerId!!).shouldBeTrue()
        p.filter { it != state.priorityPlayerId }.forEach { state.hasPriority(it).shouldBeFalse() }
        state.priorityTeam shouldBe listOf(state.priorityPlayerId!!)
    }

    test("Team vs. Team takes individual turns (CR 808.4), so priority stays individual too") {
        // The contrast that makes `hasPriority` a format capability rather than "has a team":
        // these players *are* teammates, but their team does not share a turn.
        val (booted, p, proc) = boot(Format.TeamVsTeam(), listOf(listOf(0, 1), listOf(2, 3)))
        val state = toPrecombatMain(booted, proc)

        state.priorityPlayerId shouldBe p[0]
        state.hasPriority(p[0]).shouldBeTrue()
        state.hasPriority(p[1]).shouldBeFalse()
    }

    test("a teammate may respond to their partner's spell while it is still on the stack") {
        val (booted, p, proc) = boot2hg()
        var state = toPrecombatMain(booted, proc)

        // p0 casts; the baton stays with p0 and the spell sits on the stack.
        val creature = state.handCard(p[0], sprout.name)
        state = proc.process(state, CastSpell(p[0], creature)).result.newState
        state.stack.size shouldBe 1
        state.priorityPlayerId shouldBe p[0]

        // p1 — the teammate, *not* the baton holder — answers with an instant. Before team
        // priority this was rejected outright with "You don't have priority".
        val instant = state.handCard(p[1], hunch.name)
        val result = proc.process(state, CastSpell(p[1], instant)).result
        result.error.shouldBeNull()
        result.newState.stack.size shouldBe 2
    }

    test("an opposing player still cannot act on the active team's priority") {
        val (booted, p, proc) = boot2hg()
        var state = toPrecombatMain(booted, proc)

        val creature = state.handCard(p[0], sprout.name)
        state = proc.process(state, CastSpell(p[0], creature)).result.newState

        val instant = state.handCard(p[2], hunch.name)
        val result = proc.process(state, CastSpell(p[2], instant)).result
        result.error shouldBe "You don't have priority"
    }

    test("the chain: partner acts at sorcery speed without the baton making a lap (CR 805.5a)") {
        val (booted, p, proc) = boot2hg()
        var state = toPrecombatMain(booted, proc)

        // Link 1 — p0 plays a creature and lets it resolve. Priority comes back to p0.
        state = proc.process(state, CastSpell(p[0], state.handCard(p[0], sprout.name))).result.newState
        state = resolveStack(state, proc)
        state.priorityPlayerId shouldBe p[0]
        state.battlefieldCount(p[0], sprout.name) shouldBe 1

        // Link 2 — p1 plays a creature *immediately*. The baton is on p0 and p0 has not passed;
        // it is p1's team's turn and p1's team's priority, which is all CR 805.5a asks for.
        val partnerPlay = proc.process(state, CastSpell(p[1], state.handCard(p[1], sprout.name))).result
        partnerPlay.error.shouldBeNull()
        state = resolveStack(partnerPlay.newState, proc)
        state.battlefieldCount(p[1], sprout.name) shouldBe 1

        // Link 3 — and back to p0, again without p1 having to pass.
        val backToP0 = proc.process(state, CastSpell(p[0], state.handCard(p[0], sprout.name))).result
        backToP0.error.shouldBeNull()
        state = resolveStack(backToP0.newState, proc)
        state.battlefieldCount(p[0], sprout.name) shouldBe 2

        // The whole chain happened inside one main phase.
        state.phase shouldBe Phase.PRECOMBAT_MAIN
    }

    test("an out-of-order action re-arms the whole team: every seat still gets its own window") {
        val (booted, p, proc) = boot2hg()
        var state = toPrecombatMain(booted, proc)

        // p0 passes, then p1 casts instead of passing. p0's pass is void — CR 805.5b only ends a
        // priority round when every team passes *in succession*, and an action intervened.
        state = proc.process(state, PassPriority(p[0])).result.newState
        state.priorityPassedBy shouldBe setOf(p[0])
        state = proc.process(state, CastSpell(p[1], state.handCard(p[1], hunch.name))).result.newState
        state.priorityPassedBy.shouldBeEmpty()

        // Nothing resolves until all four seats have passed again — including p0, whose earlier
        // pass no longer counts.
        var guard = 0
        val stackSize = state.stack.size
        while (state.stack.size == stackSize) {
            check(guard++ < 10) { "stack never resolved" }
            val before = state.priorityPassedBy.size
            state = proc.process(state, PassPriority(state.priorityPlayerId!!)).result.newState
            if (state.stack.size == stackSize) state.priorityPassedBy.size shouldBe before + 1
        }
        guard shouldBe 4
    }

    test("the baton skips seats that already passed this round") {
        val (booted, p, proc) = boot2hg()
        var state = toPrecombatMain(booted, proc)

        // p1 passes out of baton order. The baton does NOT move — p0 still owes a pass, and p1
        // declining their own window is no reason to spend p0's.
        state = proc.process(state, PassPriority(p[1])).result.newState
        state.priorityPassedBy shouldBe setOf(p[1])
        state.priorityPlayerId shouldBe p[0]

        // Now p0 passes. The next seat that still owes a pass is p2 — p1 is skipped rather than
        // handed a window it just declined.
        state = proc.process(state, PassPriority(p[0])).result.newState
        state.priorityPlayerId shouldBe p[2]
        state.priorityPassedBy shouldBe setOf(p[0], p[1])
    }

    test("nextUnpassedPriorityAfter is plain getNextPlayer while nobody has passed") {
        val (booted, p, proc) = boot2hg()
        val state = toPrecombatMain(booted, proc)
        state.priorityPassedBy.shouldBeEmpty()
        p.forEach { state.nextUnpassedPriorityAfter(it) shouldBe state.getNextPlayer(it) }
    }

    test("a teammate's cast is a real spell of theirs, not of the baton holder") {
        val (booted, p, proc) = boot2hg()
        var state = toPrecombatMain(booted, proc)

        // p0 (the baton holder) casts a creature; p1 responds with a draw spell of their own.
        state = proc.process(state, CastSpell(p[0], state.handCard(p[0], sprout.name))).result.newState
        val handsBefore = p.associateWith { state.getZone(ZoneKey(it, Zone.HAND)).size }
        state = proc.process(state, CastSpell(p[1], state.handCard(p[1], hunch.name))).result.newState
        state = resolveStack(state, proc)

        // The creature is controlled by p0 and the card is drawn by p1 — team priority hands out
        // permission to act, never controllership of what gets cast.
        state.battlefieldCount(p[0], sprout.name) shouldBe 1
        state.battlefieldCount(p[1], sprout.name) shouldBe 0
        // p1 spent the instant and drew one back: net zero. p0 only lost the creature it cast.
        state.getZone(ZoneKey(p[1], Zone.HAND)).size shouldBe handsBefore.getValue(p[1])
        state.getZone(ZoneKey(p[0], Zone.HAND)).size shouldBe handsBefore.getValue(p[0])
    }

    test("priorityTeam is empty when nobody holds priority") {
        val (booted, _, _) = boot2hg()
        // Untap grants no priority (CR 502.3); the booted state is pre-priority.
        val noPriority = booted.copy(priorityPlayerId = null)
        noPriority.priorityTeam shouldBe emptyList()
        noPriority.hasPriority(booted.turnOrder.first()).shouldBeFalse()
        noPriority.priorityPlayerId.shouldBeNull()
        booted.turnOrder.size shouldNotBe 0
    }
})
