package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A copy of a *permanent* does not inherit the cast-time X (CR 707.2: X is a choice made on the
 * stack, not a copiable value of a permanent; counters are not copied either). Cloning a creature
 * that entered with X +1/+1 counters therefore produces a copy with no counters and no
 * [CastChoicesComponent] — proving [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastX] is not
 * carried across the copy.
 *
 * Uses a 3/3 base "enters with X +1/+1 counters" creature (rather than Hydroid Krasis, a 0/0) so the
 * counter-less copy survives state-based actions and can be inspected.
 */
class CastXNotInheritedByCopyTest : FunSpec({

    // {X}{G} 3/3 "This creature enters with X +1/+1 counters on it." — reads CastX.
    val castXGolem = card("CastX Golem") {
        manaCost = "{X}{G}"
        typeLine = "Creature — Golem"
        power = 3
        toughness = 3
        replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.CastX))
    }

    test("Clone of an X-cast creature inherits no cast X (no counters, no CastChoicesComponent)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(castXGolem)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast CastX Golem for X=4 → a 7/7 with four +1/+1 counters and CastChoicesComponent(x = 4).
        val golem = driver.putCardInHand(player, "CastX Golem")
        driver.giveMana(player, Color.GREEN, 5)
        driver.castXSpell(player, golem, xValue = 4).error shouldBe null
        repeat(6) { if (driver.state.stack.isNotEmpty() && driver.pendingDecision == null) driver.bothPass() }

        driver.state.getEntity(golem)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        driver.state.getEntity(golem)?.get<CastChoicesComponent>()?.x shouldBe 4

        // Clone it.
        val clone = driver.putCardInHand(player, "Clone")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, clone)
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain golem
        driver.submitCardSelection(player, listOf(golem))

        // The clone is a copy of CastX Golem and survives (3/3 base)...
        driver.state.getBattlefield() shouldContain clone
        driver.state.getEntity(clone)?.get<CardComponent>()?.name shouldBe "CastX Golem"
        // ...but it was not cast with X, so it inherits neither the cast X nor the counters.
        driver.state.getEntity(clone)?.get<CastChoicesComponent>().shouldBeNull()
        (driver.state.getEntity(clone)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})
