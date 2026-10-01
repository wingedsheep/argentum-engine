package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.core.untapLimitChoices
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConditionalUntapLimitsTest : FunSpec({
    val cap = card("Conditional Cap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { condition = Conditions.SourceIsUntapped
            ability = UntapLimitPerStep(GameObjectFilter.Land, 1) }
    }
    val nested = card("Nested Cap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { condition = Conditions.SourceIsUntapped
            ability = CompositeStaticAbility(listOf(ConditionalStaticAbility(
                UntapLimitPerStep(GameObjectFilter.Land, 0), Conditions.SourceIsUntapped))) }
    }
    val forestCap = card("Forest Cap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Land.withSubtype("Forest"), 1) }
    }
    val ownCap = card("Own Cap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Land.youControl(), 1) }
    }
    val artifactCap = card("Artifact Cap") {
        manaCost = "{1}"; typeLine = "Enchantment"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Artifact, 1) }
    }
    val widget = card("Cap Widget") { manaCost = "{1}"; typeLine = "Artifact" }
    val artifactLand = card("Cap Artifact Land") { typeLine = "Artifact Land" }
    val seedborn = card("Cap Other Untap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = UntapDuringOtherUntapSteps }
    }
    val mute = card("Mute Cap") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }
    val turnToArtifact = card("Cap Type Change") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent)
            effect = Effects.BecomeArtifact(t, loseAllAbilities = false) }
    }
    val steal = card("Cap Control Change") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.GainControl(t) }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(cap, nested, forestCap, ownCap, artifactCap, widget, artifactLand, seedborn, mute, turnToArtifact, steal))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun lands(d: GameTestDriver, player: EntityId = d.activePlayer!!) =
        List(3) { d.putPermanentOnBattlefield(player, "Forest").also(d::tapPermanent) }
    fun limits(d: GameTestDriver, ids: List<EntityId>) =
        untapLimitChoices(d.state, d.cardRegistry, d.services.predicateEvaluator, ids)

    test("conditional and nested composite caps track the pre-untap source condition") {
        val d = driver(); val ids = lands(d)
        val source = d.putPermanentOnBattlefield(d.activePlayer!!, nested.name)
        limits(d, ids).single().max shouldBe 0
        d.tapPermanent(source)
        limits(d, ids).isEmpty() shouldBe true
        d.untapPermanent(source)
        limits(d, ids).single().max shouldBe 0
    }
    test("face-down printed caps and phased-out sources do not function") {
        val d = driver(); val ids = lands(d)
        val source = d.putPermanentOnBattlefield(d.activePlayer!!, cap.name)
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) })
        limits(d, ids).isEmpty() shouldBe true
        d.replaceState(d.state.updateEntity(source) { it.without<FaceDownComponent>().with(PhasedOutComponent(d.activePlayer!!)) })
        limits(d, ids).isEmpty() shouldBe true
        d.replaceState(d.state.updateEntity(source) { it.without<PhasedOutComponent>() })
        limits(d, ids).single().max shouldBe 1
    }
    test("removing a cap source's abilities stops the restriction") {
        val d = driver(); val ids = lands(d)
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, cap.name)
        val spell = d.putCardInHand(me, mute.name)
        d.castSpell(me, spell, listOf(source)).error shouldBe null
        d.bothPass()
        d.state.projectedState.hasLostAllAbilities(source) shouldBe true
        limits(d, ids).isEmpty() shouldBe true
    }
    test("subtype and controller predicates use the source context") {
        val d = driver(); val me = d.activePlayer!!
        val own = lands(d); val opposing = lands(d, d.getOpponent(me))
        d.putPermanentOnBattlefield(me, ownCap.name)
        limits(d, own + opposing).single().matchingPermanents.toSet() shouldBe own.toSet()
    }
    test("printed subtype text changes alter the capped group") {
        val d = driver(); val me = d.activePlayer!!
        val forests = lands(d)
        val islands = List(2) { d.putPermanentOnBattlefield(me, "Island").also(d::tapPermanent) }
        val source = d.putPermanentOnBattlefield(me, forestCap.name)
        d.replaceState(d.state.updateEntity(source) { it.with(TextReplacementComponent(listOf(
            TextReplacement("Forest", "Island", TextReplacementCategory.BASIC_LAND_TYPE)))) })
        limits(d, forests + islands).single().matchingPermanents.toSet() shouldBe islands.toSet()
    }
    test("duration-gated grants work on face-down holders and are not text-changed") {
        val d = driver(); val me = d.activePlayer!!; val ids = lands(d)
        val source = d.putPermanentOnBattlefield(me, widget.name)
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) }.copy(
            grantedStaticAbilities = listOf(GrantedStaticAbility(source,
                UntapLimitPerStep(GameObjectFilter.Land.withSubtype("Forest"), 1), Duration.WhileSourceTapped(), source))))
        limits(d, ids).isEmpty() shouldBe true
        d.tapPermanent(source)
        d.replaceState(d.state.updateEntity(source) { it.with(TextReplacementComponent(listOf(
            TextReplacement("Forest", "Island", TextReplacementCategory.BASIC_LAND_TYPE)))) })
        limits(d, ids).single().matchingPermanents.toSet() shouldBe ids.toSet()
    }
    test("duplicate and overlapping caps never require an impossible selection count") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, cap.name)
        d.putPermanentOnBattlefield(me, forestCap.name)
        val ids = lands(d, opponent)
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).minSelections shouldBe 2
        d.submitCardSelection(opponent, ids.take(2)).error shouldBe null
        d.state.getEntity(ids.last())!!.has<TappedComponent>() shouldBe false
    }
    test("partially overlapping caps share a kept permanent") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, cap.name)
        d.putPermanentOnBattlefield(me, artifactCap.name)
        val shared = d.putPermanentOnBattlefield(opponent, artifactLand.name).also(d::tapPermanent)
        val land = d.putPermanentOnBattlefield(opponent, "Forest").also(d::tapPermanent)
        val artifact = d.putPermanentOnBattlefield(opponent, widget.name).also(d::tapPermanent)
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).minSelections shouldBe 1
        d.submitCardSelection(opponent, listOf(shared)).error shouldBe null
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
        d.state.getEntity(artifact)!!.has<TappedComponent>() shouldBe false
    }
    test("each disjoint cap is independently validated even when the lower bound is met") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, cap.name)
        d.putPermanentOnBattlefield(me, artifactCap.name)
        val ids = lands(d, opponent)
        val artifacts = List(2) { d.putPermanentOnBattlefield(opponent, widget.name).also(d::tapPermanent) }
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).minSelections shouldBe 2
        d.submitCardSelection(opponent, ids.take(2)).error!!.contains("restricted permanents") shouldBe true
        (d.pendingDecision is SelectCardsDecision) shouldBe true
        d.submitCardSelection(opponent, ids.take(2) + artifacts.take(1)).error shouldBe null
        d.state.getEntity(ids.last())!!.has<TappedComponent>() shouldBe false
        d.state.getEntity(artifacts.last())!!.has<TappedComponent>() shouldBe false
    }
    test("other-player untaps are unaffected by active-player caps") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, cap.name)
        d.putPermanentOnBattlefield(me, seedborn.name)
        val ids = lands(d)
        d.passPriorityUntil(Step.UPKEEP)
        ids.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe false }
    }
    test("cap filter reads projected types after a type change") {
        val d = driver(); val me = d.activePlayer!!; val ids = lands(d)
        d.putPermanentOnBattlefield(me, artifactCap.name)
        d.putPermanentOnBattlefield(me, widget.name)
        val spell = d.putCardInHand(me, turnToArtifact.name)
        d.castSpell(me, spell, listOf(ids.first())).error shouldBe null
        d.bothPass()
        d.state.projectedState.hasType(ids.first(), "ARTIFACT") shouldBe true
        val candidate = d.putPermanentOnBattlefield(me, widget.name)
        limits(d, ids + candidate).single().matchingPermanents.toSet() shouldBe setOf(ids.first(), candidate)
    }
    test("controller-relative filters follow projected control changes") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val own = lands(d); val opposing = lands(d, opponent)
        val source = d.putPermanentOnBattlefield(opponent, ownCap.name)
        val spell = d.putCardInHand(me, steal.name)
        d.castSpell(me, spell, listOf(source)).error shouldBe null
        d.bothPass()
        limits(d, own + opposing).single().matchingPermanents.toSet() shouldBe own.toSet()
    }
    test("a zero cap keeps every matching permanent tapped") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, nested.name); val ids = lands(d, opponent)
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).minSelections shouldBe 3
        d.submitCardSelection(opponent, ids).error shouldBe null
        ids.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
    }
    test("a cap phased in before untapping restricts that same step") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(opponent, cap.name)
        d.replaceState(d.state.updateEntity(source) { it.with(PhasedOutComponent(opponent)) })
        val ids = lands(d, opponent)
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).minSelections shouldBe 2
        d.submitCardSelection(opponent, ids.take(2)).error shouldBe null
        d.state.getEntity(ids.last())!!.has<TappedComponent>() shouldBe false
    }
    test("serialized conditional cap and paused selection retain their semantics") {
        val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, cap.name); val ids = lands(d, opponent)
        d.passPriorityUntil(Step.UNTAP)
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitCardSelection(opponent, ids.take(2)).error shouldBe null
        d.state.getEntity(ids.last())!!.has<TappedComponent>() shouldBe false
    }
})
