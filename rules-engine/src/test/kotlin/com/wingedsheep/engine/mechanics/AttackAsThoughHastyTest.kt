package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AttackAsThoughHastyTest : FunSpec({
    val runner = card("Permission Test Runner") {
        manaCost = "{1}"; typeLine = "Creature — Human"; power = 2; toughness = 2
        activatedAbility { cost = Costs.Tap; effect = Effects.GainLife(1) }
    }
    val wall = card("Permission Test Wall") {
        manaCost = "{1}"; typeLine = "Creature — Wall"; power = 2; toughness = 2
        keywords(Keyword.DEFENDER)
    }
    val hasteControl = card("Permission Haste Control") {
        manaCost = "{1}"; typeLine = "Creature — Human"; power = 2; toughness = 2
        keywords(Keyword.HASTE)
    }
    val banner = card("Permission Banner") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = CanAttackAsThoughHasty(GroupFilter.AllCreaturesYouControl) }
    }
    val smallBanner = card("Small Permission Banner") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = CanAttackAsThoughHasty(
            GroupFilter(GameObjectFilter.Creature.youControl().powerAtMost(1))) }
    }
    val untappedHumanBanner = card("Untapped Human Permission Banner") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = CanAttackAsThoughHasty(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Human").untapped())) }
    }
    val conditionalBanner = card("Untapped Permission Banner") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = ConditionalStaticAbility(
            CanAttackAsThoughHasty(GroupFilter.AllCreaturesYouControl), Conditions.SourceIsUntapped) }
    }
    val temporaryGrant = card("Temporary Attack Permission") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature)
            effect = Effects.GrantStaticAbility(CanAttackAsThoughHasty(), t) }
    }
    val mute = card("Mute Permission Source") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) }
    }
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(runner, wall, hasteControl, banner, smallBanner, untappedHumanBanner, conditionalBanner, temporaryGrant, mute))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        while (activePlayer != player1) { bothPass(); passPriorityUntil(Step.PRECOMBAT_MAIN) }
    }
    fun List<LegalAction>.attackers() = firstOrNull { it.actionType == "DeclareAttackers" }?.validAttackers.orEmpty()

    test("attack-only permission is enumerated and enforced while tap activation stays blocked") {
        val d = driver()
        val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        d.putPermanentOnBattlefield(d.player1, banner.name)
        d.state.getEntity(id)!!.has<SummoningSicknessComponent>() shouldBe true
        d.state.projectedState.hasKeyword(id, Keyword.HASTE) shouldBe false
        d.legalActions(d.player1).filter { it.affordable }.any { (it.action as? ActivateAbility)?.sourceId == id } shouldBe false
        val rejected = d.submitExpectFailure(ActivateAbility(d.player1, id, runner.script.activatedAbilities.single().id))
        rejected.error shouldBe "This creature has summoning sickness"
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        (id in d.legalActions(d.player1).attackers()) shouldBe true
        d.submitSuccess(DeclareAttackers(d.player1, mapOf(id to d.player2)))
    }
    test("without permission the same fresh creature cannot attack") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        d.putCreatureOnBattlefield(d.player1, hasteControl.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        (id in d.legalActions(d.player1).attackers()) shouldBe false
        d.submitExpectFailure(DeclareAttackers(d.player1, mapOf(id to d.player2)))
    }
    test("a granted permission expires when its source leaves") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val source = d.putPermanentOnBattlefield(d.player1, banner.name)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
        d.moveToGraveyard(source)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
    }
    test("phasing the granting source suspends the permission") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val source = d.putPermanentOnBattlefield(d.player1, banner.name)
        d.replaceState(d.state.updateEntity(source) { it.with(PhasedOutComponent(d.player1)) })
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
        d.replaceState(d.state.updateEntity(source) { it.without<PhasedOutComponent>() })
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
    }
    test("controller-scoped permission does not cover opponents") {
        val d = driver(); val own = d.putCreatureOnBattlefield(d.player1, runner.name)
        val other = d.putCreatureOnBattlefield(d.player2, runner.name)
        d.putPermanentOnBattlefield(d.player1, banner.name)
        d.state.projectedState.canAttackAsThoughHasty(own) shouldBe true
        d.state.projectedState.canAttackAsThoughHasty(other) shouldBe false
    }
    test("attack permission does not bypass defender or tapped restrictions") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val wallId = d.putCreatureOnBattlefield(d.player1, wall.name)
        d.putCreatureOnBattlefield(d.player1, hasteControl.name)
        d.putPermanentOnBattlefield(d.player1, banner.name); d.tapPermanent(id)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        (id in d.legalActions(d.player1).attackers()) shouldBe false
        (wallId in d.legalActions(d.player1).attackers()) shouldBe false
        d.submitExpectFailure(DeclareAttackers(d.player1, mapOf(id to d.player2)))
        d.submitExpectFailure(DeclareAttackers(d.player1, mapOf(wallId to d.player2)))
    }
    test("conditional permissions track their current source condition") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val source = d.putPermanentOnBattlefield(d.player1, conditionalBanner.name)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
        d.tapPermanent(source)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
        d.untapPermanent(source)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
    }
    test("removing the source ability suspends its permission regardless of timestamps") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val source = d.putPermanentOnBattlefield(d.player1, banner.name)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
        val spell = d.putCardInHand(d.player1, mute.name)
        d.castSpell(d.player1, spell, listOf(source)).error shouldBe null
        d.bothPass()
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
    }
    test("runtime granted permission expires at cleanup") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val spell = d.putCardInHand(d.player1, temporaryGrant.name)
        d.castSpell(d.player1, spell, listOf(id)).error shouldBe null
        d.bothPass()
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
    }
    test("a face-down creature retains its externally granted attack permission") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        d.replaceState(d.state.updateEntity(id) { it.with(FaceDownComponent) })
        val spell = d.putCardInHand(d.player1, temporaryGrant.name)
        d.castSpell(d.player1, spell, listOf(id)).error shouldBe null
        d.bothPass()
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        (id in d.legalActions(d.player1).attackers()) shouldBe true
        d.submitSuccess(DeclareAttackers(d.player1, mapOf(id to d.player2)))
    }
    test("a face-down source cannot supply its printed attack permission") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        val source = d.putPermanentOnBattlefield(d.player1, banner.name)
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) })
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
    }
    test("the projected permission survives persisted-state round trip") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        d.putPermanentOnBattlefield(d.player1, banner.name)
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        val restored = json.decodeFromString<GameState>(json.encodeToString(d.state))
        restored.projectedState.canAttackAsThoughHasty(id) shouldBe true
        restored.projectedState.hasKeyword(id, Keyword.HASTE) shouldBe false
    }
    test("composed subtype and state filters preserve every predicate") {
        val d = driver()
        val human = d.putCreatureOnBattlefield(d.player1, runner.name)
        val otherType = d.putCreatureOnBattlefield(d.player1, wall.name)
        d.putPermanentOnBattlefield(d.player1, untappedHumanBanner.name)
        d.state.projectedState.canAttackAsThoughHasty(human) shouldBe true
        d.state.projectedState.canAttackAsThoughHasty(otherType) shouldBe false
        d.tapPermanent(human)
        d.state.projectedState.canAttackAsThoughHasty(human) shouldBe false
        d.untapPermanent(human)
        d.state.projectedState.canAttackAsThoughHasty(human) shouldBe true
    }
    test("numeric filters see final projected power") {
        val d = driver(); val id = d.putCreatureOnBattlefield(d.player1, runner.name)
        d.putPermanentOnBattlefield(d.player1, smallBanner.name)
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe false
        val shrink = com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect(
            id = EntityId.generate(), sourceId = id, controllerId = d.player1, timestamp = d.state.timestamp,
            effect = com.wingedsheep.engine.mechanics.layers.FloatingEffectData(
                layer = com.wingedsheep.engine.mechanics.layers.Layer.POWER_TOUGHNESS,
                sublayer = com.wingedsheep.engine.mechanics.layers.Sublayer.MODIFICATIONS,
                modification = com.wingedsheep.engine.mechanics.layers.SerializableModification.ModifyPowerToughness(-1, 0),
                affectedEntities = setOf(id)),
            duration = Duration.EndOfTurn)
        d.replaceState(d.state.copy(floatingEffects = d.state.floatingEffects + shrink))
        d.state.projectedState.getPower(id) shouldBe 1
        d.state.projectedState.canAttackAsThoughHasty(id) shouldBe true
    }
})
