package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentUntapStepSkipTest : FunSpec({
    val all = card("Standing Untap Skip") { typeLine = "Artifact"; staticAbility { ability = SkipUntapStep() } }
    val own = card("Own Untap Skip") { typeLine = "Artifact"; staticAbility { ability = SkipUntapStep(Player.You) } }
    val enemy = card("Enemy Untap Skip") { typeLine = "Artifact"; staticAbility { ability = SkipUntapStep(Player.EachOpponent) } }
    val conditional = card("Conditional Untap Skip") { typeLine = "Artifact"
        staticAbility { condition = Conditions.SourceIsUntapped
            ability = CompositeStaticAbility(listOf(SkipUntapStep())) } }
    val mute = card("Mute Skip") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) } }
    val steal = card("Steal Skip") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.GainControl(t) } }
    val buff = card("Untap Duration Buff") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 2, t, Duration.UntilAfterAffectedControllersNextUntap) } }
    val optionalUntap = card("Skip Optional Untap") { typeLine = "Artifact"
        flags(com.wingedsheep.sdk.core.AbilityFlag.MAY_NOT_UNTAP) }
    val seedborn = card("Skip Other Untap") { typeLine = "Artifact"; staticAbility { ability = UntapDuringOtherUntapSteps } }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(all, own, enemy, conditional, mute, seedborn, steal, buff, optionalUntap))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun skips(d: GameTestDriver, p: com.wingedsheep.sdk.model.EntityId) =
        skipsUntapStep(d.state, d.cardRegistry, d.services.predicateEvaluator, p)
    fun untap(d: GameTestDriver) = d.services.turnManager.beginningPhaseManager.performUntapStep(d.state)

    test("standing scope uses the current projected controller and all opponents") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(me, own.name)
        skips(d, me) shouldBe true; skips(d, other) shouldBe false
        d.replaceState(d.state.updateEntity(source) { it.without<FaceDownComponent>().with(PhasedOutComponent(me)) })
        skips(d, me) shouldBe false
        d.replaceState(d.state.updateEntity(source) { it.without<PhasedOutComponent>() })
        d.putPermanentOnBattlefield(me, enemy.name)
        skips(d, other) shouldBe true
    }
    test("conditional nested statics stop applying when their condition becomes false") {
        val d = driver(); val source = d.putPermanentOnBattlefield(d.activePlayer!!, conditional.name)
        skips(d, d.activePlayer!!) shouldBe true; d.tapPermanent(source)
        skips(d, d.activePlayer!!) shouldBe false
    }
    test("face-down printed sources do not impose the skip") {
        val d = driver(); val source = d.putPermanentOnBattlefield(d.activePlayer!!, all.name)
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) })
        skips(d, d.activePlayer!!) shouldBe false
    }
    test("ability removal suppresses a printed restriction") {
        val d = driver(); val me = d.activePlayer!!; val source = d.putPermanentOnBattlefield(me, all.name)
        d.castSpell(me, d.putCardInHand(me, mute.name), listOf(source)).error shouldBe null
        d.bothPass(); d.state.projectedState.hasLostAllAbilities(source) shouldBe true
        skips(d, me) shouldBe false
    }
    test("skip suppresses every permanent type and other-player untaps") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, all.name)
        val ids = listOf(d.putPermanentOnBattlefield(me, "Forest"),
            d.putCreatureOnBattlefield(me, "Grizzly Bears"), d.putPermanentOnBattlefield(other, seedborn.name))
        ids.forEach(d::tapPermanent)
        val result = untap(d); result.error shouldBe null
        ids.forEach { result.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
        result.events.filterIsInstance<UntappedEvent>().isEmpty() shouldBe true
    }
    test("skip suppresses phasing and preserves exert and next-untap markers") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, all.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::tapPermanent)
        d.replaceState(d.state.updateEntity(bear) { it.with(ExertedComponent).with(PhasedOutComponent(me)) }
            .updateEntity(me) { it.with(SkipUntapComponent()).with(SkipNextUntapStepComponent()) })
        val result = untap(d); d.replaceState(result.state)
        val skipChoice = d.pendingDecision as ChooseOptionDecision
        d.submitDecision(me, OptionChosenResponse(skipChoice.id, 1)).error shouldBe null
        d.replaceState(d.services.turnManager.finishUntapStep(d.state.copy(step = Step.UNTAP), me, setOf(me), emptySet()).state)
        d.state.step shouldBe Step.UPKEEP
        d.state.getEntity(bear)!!.has<PhasedOutComponent>() shouldBe true
        d.state.getEntity(bear)!!.has<ExertedComponent>() shouldBe true
        d.state.getEntity(me)!!.has<SkipUntapComponent>() shouldBe true
        d.state.getEntity(me)!!.has<SkipNextUntapStepComponent>() shouldBe true
    }
    test("grants honor duration and survive state serialization on face-down holders") {
        val d = driver(); val me = d.activePlayer!!; val source = d.putPermanentOnBattlefield(me, "Forest")
        d.replaceState(d.state.updateEntity(source) { it.with(FaceDownComponent) }.copy(
            grantedStaticAbilities = listOf(GrantedStaticAbility(source, SkipUntapStep(), Duration.WhileSourceTapped(), source))))
        skips(d, me) shouldBe false; d.tapPermanent(source); skips(d, me) shouldBe true
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        skips(d, me) shouldBe true; d.untapPermanent(source); skips(d, me) shouldBe false
    }
    test("player-held grants skip only that player's step") {
        val d = driver(); val me = d.activePlayer!!
        d.replaceState(d.state.copy(grantedStaticAbilities = listOf(
            GrantedStaticAbility(me, SkipUntapStep(Player.You), Duration.EndOfTurn))))
        skips(d, me) shouldBe true; skips(d, d.getOpponent(me)) shouldBe false
    }
    test("a skip affecting one shared-turn teammate skips both teammates' untap") {
        val d = GameTestDriver(); d.registerCards(TestCards.all + own)
        val players = d.initMultiplayer(List(4) { Deck.of("Forest" to 40) },
            format = com.wingedsheep.sdk.core.Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)))
        d.putPermanentOnBattlefield(players[0], own.name)
        val ids = players.take(2).map { d.putPermanentOnBattlefield(it, "Forest").also(d::tapPermanent) }
        val result = untap(d)
        ids.forEach { result.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
    }

    test("standing skip persists across turns and duplicate sources until both leave") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val sources = List(2) { d.putPermanentOnBattlefield(me, all.name) }
        val land = d.putPermanentOnBattlefield(other, "Forest").also(d::tapPermanent)
        repeat(3) { d.passPriorityUntil(Step.UPKEEP); d.passPriorityUntil(Step.PRECOMBAT_MAIN) }
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
        d.replaceState(d.state.updateEntity(sources[0]) { it.with(PhasedOutComponent(me)) })
        skips(d, other) shouldBe true
        d.replaceState(d.state.updateEntity(sources[1]) { it.with(PhasedOutComponent(me)) })
        skips(d, other) shouldBe false
    }
    test("skipped untap leaves day-night designation unchanged") {
        val d = driver(); val me = d.activePlayer!!; d.putPermanentOnBattlefield(me, all.name)
        d.replaceState(d.state.copy(dayNight = com.wingedsheep.sdk.core.DayNight.DAY,
            previousTurnActiveTeamSpellCounts = mapOf(d.getOpponent(me) to 0)))
        untap(d).state.dayNight shouldBe com.wingedsheep.sdk.core.DayNight.DAY
    }
    test("inserted beginning phase omits untap event but retains upkeep and waiting durations") {
        val d = driver(); val me = d.activePlayer!!; d.putPermanentOnBattlefield(me, all.name)
        val land = d.putPermanentOnBattlefield(me, "Forest").also(d::tapPermanent)
        d.replaceState(d.state.copy(step = Step.POSTCOMBAT_MAIN).updateEntity(me) {
            it.with(AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.BEGINNING))))
        })
        val result = d.services.turnManager.advanceStep(d.state)
        result.state.step shouldBe Step.UPKEEP
        result.events.filterIsInstance<StepChangedEvent>().map { it.newStep }.contains(Step.UNTAP) shouldBe false
        result.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
    }

    test("control-changing effects move the you-scoped restriction") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val source = d.putPermanentOnBattlefield(other, own.name)
        skips(d, me) shouldBe false
        d.castSpell(me, d.putCardInHand(me, steal.name), listOf(source)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getController(source) shouldBe me
        skips(d, me) shouldBe true; skips(d, other) shouldBe false
    }
    test("end-of-turn grant expires before the following untap") {
        val d = driver(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, "Forest")
        d.replaceState(d.state.copy(grantedStaticAbilities = listOf(
            GrantedStaticAbility(source, SkipUntapStep(), Duration.EndOfTurn))))
        val land = d.putPermanentOnBattlefield(d.getOpponent(me), "Forest").also(d::tapPermanent)
        skips(d, me) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.grantedStaticAbilities.isEmpty() shouldBe true
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
    }

    test("until-after-next-untap duration waits for the first step that occurs") {
        val d = driver(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, all.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.castSpell(me, d.putCardInHand(me, buff.name), listOf(bear)).error shouldBe null
        d.bothPass()
        repeat(2) { d.passPriorityUntil(Step.UPKEEP); d.passPriorityUntil(Step.PRECOMBAT_MAIN) }
        d.state.projectedState.getPower(bear) shouldBe 4
        d.replaceState(d.state.updateEntity(source) { it.with(PhasedOutComponent(me)) })
        repeat(2) { d.passPriorityUntil(Step.UPKEEP); d.passPriorityUntil(Step.PRECOMBAT_MAIN) }
        d.state.projectedState.getPower(bear) shouldBe 2
    }

    test("pre-untap skip snapshot survives a serialized untap choice after the source phases in") {
        val d = driver(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, all.name)
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.castSpell(me, d.putCardInHand(me, buff.name), listOf(bear)).error shouldBe null
        d.bothPass()
        d.putPermanentOnBattlefield(me, optionalUntap.name).also(d::tapPermanent)
        d.replaceState(d.state.updateEntity(source) { it.with(PhasedOutComponent(me)) })
        d.passPriorityUntil(Step.UPKEEP); d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision as SelectCardsDecision).playerId shouldBe me
        d.state.getEntity(source)!!.has<PhasedOutComponent>() shouldBe false
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitCardSelection(me, emptyList()).error shouldBe null
        d.state.step shouldBe Step.UPKEEP
        d.state.projectedState.getPower(bear) shouldBe 2
    }

    test("choosing a pending replacement consumes only one skip and survives serialization") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, all.name)
        val land = d.putPermanentOnBattlefield(other, "Forest").also(d::tapPermanent)
        d.replaceState(d.state.updateEntity(other) { it.with(SkipNextUntapStepComponent(2)) })
        d.passPriorityUntil(Step.UNTAP)
        val question = d.pendingDecision as ChooseOptionDecision
        question.playerId shouldBe other
        d.submitDecision(other, OptionChosenResponse(question.id, 99)).error.isNullOrEmpty() shouldBe false
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        d.submitDecision(other, OptionChosenResponse(question.id, 0)).error shouldBe null
        d.state.step shouldBe Step.UPKEEP
        d.state.getEntity(other)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 1
        d.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
    }
    test("choosing the standing replacement preserves every pending skip") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, all.name)
        d.replaceState(d.state.updateEntity(other) { it.with(SkipNextUntapStepComponent(2)) })
        d.passPriorityUntil(Step.UNTAP)
        val question = d.pendingDecision as ChooseOptionDecision
        d.submitDecision(other, OptionChosenResponse(question.id, 1)).error shouldBe null
        d.state.step shouldBe Step.UPKEEP
        d.state.getEntity(other)!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 2
    }
    test("shared-team ordering spends only the selected teammate's pending replacement") {
        val d = GameTestDriver(); d.registerCards(TestCards.all + own)
        val players = d.initMultiplayer(List(4) { Deck.of("Forest" to 40) },
            format = com.wingedsheep.sdk.core.Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)))
        d.putPermanentOnBattlefield(players[0], own.name)
        d.replaceState(d.state.updateEntity(players[0]) { it.with(SkipNextUntapStepComponent(2)) }
            .updateEntity(players[1]) { it.with(SkipNextUntapStepComponent(3)) })
        val result = untap(d); d.replaceState(result.state)
        val question = d.pendingDecision as ChooseOptionDecision
        question.options.size shouldBe 3
        d.submitDecision(players[0], OptionChosenResponse(question.id, 1)).error shouldBe null
        d.state.getEntity(players[0])!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 2
        d.state.getEntity(players[1])!!.get<SkipNextUntapStepComponent>()?.steps shouldBe 2
    }

    test("a skipped untap does not prevent creatures becoming ready on their controller's turn") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, all.name)
        val bear = d.putCreatureOnBattlefield(other, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(bear) { it.with(SummoningSicknessComponent) })
        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(bear)!!.has<SummoningSicknessComponent>() shouldBe false
    }

})
