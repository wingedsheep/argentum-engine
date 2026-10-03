package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.wwk.cards.AvengerOfZendikar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Avenger of Zendikar (WWK #96) — {5}{G}{G} 5/5 Creature — Elemental.
 *
 *   When this creature enters, create a 0/1 green Plant creature token for each land you control.
 *   Landfall — Whenever a land you control enters, you may put a +1/+1 counter on each Plant
 *   creature you control.
 */
class AvengerOfZendikarScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AvengerOfZendikar))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun plants(d: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId) =
        d.getCreatures(player).filter { d.getCardName(it) == "Plant Token" }

    fun castAvenger(d: GameTestDriver, lands: Int) {
        val you = d.activePlayer!!
        repeat(lands) { d.putLandOnBattlefield(you, "Forest") }
        val avenger = d.putCardInHand(you, "Avenger of Zendikar")
        d.giveMana(you, Color.GREEN, 7)
        d.castSpell(you, avenger).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the ETB trigger
    }

    test("entering creates one 0/1 Plant per land you control") {
        val d = driver()
        val you = d.activePlayer!!
        castAvenger(d, lands = 3)

        val tokens = plants(d, you)
        tokens.size shouldBe 3
        tokens.forEach {
            d.state.projectedState.getPower(it) shouldBe 0
            d.state.projectedState.getToughness(it) shouldBe 1
        }
    }

    test("landfall puts a +1/+1 counter on each Plant when accepted") {
        val d = driver()
        val you = d.activePlayer!!
        castAvenger(d, lands = 2)
        plants(d, you).size shouldBe 2

        val forest = d.putCardInHand(you, "Forest")
        d.playLand(you, forest).outcome shouldBe Outcome.Done
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(you, true)

        plants(d, you).forEach {
            d.state.projectedState.getPower(it) shouldBe 1
            d.state.projectedState.getToughness(it) shouldBe 2
        }
        // The Avenger itself is not a Plant.
        d.state.projectedState.getPower(d.findPermanent(you, "Avenger of Zendikar")!!) shouldBe 5
    }

    test("declining the landfall trigger adds no counters") {
        val d = driver()
        val you = d.activePlayer!!
        castAvenger(d, lands = 1)

        val forest = d.putCardInHand(you, "Forest")
        d.playLand(you, forest).outcome shouldBe Outcome.Done
        d.bothPass()
        d.submitYesNo(you, false)

        plants(d, you).forEach { d.state.projectedState.getPower(it) shouldBe 0 }
    }
})
