package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.player.BlockerCountLimitThisCombatComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.CantBeBlockedIfDefenderControls
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessDefenderSharesCreatureType
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustBlockEachAttacker
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SharedTeamBlockDeclarationTest : FunSpec({
    val menace = card("Team Menace") {
        typeLine = "Creature"; power = 2; toughness = 2; keywords(Keyword.MENACE)
    }
    val dependent = card("Team Dependent") {
        typeLine = "Creature"; power = 2; toughness = 2
        staticAbility { ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature) }
    }
    val conditional = card("Team Conditional") {
        typeLine = "Creature"; power = 2; toughness = 2
        staticAbility { ability = CantBlockUnless(Conditions.YouControl(GameObjectFilter.Artifact)) }
    }
    val artifact = card("Team Artifact") { typeLine = "Artifact" }
    val forestwalker = card("Team Forestwalker") {
        typeLine = "Creature"; power = 2; toughness = 2; keywords(Keyword.FORESTWALK)
    }
    val artifactEvasion = card("Team Artifact Evasion") {
        typeLine = "Creature"; power = 2; toughness = 2
        staticAbility { ability = CantBeBlockedIfDefenderControls(GameObjectFilter.Artifact) }
    }
    val tribalEvasion = card("Team Tribal Evasion") {
        typeLine = "Creature"; power = 2; toughness = 2
        staticAbility { ability = CantBeBlockedUnlessDefenderSharesCreatureType(2) }
    }
    fun board(format: Format = Format.TwoHeadedGiant()): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(menace, dependent, conditional, artifact, forestwalker, artifactEvasion, tribalEvasion))
        val players = d.initMultiplayer(List(4) { Deck.of("Mountain" to 40) }, format = format,
            teams = listOf(listOf(0, 1), listOf(2, 3)))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }
    fun GameTestDriver.blocks(attacks: Map<EntityId, EntityId>, defender: EntityId) {
        var next = state.copy(step = Step.DECLARE_BLOCKERS, phase = Phase.COMBAT, priorityPlayerId = defender)
        for ((attacker, target) in attacks) next = next.updateEntity(attacker) { it.with(AttackingComponent(target)) }
        replaceState(next)
    }

    test("combined menace block includes both heads and completes their declaration together") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], menace.name)
        val b = d.putCreatureOnBattlefield(p[2], "Centaur Courser")
        val c = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
        d.blocks(mapOf(a to p[3]), p[2])
        val action = d.legalActions(p[2]).single()
        action.validBlockers!!.toSet() shouldBe setOf(b, c)
        action.blockableAttackers shouldBe listOf(a)
        d.declareBlockers(p[2], mapOf(b to listOf(a))).error shouldNotBe null
        val result = d.declareBlockers(p[2], mapOf(b to listOf(a), c to listOf(a)))
        result.error shouldBe null
        result.events.filterIsInstance<BlockersDeclaredEvent>().single().blockers.keys shouldBe setOf(b, c)
        listOf(p[2], p[3]).forEach {
            d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
        }
        d.replaceState(d.state.withPriority(p[3]))
        d.declareBlockers(p[3], emptyMap()).error shouldNotBe null
    }

    test("an empty combined declaration closes both heads") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        d.blocks(mapOf(a to p[2]), p[2])
        d.declareBlockers(p[2], emptyMap()).error shouldBe null
        listOf(p[2], p[3]).forEach {
            d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
        }
    }

    test("a required co-blocker recruits the teammate while inability to pair removes the requirement") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val b = d.putCreatureOnBattlefield(p[3], dependent.name)
        val grant = d.services.effectExecutorRegistry.execute(d.state,
            Effects.GrantStaticAbility(MustBlockEachAttacker(), EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = b, controllerId = p[3], targets = listOf(ChosenTarget.Permanent(b))))
        grant.error shouldBe null
        d.replaceState(grant.state)
        d.blocks(mapOf(a to p[2]), p[2])
        val alone = d.state
        d.declareBlockers(p[2], emptyMap()).error shouldBe null
        d.replaceState(alone)
        val c = d.putCreatureOnBattlefield(p[2], "Centaur Courser")
        d.declareBlockers(p[2], emptyMap()).error shouldNotBe null
        d.declareBlockers(p[2], mapOf(b to listOf(a), c to listOf(a))).error shouldBe null
    }

    test("a per-player blocker limit does not consume the teammate's allowance") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val b = d.putCreatureOnBattlefield(p[2], "Centaur Courser")
        val c = d.putCreatureOnBattlefield(p[2], "Centaur Courser")
        val e = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
        d.replaceState(d.state.updateEntity(p[2]) { it.with(BlockerCountLimitThisCombatComponent(1)) })
        d.blocks(mapOf(a to p[2]), p[2])
        d.declareBlockers(p[2], mapOf(b to listOf(a), c to listOf(a))).error shouldNotBe null
        d.declareBlockers(p[2], mapOf(b to listOf(a), e to listOf(a))).error shouldBe null
    }

    test("empty target lists cannot masquerade as a co-blocker") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val b = d.putCreatureOnBattlefield(p[2], dependent.name)
        val c = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
        d.blocks(mapOf(a to p[2]), p[2])
        d.declareBlockers(p[2], mapOf(b to listOf(a), c to emptyList())).error shouldNotBe null
        d.state.getEntity(b)!!.has<BlockingComponent>() shouldBe false
    }

    test("team versus team without shared turns cannot declare a teammate's blocker") {
        val (d, p) = board(Format.TeamVsTeam())
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val b = d.putCreatureOnBattlefield(p[2], "Centaur Courser")
        val c = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
        d.blocks(mapOf(a to p[2]), p[2])
        d.legalActions(p[2]).single().validBlockers shouldBe listOf(b)
        d.declareBlockers(p[2], mapOf(c to listOf(a))).error shouldNotBe null
        d.declareBlockers(p[2], mapOf(b to listOf(a))).error shouldBe null
        d.state.getEntity(p[3])!!.has<BlockersDeclaredThisCombatComponent>() shouldBe false
    }
    test("a teammate's conditional blocker reads its own controller in queries and declarations") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val b = d.putCreatureOnBattlefield(p[3], conditional.name)
        d.putPermanentOnBattlefield(p[2], artifact.name)
        d.blocks(mapOf(a to p[2]), p[2])
        d.legalActions(p[2]).single().validBlockers!!.contains(b) shouldBe false
        d.declareBlockers(p[2], mapOf(b to listOf(a))).error shouldNotBe null
        d.putPermanentOnBattlefield(p[3], artifact.name)
        d.legalActions(p[2]).single().validBlockers!!.contains(b) shouldBe true
        d.declareBlockers(p[2], mapOf(b to listOf(a))).error shouldBe null
    }

    test("a co-blocker restriction disappears when its creature loses all abilities") {
        val (d, p) = board()
        val a = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val b = d.putCreatureOnBattlefield(p[3], dependent.name)
        val removed = d.services.effectExecutorRegistry.execute(d.state, Effects.RemoveAllAbilities(EffectTarget.Self),
            EffectContext(sourceId = b, controllerId = p[3]))
        removed.error shouldBe null
        d.replaceState(removed.state)
        d.blocks(mapOf(a to p[2]), p[2])
        d.declareBlockers(p[2], mapOf(b to listOf(a))).error shouldBe null
    }

    for (permanentOnAttackedSeat in listOf(true, false)) {
        val ownerDescription = if (permanentOnAttackedSeat) "attacked seat" else "blocking teammate"
        test("forestwalk checks the attacked player when the Forest belongs to the $ownerDescription") {
            val (d, p) = board()
            val a = d.putCreatureOnBattlefield(p[0], forestwalker.name)
            val b = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
            d.putPermanentOnBattlefield(if (permanentOnAttackedSeat) p[2] else p[3], "Forest")
            d.blocks(mapOf(a to p[2]), p[2])
            d.legalActions(p[2]).single().validBlockers!!.contains(b) shouldBe !permanentOnAttackedSeat
            val result = d.declareBlockers(p[2], mapOf(b to listOf(a)))
            (result.error == null) shouldBe !permanentOnAttackedSeat
        }
        test("defender artifact evasion checks the attacked player when the artifact belongs to the $ownerDescription") {
            val (d, p) = board()
            val a = d.putCreatureOnBattlefield(p[0], artifactEvasion.name)
            val b = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
            d.putPermanentOnBattlefield(if (permanentOnAttackedSeat) p[2] else p[3], artifact.name)
            d.blocks(mapOf(a to p[2]), p[2])
            d.legalActions(p[2]).single().validBlockers!!.contains(b) shouldBe !permanentOnAttackedSeat
            val result = d.declareBlockers(p[2], mapOf(b to listOf(a)))
            (result.error == null) shouldBe !permanentOnAttackedSeat
        }
        test("defender shared-type condition checks the attacked player when the pair belongs to the $ownerDescription") {
            val (d, p) = board()
            val a = d.putCreatureOnBattlefield(p[0], tribalEvasion.name)
            val b = d.putCreatureOnBattlefield(p[3], "Centaur Courser")
            repeat(2) { d.putCreatureOnBattlefield(if (permanentOnAttackedSeat) p[2] else p[3], "Centaur Courser") }
            d.blocks(mapOf(a to p[2]), p[2])
            d.legalActions(p[2]).single().validBlockers!!.contains(b) shouldBe permanentOnAttackedSeat
            val result = d.declareBlockers(p[2], mapOf(b to listOf(a)))
            (result.error == null) shouldBe permanentOnAttackedSeat
        }
    }

})
