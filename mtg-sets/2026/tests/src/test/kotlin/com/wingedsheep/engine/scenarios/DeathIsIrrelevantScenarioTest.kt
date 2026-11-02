package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.DeathIsIrrelevant
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Death Is Irrelevant ({6}{B}{B} Enchantment):
 * "Artifact creatures you control have indestructible.
 * At the beginning of your end step, assimilate target creature card from a graveyard."
 */
class DeathIsIrrelevantScenarioTest : FunSpec({

    test("at your end step an opponent's creature card is assimilated: yours, a Borg artifact creature, indestructible") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(DeathIsIrrelevant))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Death Is Irrelevant")
        val bears = d.putCardInGraveyard(opp, "Grizzly Bears")
        val myBears = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.passPriorityUntil(Step.END)
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(me, listOf(bears)).error shouldBe null
        d.bothPass()

        (bears in d.state.getBattlefield()) shouldBe true
        val projected = d.state.projectedState
        projected.getController(bears) shouldBe me
        projected.getTypes(bears) shouldContainAll setOf("ARTIFACT", "CREATURE")
        projected.getSubtypes(bears) shouldContain "Borg"
        projected.getSubtypes(bears) shouldNotContain "Bear"
        d.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        projected.getPower(bears) shouldBe 3
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
        // A non-artifact creature you control isn't covered.
        projected.hasKeyword(myBears, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
