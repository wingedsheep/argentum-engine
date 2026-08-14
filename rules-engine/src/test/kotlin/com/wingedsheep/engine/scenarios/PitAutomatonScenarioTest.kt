package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for Pit Automaton — "{2}, {T}: When you next activate an exhaust ability that
 * isn't a mana ability this turn, copy it. You may choose new targets for the copy."
 *
 * The claims under test:
 *  - the ability arms a one-shot delayed trigger that copies the next qualifying exhaust activation
 *    (observed as a doubled effect), and is consumed by it;
 *  - a *non*-exhaust activated ability leaves it armed;
 *  - an exhaust **mana** ability leaves it armed — the "that isn't a mana ability" clause Pit
 *    Automaton's Oracle text was updated to add, and the reason it can't reuse the plain
 *    `Triggers.YouActivateExhaustAbility` the set's other exhaust payoffs use;
 *  - the mana ability produces two colorless restricted to activating abilities.
 */
class PitAutomatonScenarioTest : ScenarioTestBase() {

    // "Exhaust — {1}: Put a +1/+1 counter on this creature." Copying it yields two counters.
    private val exhaustBuddy = card("Exhaust Buddy") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        activatedAbility {
            isExhaust = true
            cost = Costs.Mana("{1}")
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // A plain (non-exhaust) activated ability — must not consume the armed trigger.
    private val plainActivator = card("Plain Activator") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // An exhaust MANA ability with no {T} in its cost, so the engine still emits an
    // AbilityActivatedEvent for it (a {T} mana ability never reaches the stack at all).
    private val exhaustManaSource = card("Exhaust Mana Source") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        activatedAbility {
            isExhaust = true
            cost = Costs.Mana("{0}")
            effect = Effects.AddMana(Color.GREEN, 1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    private val armAbility
        get() = cardRegistry.getCard("Pit Automaton")!!.script.activatedAbilities.single { !it.isManaAbility }
    private val pitManaAbility
        get() = cardRegistry.getCard("Pit Automaton")!!.script.activatedAbilities.single { it.isManaAbility }
    private val buddyAbility get() = cardRegistry.getCard("Exhaust Buddy")!!.script.activatedAbilities.single()
    private val plainAbility get() = cardRegistry.getCard("Plain Activator")!!.script.activatedAbilities.single()
    private val manaSourceAbility
        get() = cardRegistry.getCard("Exhaust Mana Source")!!.script.activatedAbilities.single()

    private fun TestGame.activateAndResolve(permanent: EntityId, abilityId: AbilityId) =
        execute(ActivateAbility(player1Id, permanent, abilityId)).also { result ->
            if (result.error == null) {
                if (state.pendingDecision is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
            }
        }

    private fun TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(vararg extraCards: String) = run {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Pit Automaton", summoningSickness = false)
            .withLandsOnBattlefield(1, "Forest", 8)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (name in extraCards) builder = builder.withCardOnBattlefield(1, name, summoningSickness = false)
        // Enough library for the turn-passing tests so nobody decks out mid-scenario.
        repeat(10) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
        repeat(10) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
        builder.build()
    }

    init {
        cardRegistry.register(exhaustBuddy)
        cardRegistry.register(plainActivator)
        cardRegistry.register(exhaustManaSource)

        test("the next exhaust ability activated is copied, and the trigger is spent") {
            val game = board("Exhaust Buddy")
            val automaton = game.findPermanent("Pit Automaton")!!
            val buddy = game.findPermanent("Exhaust Buddy")!!

            val arm = game.activateAndResolve(automaton, armAbility.id)
            withClue("arming should be legal: ${arm.error}") { arm.error shouldBe null }
            withClue("a one-shot delayed trigger is now watching for an exhaust activation") {
                game.state.delayedTriggers.size shouldBe 1
            }

            game.activateAndResolve(buddy, buddyAbility.id).error shouldBe null
            withClue("the exhaust ability resolved once, and its copy resolved once more") {
                game.plusOneCounters(buddy) shouldBe 2
            }
            withClue("a 'when you next …' trigger is consumed by the activation that fired it") {
                game.state.delayedTriggers.size shouldBe 0
            }
        }

        test("a non-exhaust activated ability leaves the trigger armed") {
            val game = board("Plain Activator", "Exhaust Buddy")
            val automaton = game.findPermanent("Pit Automaton")!!
            val plain = game.findPermanent("Plain Activator")!!
            val buddy = game.findPermanent("Exhaust Buddy")!!

            game.activateAndResolve(automaton, armAbility.id).error shouldBe null
            game.activateAndResolve(plain, plainAbility.id).error shouldBe null

            withClue("an ordinary ability isn't an exhaust ability, so nothing was copied") {
                game.plusOneCounters(plain) shouldBe 1
            }
            game.state.delayedTriggers.size shouldBe 1

            // Still armed: the next real exhaust activation gets doubled.
            game.activateAndResolve(buddy, buddyAbility.id).error shouldBe null
            game.plusOneCounters(buddy) shouldBe 2
        }

        test("an exhaust mana ability does not fire it (Oracle update)") {
            val game = board("Exhaust Mana Source", "Exhaust Buddy")
            val automaton = game.findPermanent("Pit Automaton")!!
            val manaSource = game.findPermanent("Exhaust Mana Source")!!
            val buddy = game.findPermanent("Exhaust Buddy")!!

            game.activateAndResolve(automaton, armAbility.id).error shouldBe null
            game.activateAndResolve(manaSource, manaSourceAbility.id).error shouldBe null

            withClue("'an exhaust ability that isn't a mana ability' excludes this one") {
                game.state.delayedTriggers.size shouldBe 1
            }

            game.activateAndResolve(buddy, buddyAbility.id).error shouldBe null
            withClue("the still-armed trigger copies the first non-mana exhaust ability instead") {
                game.plusOneCounters(buddy) shouldBe 2
            }
        }

        test("an unfired trigger expires at end of turn ('this turn')") {
            val game = board("Exhaust Buddy")
            val automaton = game.findPermanent("Pit Automaton")!!
            val buddy = game.findPermanent("Exhaust Buddy")!!

            game.activateAndResolve(automaton, armAbility.id).error shouldBe null
            game.state.delayedTriggers.size shouldBe 1

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            withClue("the delayed trigger is scoped to the turn it was created on") {
                game.state.delayedTriggers.size shouldBe 0
            }
            withClue("and the exhaust ability it was watching for was never copied") {
                game.plusOneCounters(buddy) shouldBe 0
            }
        }

        test("the mana ability adds two colorless spendable only on activated abilities") {
            val game = board()
            val automaton = game.findPermanent("Pit Automaton")!!

            game.execute(ActivateAbility(game.player1Id, automaton, pitManaAbility.id)).error shouldBe null

            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            withClue("both pips are stored as restricted mana carrying the activation-only rider") {
                pool.restrictedMana.size shouldBe 2
            }
        }
    }
}
