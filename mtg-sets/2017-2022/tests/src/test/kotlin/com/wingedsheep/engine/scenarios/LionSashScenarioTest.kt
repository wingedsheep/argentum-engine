package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.LionSash
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lion Sash — {1}{W} Artifact Creature — Equipment Cat 1/1
 * {W}: Exile target card from a graveyard. If it was a permanent card, put a +1/+1 counter on this
 * permanent. Equipped creature gets +1/+1 for each +1/+1 counter on this Equipment. Reconfigure {2}.
 */
class LionSashScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + LionSash)
        initMirrorMatch(deck = Deck.of("Plains" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("grows only from permanent cards, and passes its counters on to the equipped creature") {
        val d = driver()
        val sash = d.putPermanentOnBattlefield(d.player1, LionSash.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val deadBear = d.putCardInGraveyard(d.player2, "Grizzly Bears")
        val bolt = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        val (exile, attach) = LionSash.activatedAbilities

        d.giveMana(d.player1, Color.WHITE, 1)
        d.submit(ActivateAbility(d.player1, sash, exile.id, listOf(ChosenTarget.Card(deadBear, d.player2, Zone.GRAVEYARD))))
            .outcome shouldBe Outcome.Done
        d.bothPass()
        d.giveMana(d.player1, Color.WHITE, 1)
        d.submit(ActivateAbility(d.player1, sash, exile.id, listOf(ChosenTarget.Card(bolt, d.player2, Zone.GRAVEYARD))))
            .outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.getZone(d.player2, Zone.EXILE).toSet() shouldBe setOf(deadBear, bolt)
        // One counter: the creature card counted, the instant didn't.
        d.state.projectedState.getPower(sash) shouldBe 2

        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, sash, attach.id, listOf(ChosenTarget.Permanent(bears))))
            .outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.projectedState.isCreature(sash) shouldBe false
        d.state.projectedState.getPower(bears) shouldBe 3
        d.state.projectedState.getToughness(bears) shouldBe 3
    }
})
