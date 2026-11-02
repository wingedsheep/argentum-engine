package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An [AdditionalSourceTriggers] doubler handed to a permanent by a resolving effect
 * (`Effects.GrantStaticAbility`, stored in `GameState.grantedStaticAbilities`) doubles exactly like
 * a printed one (CR 603.2d), reads its filter relative to the holder, and lapses with its duration —
 * the Lwaxana Troi, Charming Guest shape: "for as long as ~ remains tapped, if a triggered ability
 * of that creature triggers, that ability triggers an additional time."
 */
class GrantedSourceTriggerDoublerTest : ScenarioTestBase() {

    private val granter = card("Test Doubling Granter") {
        manaCost = "{1}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        activatedAbility {
            val creature = target(TargetFilter.OtherCreatureYouControl)
            cost = Costs.Tap
            effect = Effects.GrantStaticAbility(
                AdditionalSourceTriggers(
                    sourceFilter = GameObjectFilter.Any.sourceItself(),
                    excludeSelf = false,
                    description = "If a triggered ability of this creature triggers, that ability triggers an additional time",
                ),
                creature,
                Duration.WhileSourceTapped(),
            )
        }
    }

    private fun lifeWatcher(name: String) = card(name) {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.gainsLife()
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    private val heal = card("Test Heal") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3) }
    }

    private fun TestGame.counters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.grantTo(holder: String) {
        val source = findPermanent("Test Doubling Granter")!!
        val abilityId = cardRegistry.getCard("Test Doubling Granter")!!.script.activatedAbilities.single().id
        execute(
            ActivateAbility(player1Id, source, abilityId, targets = listOf(ChosenTarget.Permanent(findPermanent(holder)!!)))
        ).error shouldBe null
        resolveStack()
    }

    private fun TestGame.gainLife() {
        castSpell(1, "Test Heal").error shouldBe null
        repeat(5) { if (state.stack.isNotEmpty()) resolveStack() }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Test Doubling Granter")
        .withCardOnBattlefield(1, "Test Chosen Watcher")
        .withCardOnBattlefield(1, "Test Other Watcher")
        .withCardInHand(1, "Test Heal")
        .withLandsOnBattlefield(1, "Plains", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        cardRegistry.register(granter)
        cardRegistry.register(lifeWatcher("Test Chosen Watcher"))
        cardRegistry.register(lifeWatcher("Test Other Watcher"))
        cardRegistry.register(heal)

        test("the granted doubler makes the holder's triggered ability trigger an additional time") {
            val game = board().build()
            game.grantTo("Test Chosen Watcher")

            game.gainLife()

            withClue("the chosen creature's trigger fires twice") { game.counters("Test Chosen Watcher") shouldBe 2 }
            withClue("another creature with the same trigger is not doubled") { game.counters("Test Other Watcher") shouldBe 1 }
        }

        test("the doubler ends once its source untaps, and tapping it again does not restore it") {
            val game = board().build()
            game.grantTo("Test Chosen Watcher")

            val source = game.findPermanent("Test Doubling Granter")!!
            game.state = game.state.updateEntity(source) { it.without<TappedComponent>() }
            game.checkStateBasedActions()
            game.state = game.state.updateEntity(source) { it.with(TappedComponent) }

            game.gainLife()

            withClue("CR 611.2b: a 'for as long as' effect that ended stays ended") {
                game.counters("Test Chosen Watcher") shouldBe 1
            }
        }

        test("the doubler ends when its source leaves the battlefield") {
            val game = board().build()
            game.grantTo("Test Chosen Watcher")

            val source: EntityId = game.findPermanent("Test Doubling Granter")!!
            game.state = game.state.removeFromZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD), source)

            game.gainLife()

            game.counters("Test Chosen Watcher") shouldBe 1
        }
    }
}
