package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HisokasGuard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hisoka's Guard (CHK #68) — "You may choose not to untap this creature during your untap step.
 * {1}{U}, {T}: Target creature you control other than this creature has shroud for as long as this
 * creature remains tapped."
 *
 * Pins the target scope (your creature, never the Guard itself, never an opponent's) and the
 * tap-latched duration: shroud survives an untap step the Guard is kept tapped through, and drops
 * the moment the Guard untaps.
 */
class HisokasGuardScenarioTest : FunSpec({

    val guardAbility = HisokasGuard.activatedAbilities.single().id
    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activate(guard: com.wingedsheep.sdk.model.EntityId, target: com.wingedsheep.sdk.model.EntityId) =
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = guard,
                abilityId = guardAbility,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )

    test("grants shroud to another creature you control while the Guard stays tapped") {
        val d = driver()
        val guard = d.putCreatureOnBattlefield(d.player1, "Hisoka's Guard")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(guard)
        d.giveMana(d.player1, Color.BLUE, 2)

        d.activate(guard, bears).outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.getEntity(guard)?.has<TappedComponent>() shouldBe true
        projector.project(d.state).hasKeyword(bears, Keyword.SHROUD) shouldBe true

        // Keep the Guard tapped through the next untap step: shroud persists.
        d.passPriorityUntil(Step.UNTAP)
        (d.pendingDecision is SelectCardsDecision) shouldBe true
        d.submitCardSelection(d.player1, listOf(guard))
        d.state.getEntity(guard)?.has<TappedComponent>() shouldBe true
        projector.project(d.state).hasKeyword(bears, Keyword.SHROUD) shouldBe true

        // Untap it the following turn: shroud ends.
        d.passPriorityUntil(Step.UNTAP)
        d.submitCardSelection(d.player1, emptyList())
        d.state.getEntity(guard)?.has<TappedComponent>() shouldBe false
        projector.project(d.state).hasKeyword(bears, Keyword.SHROUD) shouldBe false
    }

    test("cannot target the Guard itself") {
        val d = driver()
        val guard = d.putCreatureOnBattlefield(d.player1, "Hisoka's Guard")
        d.removeSummoningSickness(guard)
        d.giveMana(d.player1, Color.BLUE, 2)

        d.activate(guard, guard).error shouldNotBe null
    }

    test("cannot target a creature an opponent controls") {
        val d = driver()
        val guard = d.putCreatureOnBattlefield(d.player1, "Hisoka's Guard")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(d.player1), "Grizzly Bears")
        d.removeSummoningSickness(guard)
        d.giveMana(d.player1, Color.BLUE, 2)

        d.activate(guard, theirs).error shouldNotBe null
    }
})
