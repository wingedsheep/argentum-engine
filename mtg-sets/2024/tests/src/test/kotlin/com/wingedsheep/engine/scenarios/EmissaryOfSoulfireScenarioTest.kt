package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.EmissaryOfSoulfire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Emissary of Soulfire (MH3) — three energy on entry; {E}{E} at sorcery speed puts an exalted
 * counter on a creature you control, and each counter is its own exalted trigger.
 */
class EmissaryOfSoulfireScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + EmissaryOfSoulfire)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.exaltedCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.EXALTED) ?: 0
    val ability = EmissaryOfSoulfire.activatedAbilities.single().id
    fun GameTestDriver.activate(emissary: EntityId, target: EntityId) =
        submit(ActivateAbility(player1, emissary, ability, targets = listOf(ChosenTarget.Permanent(target))))

    test("entering gives three energy") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Emissary of Soulfire")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.energy() shouldBe 3
    }

    test("two activations stack two exalted counters that pump a lone attacker +2/+2") {
        val d = driver()
        val emissary = d.putCreatureOnBattlefield(d.player1, "Emissary of Soulfire")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        d.giveEnergy(4)

        repeat(2) {
            d.activate(emissary, bears).error shouldBe null
            d.bothPass()
        }
        d.energy() shouldBe 0
        d.exaltedCounters(bears) shouldBe 2
        d.state.projectedState.hasKeyword(bears, Keyword.EXALTED) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bears), d.player2).error shouldBe null
        withClue("one exalted trigger per counter") { d.state.stack.size shouldBe 2 }
        while (d.state.stack.isNotEmpty()) d.bothPass()

        d.state.projectedState.getPower(bears) shouldBe 4
        d.state.projectedState.getToughness(bears) shouldBe 4
    }

    test("costs two energy, sorcery speed, and only targets your creatures") {
        val d = driver()
        val emissary = d.putCreatureOnBattlefield(d.player1, "Emissary of Soulfire")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.giveEnergy(1)
        withClue("one energy can't pay {E}{E}") { d.activate(emissary, emissary).error shouldNotBe null }

        d.giveEnergy(2)
        withClue("an opponent's creature isn't a legal target") { d.activate(emissary, theirs).error shouldNotBe null }

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        withClue("not at instant speed") { d.activate(emissary, emissary).error shouldNotBe null }
        d.energy() shouldBe 2
    }
})
