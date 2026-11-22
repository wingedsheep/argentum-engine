package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantNextSpellRiderEffect
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * The general one-shot "the next spell you cast this turn gets <rider>" grant
 * ([GrantNextSpellRiderEffect]) with riders other than can't-be-countered (that one is pinned in
 * [MakeNextSpellUncounterableTest]): Savage Summoning's "that creature enters with an additional
 * +1/+1 counter on it", and a rider that queues a trigger.
 */
class NextSpellRiderGrantTest : FunSpec({

    fun grant(name: String, effect: com.wingedsheep.sdk.scripting.effects.Effect) = CardDefinition.instant(
        name = name,
        manaCost = ManaCost.parse("{G}"),
        oracleText = effect.description,
        script = CardScript.spell(effect = effect)
    )

    val counterGrant = grant("Counter Grant", Effects.GrantNextSpellEntryCounters(GameObjectFilter.Creature))
    val lifeGrant = grant(
        "Life Grant",
        GrantNextSpellRiderEffect(
            ManaSpellRider.TriggersWhenSpent(GameObjectFilter.Any, Effects.GainLife(3)),
            GameObjectFilter.Creature
        )
    )
    val sorcery = CardDefinition.sorcery(
        name = "Rider Test Sorcery",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = Effects.DrawCards(1))
    )

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.cast(card: EntityId, targets: List<ChosenTarget> = emptyList()) =
        submitSuccess(CastSpell(player1, card, targets = targets, paymentStrategy = PaymentStrategy.FromPool))

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(counterGrant, lifeGrant, sorcery))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.GREEN, 12)
    }

    test("the next matching spell enters with the counter; a non-matching spell leaves the grant waiting") {
        val d = setup()
        d.cast(d.putCardInHand(d.player1, "Counter Grant")); d.bothPass()
        d.cast(d.putCardInHand(d.player1, "Rider Test Sorcery")); d.bothPass()
        d.state.pendingNextSpellRiders.size shouldBe 1

        val first = d.putCardInHand(d.player1, "Centaur Courser")
        d.cast(first)
        d.state.pendingNextSpellRiders.shouldBeEmpty()
        d.bothPass()
        val second = d.putCardInHand(d.player1, "Centaur Courser")
        d.cast(second); d.bothPass()

        d.plusOnes(first) shouldBe 1
        d.plusOnes(second) shouldBe 0
    }

    test("two grants both ride the same next spell") {
        val d = setup()
        d.cast(d.putCardInHand(d.player1, "Counter Grant")); d.bothPass()
        d.cast(d.putCardInHand(d.player1, "Counter Grant")); d.bothPass()
        val courser = d.putCardInHand(d.player1, "Centaur Courser")
        d.cast(courser); d.bothPass()
        d.plusOnes(courser) shouldBe 2
    }

    test("a countered spell still spends the grant and the next creature gets nothing") {
        val d = setup()
        d.cast(d.putCardInHand(d.player1, "Counter Grant")); d.bothPass()
        val countered = d.putCardInHand(d.player1, "Centaur Courser")
        d.cast(countered)
        d.passPriority(d.player1)
        val counterspell = d.putCardInHand(d.player2, "Counterspell")
        d.giveMana(d.player2, Color.BLUE, 2)
        d.submitSuccess(
            CastSpell(d.player2, counterspell, targets = listOf(ChosenTarget.Spell(countered)), paymentStrategy = PaymentStrategy.FromPool)
        )
        d.bothPass()
        d.state.getBattlefield().contains(countered) shouldBe false

        val next = d.putCardInHand(d.player1, "Centaur Courser")
        d.cast(next); d.bothPass()
        d.plusOnes(next) shouldBe 0
    }

    test("a trigger rider is put on the stack above the spell and resolves first") {
        val d = setup()
        d.cast(d.putCardInHand(d.player1, "Life Grant")); d.bothPass()
        val life = d.getLifeTotal(d.player1)
        d.cast(d.putCardInHand(d.player1, "Centaur Courser"))
        d.stackSize shouldBe 2
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 3
        d.stackSize shouldBe 1
    }
})
