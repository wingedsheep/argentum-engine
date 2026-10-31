package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectManaSourcesDecision
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
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `CounterTargetSource.EachOnStack` — "for each spell and ability your opponents control, counter
 * it unless its controller pays {N}" (Whirlwind Denial). Untargeted; the resolving object's own
 * controller's objects are untouched; controllers decide in APNAP order; and nothing is countered
 * until every controller has decided, after which every unpaid object is countered together
 * (Whirlwind Denial's 2020-01-24 ruling).
 */
class CounterEachOnStackUnlessPaysTest : FunSpec({

    val pingRod = card("Ping Rod") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val player = target(Targets.Player)
            effect = Effects.DealDamage(1, player)
        }
    }
    val denial = card("Test Denial") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            effect = Effects.CounterUnlessPays(
                "{4}",
                target = CounterTarget.SpellOrAbility,
                targetSource = CounterTargetSource.EachOnStack()
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(pingRod)
        registerCard(denial)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    class Board(val bolt: EntityId, val ping: EntityId, val ownBolt: EntityId)

    /**
     * The active player bolts the opponent and, holding priority, pings them. The opponent bolts
     * back and, holding priority, casts the denial on top of everything.
     */
    fun GameTestDriver.setUpStack(): Board {
        val active = state.activePlayerId!!
        val opponent = getOpponent(active)
        giveMana(active, Color.RED, 1)
        castSpell(active, putCardInHand(active, "Lightning Bolt"), listOf(opponent)).outcome shouldBe Outcome.Done
        val bolt = state.stack.last()
        val rod = putPermanentOnBattlefield(active, "Ping Rod")
        val abilityId = cardRegistry.getCard("Ping Rod")!!.script.activatedAbilities[0].id
        submit(ActivateAbility(active, rod, abilityId, targets = listOf(ChosenTarget.Player(opponent))))
            .outcome shouldBe Outcome.Done
        val ping = state.stack.last()
        passPriority(active)
        giveMana(opponent, Color.RED, 1)
        castSpell(opponent, putCardInHand(opponent, "Lightning Bolt"), listOf(active)).outcome shouldBe Outcome.Done
        val ownBolt = state.stack.last()
        giveMana(opponent, Color.BLUE, 1)
        castSpell(opponent, putCardInHand(opponent, "Test Denial")).outcome shouldBe Outcome.Done
        return Board(bolt, ping, ownBolt)
    }

    test("an opponent who can't pay loses every spell and ability; the caster's own spell is untouched") {
        val d = driver()
        val board = d.setUpStack()

        d.bothPass()

        d.state.pendingDecision shouldBe null
        d.state.stack.contains(board.bolt) shouldBe false
        d.state.stack.contains(board.ping) shouldBe false
        d.state.stack.contains(board.ownBolt) shouldBe true
        d.getGraveyardCardNames(d.state.activePlayerId!!) shouldBe listOf("Lightning Bolt")
    }

    test("paying for one object leaves too little for the next, which is countered without asking") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val opponent = d.getOpponent(active)
        val board = d.setUpStack()
        d.giveMana(active, Color.RED, 4)

        d.bothPass()

        // Top of the active player's objects first: the Ping Rod ability.
        val ask = d.pendingDecision
        ask.shouldBeInstanceOf<YesNoDecision>()
        ask.playerId shouldBe active
        ask.prompt shouldContain "ability (Ping Rod)"
        d.submitYesNo(active, true).outcome shouldBe Outcome.Done

        d.state.pendingDecision shouldBe null
        d.state.stack.contains(board.ping) shouldBe true
        d.state.stack.contains(board.bolt) shouldBe false
        d.bothPass() // opponent's own bolt
        d.bothPass() // ping
        d.getLifeTotal(opponent) shouldBe 19
    }

    test("a declined object stays on the stack until every object is decided, then is countered") {
        val d = driver()
        val active = d.state.activePlayerId!!
        val board = d.setUpStack()
        d.giveMana(active, Color.RED, 8)

        d.bothPass()
        d.submitYesNo(active, false).outcome.shouldBeInstanceOf<Outcome.Paused>()

        // Second question, about the bolt: the declined ping is not countered yet.
        val ask = d.pendingDecision
        ask.shouldBeInstanceOf<YesNoDecision>()
        ask.prompt shouldContain "spell (Lightning Bolt)"
        d.state.stack.contains(board.ping) shouldBe true

        d.submitYesNo(active, true).outcome shouldBe Outcome.Done

        d.state.stack.contains(board.ping) shouldBe false
        d.state.stack.contains(board.bolt) shouldBe true
    }

    test("paying by tapping lands goes through mana-source selection; declining there defers the counter") {
        val d = driver()
        val active = d.state.activePlayerId!!
        repeat(4) { d.putPermanentOnBattlefield(active, "Mountain") }
        val board = d.setUpStack()

        d.bothPass()
        d.submitYesNo(active, true).outcome.shouldBeInstanceOf<Outcome.Paused>()
        val pay = d.pendingDecision
        pay.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitDecision(active, ManaSourcesSelectedResponse(pay.id, emptyList(), autoPay = false))
            .outcome.shouldBeInstanceOf<Outcome.Paused>()

        // Declined the ping's payment — the bolt is asked about while the ping waits.
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.state.stack.contains(board.ping) shouldBe true
        d.submitYesNo(active, true).outcome.shouldBeInstanceOf<Outcome.Paused>()
        val payBolt = d.pendingDecision
        payBolt.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitDecision(active, ManaSourcesSelectedResponse(payBolt.id, emptyList(), autoPay = true))
            .outcome shouldBe Outcome.Done

        d.state.pendingDecision shouldBe null
        d.state.stack.contains(board.ping) shouldBe false
        d.state.stack.contains(board.bolt) shouldBe true
    }

    test("controllers decide in APNAP order, not stack order") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all)
            registerCard(denial)
        }
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.state.activePlayerId shouldBe p1

        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, d.putCardInHand(p1, "Lightning Bolt"), listOf(p3)).outcome shouldBe Outcome.Done
        d.passPriority(p1)
        d.giveMana(p2, Color.RED, 1)
        d.castSpell(p2, d.putCardInHand(p2, "Lightning Bolt"), listOf(p3)).outcome shouldBe Outcome.Done
        d.passPriority(p2)
        d.giveMana(p3, Color.BLUE, 1)
        d.castSpell(p3, d.putCardInHand(p3, "Test Denial")).outcome shouldBe Outcome.Done
        d.giveMana(p1, Color.RED, 4)
        d.giveMana(p2, Color.RED, 4)

        d.passPriority(p3)
        d.passPriority(p1)
        d.passPriority(p2)

        // p2's bolt is higher on the stack, but the active player p1 decides first.
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe p1
        d.submitYesNo(p1, false).outcome.shouldBeInstanceOf<Outcome.Paused>()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe p2
        d.submitYesNo(p2, true).outcome shouldBe Outcome.Done

        d.state.pendingDecision shouldBe null
        d.state.stack.size shouldBe 1
        d.getGraveyardCardNames(p1) shouldBe listOf("Lightning Bolt")
    }

    test("the description reads the stack-wide shape") {
        Effects.CounterUnlessPays(
            "{4}", target = CounterTarget.SpellOrAbility, targetSource = CounterTargetSource.EachOnStack()
        ).description shouldBe
            "For each spell and ability your opponents control, counter it unless its controller pays {4}"
        Effects.CounterUnlessPays(
            "{1}", targetSource = CounterTargetSource.EachOnStack(Player.Each)
        ).description shouldBe "For each spell on the stack, counter it unless its controller pays {1}"
    }

    test("an unconditional stack-wide counter is rejected in favour of CounterAllOnStackEffect") {
        shouldThrow<IllegalArgumentException> {
            CounterEffect(target = CounterTarget.SpellOrAbility, targetSource = CounterTargetSource.EachOnStack())
        }
    }
})
