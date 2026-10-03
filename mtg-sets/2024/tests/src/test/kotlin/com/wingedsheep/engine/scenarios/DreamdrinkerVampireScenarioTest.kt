package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.DreamdrinkerVampire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dreamdrinker Vampire (MH3) — "{1}{B}: Adapt 1." and "Whenever one or more +1/+1 counters are put
 * on this creature, it gains menace until end of turn."
 */
class DreamdrinkerVampireScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + DreamdrinkerVampire)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("adapt puts one counter and the counter grants menace; a second adapt does nothing") {
        val d = driver()
        val active = d.activePlayer!!
        val vampire = d.putCreatureOnBattlefield(active, "Dreamdrinker Vampire")
        val adapt = DreamdrinkerVampire.activatedAbilities[0].id

        d.state.projectedState.hasKeyword(vampire, Keyword.MENACE) shouldBe false

        d.giveMana(active, Color.BLACK, 2)
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = vampire, abilityId = adapt))
        d.bothPass() // adapt resolves; menace trigger goes on the stack
        d.state.stack.size shouldBe 1
        d.bothPass() // menace trigger resolves

        d.state.getEntity(vampire)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.getPower(vampire) shouldBe 3
        d.state.projectedState.hasKeyword(vampire, Keyword.MENACE) shouldBe true

        // Already has a +1/+1 counter: adapt adds nothing, so nothing triggers.
        d.giveMana(active, Color.BLACK, 2)
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = vampire, abilityId = adapt))
        d.bothPass()
        d.state.stack.isEmpty() shouldBe true
        d.state.getEntity(vampire)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("menace lasts only until end of turn") {
        val d = driver()
        val active = d.activePlayer!!
        val vampire = d.putCreatureOnBattlefield(active, "Dreamdrinker Vampire")
        val adapt = DreamdrinkerVampire.activatedAbilities[0].id

        d.giveMana(active, Color.BLACK, 2)
        d.submitSuccess(ActivateAbility(playerId = active, sourceId = vampire, abilityId = adapt))
        d.bothPass()
        d.bothPass()
        d.state.projectedState.hasKeyword(vampire, Keyword.MENACE) shouldBe true

        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasKeyword(vampire, Keyword.MENACE) shouldBe false
        d.state.projectedState.hasKeyword(vampire, Keyword.LIFELINK) shouldBe true
    }
})
