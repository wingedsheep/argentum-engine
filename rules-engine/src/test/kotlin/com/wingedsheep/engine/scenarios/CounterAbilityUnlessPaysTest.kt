package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Counter target spell or ability unless its controller pays {N}" (`Effects.CounterUnlessPays`
 * with `target = CounterTarget.SpellOrAbility`): when the target is an activated or triggered
 * ability, the payer is that ability's controller — an ability has no caster. Paying leaves the
 * ability on the stack; not paying (or being unable to) removes it.
 */
class CounterAbilityUnlessPaysTest : FunSpec({

    val pingRod = card("Ping Rod") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val player = target(Targets.Player)
            effect = Effects.DealDamage(1, player)
        }
    }
    val taxingStifle = card("Taxing Stifle") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOrAbilityOnStack)
            effect = Effects.CounterUnlessPays("{2}", target = CounterTarget.SpellOrAbility)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(pingRod)
        registerCard(taxingStifle)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** The active player pings the opponent; the opponent answers with Taxing Stifle. */
    fun GameTestDriver.pingThenStifle(): com.wingedsheep.sdk.model.EntityId {
        val active = state.activePlayerId!!
        val opponent = getOpponent(active)
        val rod = putPermanentOnBattlefield(active, "Ping Rod")
        val abilityId = cardRegistry.getCard("Ping Rod")!!.script.activatedAbilities[0].id
        submit(ActivateAbility(active, rod, abilityId, targets = listOf(ChosenTarget.Player(opponent))))
            .outcome shouldBe Outcome.Done
        val ping = state.stack.last()
        passPriority(active)
        giveMana(opponent, Color.BLUE, 1)
        val stifle = putCardInHand(opponent, "Taxing Stifle")
        castSpellWithTargets(opponent, stifle, listOf(ChosenTarget.Spell(ping))).outcome shouldBe Outcome.Done
        return ping
    }

    test("an ability whose controller can't pay is countered") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        val ping = d.pingThenStifle()

        d.bothPass()

        d.state.stack.contains(ping) shouldBe false
        d.state.pendingDecision shouldBe null
        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 20
    }

    test("the ability's controller is asked, and paying from floating mana keeps the ability") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        val ping = d.pingThenStifle()
        d.giveMana(active, Color.RED, 2)

        d.bothPass()

        val ask = d.pendingDecision
        ask.shouldBeInstanceOf<YesNoDecision>()
        ask.playerId shouldBe active
        ask.prompt shouldContain "ability"
        d.submitYesNo(active, true).outcome shouldBe Outcome.Done

        d.state.stack.contains(ping) shouldBe true
        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 19
    }

    test("declining to pay counters the ability") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        val ping = d.pingThenStifle()
        d.giveMana(active, Color.RED, 2)

        d.bothPass()
        d.submitYesNo(active, false).outcome shouldBe Outcome.Done

        d.state.stack.contains(ping) shouldBe false
        d.getLifeTotal(opponent) shouldBe 20
    }

    test("the description reads the widened target") {
        Effects.CounterUnlessPays("{3}", target = CounterTarget.SpellOrAbility).description shouldBe
            "Counter target spell or ability unless its controller pays {3}"
        Effects.CounterUnlessPays("{3}").description shouldBe
            "Counter target spell unless its controller pays {3}"
    }
})
