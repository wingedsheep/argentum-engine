package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.ChangeTargetExecutor
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellExecutor
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetTriggeredAbilityExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

/**
 * A copy may keep or change each target (CR 707.10c); a changed target must be legal and must not
 * make a kept one illegal (CR 115.7d).
 * When a later target reads an earlier one — "Choose target opponent. Destroy target land that
 * player controls." (The Fall of Kroog) — which lands are legal depends on the opponent the copy
 * ends up with, kept or changed. The prompt carries that as a per-prefix table and the decision
 * validator holds the answer to it.
 */
class DependentCopyTargetTest : FunSpec({
    val raze = card("Test Dependent Raze") {
        manaCost = "{R}"; typeLine = "Sorcery"
        spell {
            val opponent = target(Targets.Opponent)
            val land = target(TargetFilter(GameObjectFilter.Land.targetPlayerControls(opponent)))
            effect = Effects.Destroy(land)
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + raze)
        it.initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    class Board(val d: GameTestDriver, val p1Land: EntityId, val p2Land: EntityId, val p2Other: EntityId, val source: EntityId)

    // Player 1 razes player 2's land; player 2 copies it and may choose new targets.
    fun board(p1HasLand: Boolean = true): Board {
        val d = driver()
        val p1Land = if (p1HasLand) d.putLandOnBattlefield(d.player1, "Forest") else EntityId("none")
        val p2Land = d.putLandOnBattlefield(d.player2, "Forest")
        val p2Other = d.putLandOnBattlefield(d.player2, "Mountain")
        val id = d.putCardInHand(d.player1, raze.name)
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, id, listOf(d.player2, p2Land)).error shouldBe null
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyTargetSpellExecutor(predicates.conditions.amounts, TargetFinder(predicates)).execute(
            d.state, CopyTargetSpellEffect(EffectTarget.ContextTarget(0), copies = DynamicAmount.Fixed(1)),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(id)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        return Board(d, p1Land, p2Land, p2Other, id)
    }
    fun Board.decision() = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    fun Board.answer(slots: Map<Int, List<EntityId>>) =
        d.submit(SubmitDecision(d.player2, TargetsResponse(decision().id, slots)))
    fun Board.copyTargets() = d.state.stack
        .mapNotNull { d.state.getEntity(it) }
        .single { it.has<CopyOfComponent>() }
        .get<TargetsComponent>()!!.targets

    test("the land slot is offered per opponent, kept or changed") {
        val b = board()
        val q = b.decision()
        q.targetRequirements[1].legalTargetsByPrefix shouldBe mapOf(
            "" to listOf(b.p2Land, b.p2Other),
            b.d.player1.toString() to listOf(b.p1Land),
        )
        q.targetRequirements[1].prefixLength shouldBe 1
        q.legalTargets[1]!!.toSet() shouldBe setOf(b.p1Land, b.p2Land, b.p2Other)
        q.legalTargetsFor(1, mapOf(0 to listOf(b.d.player1))) shouldBe listOf(b.p1Land)
        q.legalTargetsFor(1, mapOf(0 to emptyList())) shouldBe listOf(b.p2Land, b.p2Other)
    }

    test("changing the opponent and the land together binds the land to the new opponent") {
        val b = board()
        val before = b.d.state
        (b.answer(mapOf(0 to listOf(b.d.player1), 1 to listOf(b.p2Other))).error != null) shouldBe true
        b.d.state shouldBe before
        b.answer(mapOf(0 to listOf(b.d.player1), 1 to listOf(b.p1Land))).error shouldBe null
        b.copyTargets() shouldBe listOf(ChosenTarget.Player(b.d.player1), ChosenTarget.Permanent(b.p1Land))
    }

    test("a kept opponent still binds a changed land") {
        val b = board()
        (b.answer(mapOf(0 to emptyList(), 1 to listOf(b.p1Land))).error != null) shouldBe true
        b.answer(mapOf(0 to emptyList(), 1 to listOf(b.p2Other))).error shouldBe null
        b.copyTargets() shouldBe listOf(ChosenTarget.Player(b.d.player2), ChosenTarget.Permanent(b.p2Other))
    }

    test("a changed opponent can't leave the kept land behind (CR 115.7d)") {
        val b = board()
        val before = b.d.state
        (b.answer(mapOf(0 to listOf(b.d.player1), 1 to emptyList())).error != null) shouldBe true
        b.d.state shouldBe before
    }

    test("an opponent with no land to take is not offered for a change") {
        val b = board(p1HasLand = false)
        val q = b.decision()
        q.legalTargets[0] shouldBe emptyList()
        q.targetRequirements[1].legalTargetsByPrefix!![""] shouldBe listOf(b.p2Land, b.p2Other)
    }

    test("the prefix table survives serialization of the paused state") {
        val b = board()
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        val decoded = json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), b.d.state))
        (decoded.pendingDecision as ChooseTargetsDecision).targetRequirements shouldBe b.decision().targetRequirements
    }

    test("independent copy targets carry no prefix table") {
        val d = driver()
        val bears = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val bolt = card("Test Independent Copy Bolt") {
            manaCost = "{R}"; typeLine = "Instant"
            spell { val t = target(TargetFilter.Creature); effect = Effects.DealDamage(2, t) }
        }
        d.registerCards(listOf(bolt))
        val id = d.putCardInHand(d.player1, bolt.name)
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, id, listOf(bears)).error shouldBe null
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyTargetSpellExecutor(predicates.conditions.amounts, TargetFinder(predicates)).execute(
            d.state, CopyTargetSpellEffect(EffectTarget.ContextTarget(0), copies = DynamicAmount.Fixed(1)),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(id)))
        )
        (result.state.pendingDecision as ChooseTargetsDecision).targetRequirements
            .all { it.legalTargetsByPrefix == null && it.prefixLength == null } shouldBe true
    }

    test("a copied triggered ability offers the land slot per chosen player") {
        val d = driver()
        val p1Land = d.putLandOnBattlefield(d.player1, "Forest")
        val p2Land = d.putLandOnBattlefield(d.player2, "Mountain")
        val requirements = listOf(
            TargetPlayer(id = "player"),
            TargetObject(filter = TargetFilter(GameObjectFilter.Land.targetPlayerControls(EffectTarget.BoundVariable("player")))),
        )
        val (abilityId, withAbility) = d.state.newEntity()
        val ability = TriggeredAbilityOnStackComponent(
            sourceId = p1Land, sourceName = "Test Trigger", controllerId = d.player1,
            effect = Effects.Destroy(EffectTarget.ContextTarget(1)), description = "Destroy target land that player controls",
        )
        val targets = listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(p2Land))
        d.replaceState(withAbility.withEntity(abilityId, ComponentContainer.of(ability,
            TargetsComponent.capture(withAbility, targets, requirements))).copy(stack = withAbility.stack + abilityId))
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyTargetTriggeredAbilityExecutor(TargetFinder(predicates)).execute(
            d.state, CopyTargetTriggeredAbilityEffect(EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(abilityId)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        val q = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        q.targetRequirements[1].legalTargetsByPrefix shouldBe mapOf(
            d.player1.toString() to listOf(p1Land), d.player2.toString() to listOf(p2Land),
        )
        (d.submit(SubmitDecision(d.player2, TargetsResponse(q.id,
            mapOf(0 to listOf(d.player1), 1 to listOf(p2Land))))).error != null) shouldBe true
        d.submit(SubmitDecision(d.player2, TargetsResponse(q.id,
            mapOf(0 to listOf(d.player1), 1 to listOf(p1Land))))).error shouldBe null
    }

    test("a fixed redirect reads the spell's other targets as they stand") {
        val d = driver()
        val p1Land = d.putLandOnBattlefield(d.player1, "Forest")
        val p2Land = d.putLandOnBattlefield(d.player2, "Forest")
        val p2Other = d.putLandOnBattlefield(d.player2, "Mountain")
        val id = d.putCardInHand(d.player1, raze.name)
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, id, listOf(d.player2, p2Land)).error shouldBe null
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        fun redirect(to: EntityId) = ChangeTargetExecutor(predicates, TargetFinder(predicates)).execute(
            d.state, ChangeTargetEffect(newTarget = EffectTarget.SpecificEntity(to)),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(id)))
        ).also { it.error shouldBe null; d.replaceState(it.state) }
        fun targets() = d.state.getEntity(id)!!.get<TargetsComponent>()!!.targets
        // Player 1's land isn't one "that player" (player 2) controls: nothing changes.
        redirect(p1Land)
        targets() shouldBe listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(p2Land))
        redirect(p2Other)
        targets() shouldBe listOf(ChosenTarget.Player(d.player2), ChosenTarget.Permanent(p2Other))
    }

    test("minimal and repaired selections follow the earlier slot's table") {
        val a = EntityId("a"); val b = EntityId("b"); val la = EntityId("la"); val lb = EntityId("lb")
        val q = ChooseTargetsDecision(
            id = "q", playerId = a, prompt = "", context = DecisionContext(phase = DecisionPhase.RESOLUTION),
            targetRequirements = listOf(
                TargetRequirementInfo(index = 0, description = "player"),
                TargetRequirementInfo(index = 1, description = "land",
                    legalTargetsByPrefix = mapOf("a" to listOf(la), "b" to listOf(lb))),
            ),
            legalTargets = mapOf(0 to listOf(a, b), 1 to listOf(lb, la)),
        )
        q.minimalSelection() shouldBe mapOf(0 to listOf(a), 1 to listOf(la))
        q.repairSlotsAfter(0, mapOf(0 to listOf(b), 1 to listOf(la))) shouldBe mapOf(0 to listOf(b), 1 to listOf(lb))
        q.repairSlotsAfter(0, mapOf(0 to listOf(a), 1 to listOf(la))) shouldBe mapOf(0 to listOf(a), 1 to listOf(la))
        // A kept land behind a changed player is changed too: keeping it may be illegal (CR 115.7d).
        q.repairSlotsAfter(0, mapOf(0 to listOf(b), 1 to emptyList())) shouldBe mapOf(0 to listOf(b), 1 to listOf(lb))
        q.repairSlotsAfter(0, mapOf(0 to emptyList(), 1 to emptyList())) shouldBe mapOf(0 to emptyList(), 1 to emptyList())
    }
})
