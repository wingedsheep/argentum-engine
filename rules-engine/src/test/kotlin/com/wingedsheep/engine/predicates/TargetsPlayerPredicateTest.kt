package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.TargetsPlayer] — "a spell that targets
 * you / an opponent", the player half of `TargetsMatching`. The predicate reads only the stack
 * object's chosen targets, so each case stamps a bare stack entity with a [TargetsComponent].
 */
class TargetsPlayerPredicateTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)

    fun players(): Triple<GameState, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val a = driver.player1
        return Triple(driver.state, a, driver.getOpponent(a))
    }

    fun GameState.withStackObject(vararg targets: ChosenTarget): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        return withEntity(id, ComponentContainer().with(TargetsComponent(targets.toList()))) to id
    }

    fun GameState.matches(id: EntityId, player: Player, chooser: EntityId) = evaluator.matches(
        this, projectedState, id, GameObjectFilter.Any.targetsPlayer(player), PredicateContext(controllerId = chooser)
    )

    test("'you' is read relative to the chooser") {
        val (base, a, b) = players()
        val (state, spell) = base.withStackObject(ChosenTarget.Player(a))
        state.matches(spell, Player.You, chooser = a) shouldBe true
        state.matches(spell, Player.You, chooser = b) shouldBe false
    }

    test("'an opponent' matches a spell aimed at one of the chooser's opponents") {
        val (base, a, b) = players()
        val (state, spell) = base.withStackObject(ChosenTarget.Player(a))
        state.matches(spell, Player.EachOpponent, chooser = b) shouldBe true
        state.matches(spell, Player.EachOpponent, chooser = a) shouldBe false
    }

    test("a player among several targets still matches") {
        val (base, a, _) = players()
        val (state, spell) = base.withStackObject(ChosenTarget.Permanent(EntityId.generate()), ChosenTarget.Player(a))
        withClue("the object target doesn't hide the player target") {
            state.matches(spell, Player.You, chooser = a) shouldBe true
        }
    }

    test("object-only and targetless stack objects never match") {
        val (base, a, _) = players()
        val (withObject, objectSpell) = base.withStackObject(ChosenTarget.Permanent(EntityId.generate()))
        withObject.matches(objectSpell, Player.You, chooser = a) shouldBe false
        val (targetless, targetlessSpell) = base.withStackObject()
        targetless.matches(targetlessSpell, Player.Any, chooser = a) shouldBe false
    }
})
