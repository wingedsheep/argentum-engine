package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Repeated Reverberation — "When you next cast an instant spell, cast a sorcery spell, or activate a
 * loyalty ability this turn, copy that spell or ability twice."
 *
 * Claims: each of the three events fires the one delayed trigger (copying twice, so the effect
 * happens three times); the first one consumes it; a creature spell or a planeswalker's
 * non-loyalty ability doesn't fire it; copying a loyalty ability moves no loyalty counters; and,
 * per the ruling, the copies are still made when the original was countered first.
 */
class RepeatedReverberationScenarioTest : ScenarioTestBase() {

    private val lifeInstant = card("Reverb Test Instant") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3) }
    }

    private val lifeSorcery = card("Reverb Test Sorcery") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(4) }
    }

    private val testCreature = card("Reverb Test Creature") {
        manaCost = "{1}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
    }

    private val testWalker = card("Reverb Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        loyaltyAbility(+1) { effect = Effects.GainLife(2) }
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(7)
        }
    }

    private val counterspell = card("Reverb Test Counterspell") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }

    private val stifle = card("Reverb Test Stifle") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.ActivatedAbilityOnStack)
            effect = Effects.CounterAbility()
        }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Repeated Reverberation")
        .withCardInHand(1, "Reverb Test Instant")
        .withCardInHand(1, "Reverb Test Sorcery")
        .withCardInHand(1, "Reverb Test Creature")
        .withCardInHand(1, "Reverb Test Counterspell")
        .withCardInHand(1, "Reverb Test Stifle")
        .withCardOnBattlefield(1, "Reverb Test Walker")
        .withLandsOnBattlefield(1, "Mountain", 10)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.payIfAsked() {
        if (state.pendingDecision is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun TestGame.castAndResolve(name: String) {
        castSpell(1, name).error shouldBe null
        payIfAsked()
        resolveStack()
    }

    private fun TestGame.activate(walker: EntityId, index: Int) {
        val ability = testWalker.activatedAbilities[index]
        execute(ActivateAbility(player1Id, walker, ability.id)).error shouldBe null
        payIfAsked()
        resolveStack()
    }

    private fun TestGame.loyalty(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        cardRegistry.register(lifeInstant)
        cardRegistry.register(lifeSorcery)
        cardRegistry.register(testCreature)
        cardRegistry.register(testWalker)
        cardRegistry.register(counterspell)
        cardRegistry.register(stifle)

        test("the next instant spell is copied twice, and the trigger is spent") {
            val game = board()
            game.castAndResolve("Repeated Reverberation")
            game.state.delayedTriggers.size shouldBe 1

            game.castAndResolve("Reverb Test Instant")
            withClue("the instant and its two copies each gain 3") { game.getLifeTotal(1) shouldBe 29 }
            game.state.delayedTriggers.size shouldBe 0

            game.castAndResolve("Reverb Test Sorcery")
            withClue("only the next spell is copied") { game.getLifeTotal(1) shouldBe 33 }
        }

        test("the next sorcery spell is copied twice") {
            val game = board()
            game.castAndResolve("Repeated Reverberation")
            game.castAndResolve("Reverb Test Sorcery")
            game.getLifeTotal(1) shouldBe 32
            game.state.delayedTriggers.size shouldBe 0
        }

        test("the next loyalty ability is copied twice without moving loyalty counters") {
            val game = board()
            val walker = game.findPermanent("Reverb Test Walker")!!
            game.castAndResolve("Repeated Reverberation")

            game.activate(walker, 0)
            withClue("the loyalty ability and its two copies each gain 2") { game.getLifeTotal(1) shouldBe 26 }
            withClue("only the activation's own +1 cost moved loyalty") { game.loyalty(walker) shouldBe 4 }
            game.state.delayedTriggers.size shouldBe 0
        }

        test("a creature spell and a non-loyalty ability leave the trigger armed") {
            val game = board()
            val walker = game.findPermanent("Reverb Test Walker")!!
            game.castAndResolve("Repeated Reverberation")

            game.castAndResolve("Reverb Test Creature")
            game.activate(walker, 1)
            withClue("the {T} ability isn't a loyalty ability, so it resolved once") { game.getLifeTotal(1) shouldBe 27 }
            game.state.delayedTriggers.size shouldBe 1

            game.castAndResolve("Reverb Test Instant")
            game.getLifeTotal(1) shouldBe 36
        }

        test("a countered instant is still copied twice, from its last-known information") {
            val game = board()
            game.castAndResolve("Repeated Reverberation")

            game.castSpell(1, "Reverb Test Instant").error shouldBe null
            game.payIfAsked()
            withClue("the instant and Repeated Reverberation's trigger above it") { game.state.stack.size shouldBe 2 }
            game.castSpellTargetingStackSpell(1, "Reverb Test Counterspell", "Reverb Test Instant").error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            withClue("the original was countered, but its two copies each gain 3") { game.getLifeTotal(1) shouldBe 26 }
        }

        test("a countered loyalty ability is still copied twice") {
            val game = board()
            val walker = game.findPermanent("Reverb Test Walker")!!
            game.castAndResolve("Repeated Reverberation")

            game.execute(ActivateAbility(game.player1Id, walker, testWalker.activatedAbilities[0].id)).error shouldBe null
            game.payIfAsked()
            val ability = game.state.stack.single {
                game.state.getEntity(it)?.has<ActivatedAbilityOnStackComponent>() == true
            }
            val stifleCard = game.state.getHand(game.player1Id).single {
                game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Reverb Test Stifle"
            }
            game.execute(CastSpell(game.player1Id, stifleCard, listOf(ChosenTarget.Spell(ability)))).error shouldBe null
            game.payIfAsked()
            game.resolveStack()

            withClue("the loyalty ability was countered, but its two copies each gain 2") { game.getLifeTotal(1) shouldBe 24 }
            withClue("its +1 cost was still paid") { game.loyalty(walker) shouldBe 4 }
        }
    }
}
