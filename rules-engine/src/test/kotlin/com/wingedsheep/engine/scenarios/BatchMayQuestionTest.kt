package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.YieldKind
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityIdentity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Simultaneous optional instances announce targets individually and consent only at resolution. */
class BatchMayQuestionTest : FunSpec({
    val pinger = card("Batch Pinger") {
        manaCost = "{1}"; typeLine = "Creature — Test"; power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
            val t = target(Targets.Any)
            effect = Effects.May(Effects.DealDamage(1, t))
        }
    }
    val bear = card("Batch Bear") {
        manaCost = "{1}"; typeLine = "Creature — Test"; power = 2; toughness = 2
    }
    fun game(count: Int, yield: YieldKind? = null): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(pinger, bear))
        d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(count) { d.putCreatureOnBattlefield(d.player1, pinger.name) }
        if (yield != null) d.replaceState(d.state.withYield(d.player1,
            AbilityIdentity(pinger.name, pinger.triggeredAbilities.single().id), yield))
        d.giveColorlessMana(d.player1, 1)
        val id = d.putCardInHand(d.player1, bear.name)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass().error shouldBe null
        return d
    }
    fun targetAll(d: GameTestDriver, count: Int, target: EntityId = d.player2) {
        repeat(count) {
            d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            d.submitTargetSelection(d.player1, listOf(target)).error shouldBe null
        }
        d.pendingDecision shouldBe null
        d.state.stack.size shouldBe count
        val chosen = if (target == d.player2) ChosenTarget.Player(target) else ChosenTarget.Permanent(target)
        d.state.stack.map { d.state.getEntity(it)!!.get<TargetsComponent>()!!.targets.single() }
            .all { it == chosen } shouldBe true
    }
    test("identical optional triggers are not asked or batched before targets and priority") {
        val d = game(2)
        targetAll(d, 2)
        d.bothPass().error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().context.targetIds shouldBe listOf(d.player2)
        d.submitYesNo(d.player1, true).error shouldBe null
        d.assertLifeTotal(d.player2, 19)
        d.state.stack.size shouldBe 1
        d.pendingDecision shouldBe null
        d.bothPass().error shouldBe null
        d.submitYesNo(d.player1, false).error shouldBe null
        d.assertLifeTotal(d.player2, 19)
        d.state.stack.size shouldBe 0
    }
    test("declining every instance still announces all targets and retains all response windows") {
        val d = game(3)
        targetAll(d, 3)
        repeat(3) {
            d.bothPass().error shouldBe null
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().context.targetIds shouldBe listOf(d.player2)
            d.submitYesNo(d.player1, false).error shouldBe null
        }
        d.assertLifeTotal(d.player2, 20)
        d.state.stack.size shouldBe 0
    }
    test("remembered no suppresses consent at resolution but never target announcements") {
        val d = game(2, YieldKind.ALWAYS_ANSWER_NO)
        targetAll(d, 2)
        repeat(2) {
            val result = d.bothPass()
            result.error shouldBe null
            result.events.any { it is AbilityAutoAnsweredEvent && !it.answer } shouldBe true
            d.pendingDecision shouldBe null
        }
        d.assertLifeTotal(d.player2, 20)
    }
    test("remembered yes resolves each instance after its own priority window") {
        val d = game(2, YieldKind.ALWAYS_ANSWER_YES)
        targetAll(d, 2)
        repeat(2) {
            val result = d.bothPass()
            result.error shouldBe null
            result.events.any { it is AbilityAutoAnsweredEvent && it.answer } shouldBe true
            d.pendingDecision shouldBe null
        }
        d.assertLifeTotal(d.player2, 18)
    }
    test("all invalid targets fizzle before remembered answers or consent") {
        val d = game(2, YieldKind.ALWAYS_ANSWER_YES)
        val target = d.findPermanent(d.player1, bear.name)!!
        targetAll(d, 2, target)
        d.moveToGraveyard(target)
        repeat(2) {
            val result = d.bothPass()
            result.error shouldBe null
            result.events.any { it is AbilityAutoAnsweredEvent } shouldBe false
            d.pendingDecision shouldBe null
        }
    }
    test("resolution consent identifies every locked player and permanent target after serialization") {
        val pair = card("Optional Pair Pinger") {
            manaCost = "{1}"; typeLine = "Creature — Test"; power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
                val first = target(Targets.Any)
                val second = target(Targets.Any)
                effect = Effects.May(Effects.Composite(listOf(
                    Effects.DealDamage(1, first), Effects.DealDamage(1, second)
                )))
            }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(pair, bear))
        d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val permanent = d.putCreatureOnBattlefield(d.player1, pair.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, bear.name)).error shouldBe null
        d.bothPass().error shouldBe null
        d.submitMultiTargetSelection(d.player1, mapOf(0 to listOf(d.player2), 1 to listOf(permanent))).error shouldBe null
        d.bothPass().error shouldBe null
        val json = kotlinx.serialization.json.Json {
            serializersModule = engineSerializersModule
            classDiscriminator = "type"
        }
        val decision = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        val restored = json.decodeFromString<PendingDecision>(json.encodeToString<PendingDecision>(decision))
        restored.context.targetIds shouldBe listOf(d.player2, permanent)
        d.submitYesNo(d.player1, true).error shouldBe null
        d.assertLifeTotal(d.player2, 19)
    }
    test("one optional instance also targets first and asks only after passing priority") {
        val d = game(1)
        targetAll(d, 1)
        d.bothPass().error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().context.targetIds shouldBe listOf(d.player2)
    }
})
