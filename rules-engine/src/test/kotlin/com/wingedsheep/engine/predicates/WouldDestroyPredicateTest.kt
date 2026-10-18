package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.WouldDestroy] — "a spell that would
 * destroy a land you control" (Equinox). Each case stamps a stack object carrying an arbitrary
 * effect and its chosen targets, then asks whether it would destroy a land the chooser controls
 * if it resolved right now. Pinned from the Equinox rulings: only destruction the object performs
 * directly and unconditionally counts — never a "may", exile, damage, or a mode not chosen.
 */
class WouldDestroyPredicateTest : FunSpec({

    val landYouControl = GameObjectFilter.Any.wouldDestroy(GameObjectFilter.Land.youControl())

    data class Board(
        val driver: GameTestDriver,
        val a: EntityId,
        val b: EntityId,
        val landA: EntityId,
        val landB: EntityId,
        val bearsA: EntityId,
    )

    fun board(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val a = driver.player1
        val b = driver.getOpponent(a)
        val landA = driver.putLandOnBattlefield(a, "Plains")
        val landB = driver.putLandOnBattlefield(b, "Forest")
        val bearsA = driver.putPermanentOnBattlefield(a, "Grizzly Bears")
        return Board(driver, a, b, landA, landB, bearsA)
    }

    fun Board.stackObject(
        effect: Effect,
        targets: List<ChosenTarget> = emptyList(),
        chosenModes: List<Int> = emptyList(),
        modeTargets: List<List<ChosenTarget>> = emptyList(),
    ): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        val ability = ActivatedAbilityOnStackComponent(
            sourceId = bearsA, sourceName = "Test", controllerId = b, effect = effect,
            chosenModes = chosenModes, modeTargetsOrdered = modeTargets,
        )
        return driver.state.withEntity(id, ComponentContainer().with(ability).with(TargetsComponent(targets))) to id
    }

    fun Pair<GameState, EntityId>.wouldDestroyLandOf(chooser: EntityId): Boolean =
        PredicateEvaluator(cardRegistry = null).matches(
            first, first.projectedState, second, landYouControl, PredicateContext(controllerId = chooser)
        )

    test("a destroy aimed at a land you control matches; 'you' is the chooser") {
        val board = board()
        val obj = board.stackObject(Effects.Destroy(EffectTarget.ContextTarget(0)), listOf(ChosenTarget.Permanent(board.landA)))
        obj.wouldDestroyLandOf(board.a) shouldBe true
        withClue("the land's controller is A, so for B it isn't 'a land you control'") {
            obj.wouldDestroyLandOf(board.b) shouldBe false
        }
    }

    test("a destroy aimed at something other than a land doesn't match, even if it could have been a land") {
        val board = board()
        val obj = board.stackObject(Effects.Destroy(EffectTarget.ContextTarget(0)), listOf(ChosenTarget.Permanent(board.bearsA)))
        obj.wouldDestroyLandOf(board.a) shouldBe false
    }

    test("'destroy all lands' matches for every player who controls a land") {
        val board = board()
        val obj = board.stackObject(Effects.DestroyAll(GameObjectFilter.Land))
        obj.wouldDestroyLandOf(board.a) shouldBe true
        obj.wouldDestroyLandOf(board.b) shouldBe true
        withClue("a sweep whose group holds none of your lands doesn't match") {
            board.stackObject(Effects.DestroyAll(GameObjectFilter.Land.opponentControls()))
                .wouldDestroyLandOf(board.b) shouldBe false
        }
    }

    test("a per-permanent group iteration that destroys each land matches") {
        val board = board()
        val obj = board.stackObject(
            Effects.ForEachInGroup(GroupFilter.AllLands, Effects.Destroy(EffectTarget.IterationEntity))
        )
        obj.wouldDestroyLandOf(board.a) shouldBe true
    }

    test("a per-target iteration matches when any one of its targets is a land you control") {
        val board = board()
        val obj = board.stackObject(
            Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0))),
            listOf(ChosenTarget.Permanent(board.landB), ChosenTarget.Permanent(board.landA)),
        )
        obj.wouldDestroyLandOf(board.a) shouldBe true
    }

    test("only the chosen mode counts") {
        val board = board()
        val modal = Effects.Modal(
            listOf(Mode(Effects.Destroy(EffectTarget.ContextTarget(0))), Mode(Effects.DrawCards(1)))
        )
        val landTarget = listOf(ChosenTarget.Permanent(board.landA))
        board.stackObject(modal, chosenModes = listOf(0), modeTargets = listOf(landTarget))
            .wouldDestroyLandOf(board.a) shouldBe true
        board.stackObject(modal, chosenModes = listOf(1), modeTargets = listOf(emptyList()))
            .wouldDestroyLandOf(board.a) shouldBe false
    }

    test("destruction behind a choice, exile or damage doesn't count") {
        val board = board()
        val target = listOf(ChosenTarget.Permanent(board.landA))
        withClue("'you may destroy' — only if a choice is made") {
            board.stackObject(Effects.May(Effects.Destroy(EffectTarget.ContextTarget(0))), target)
                .wouldDestroyLandOf(board.a) shouldBe false
        }
        withClue("exile is not destruction") {
            board.stackObject(Effects.Exile(EffectTarget.ContextTarget(0)), target)
                .wouldDestroyLandOf(board.a) shouldBe false
        }
        withClue("damage to a land is never destruction by the spell") {
            board.stackObject(Effects.DealDamage(10, EffectTarget.ContextTarget(0)), target)
                .wouldDestroyLandOf(board.a) shouldBe false
        }
    }

})
