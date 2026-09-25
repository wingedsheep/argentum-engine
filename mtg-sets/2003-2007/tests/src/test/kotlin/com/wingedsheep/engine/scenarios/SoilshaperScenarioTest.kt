package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.KamiOfTheWaningMoon
import com.wingedsheep.mtg.sets.definitions.chk.cards.Soilshaper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Soilshaper (CHK) — "Whenever you cast a Spirit or Arcane spell, target land becomes a 3/3
 * creature until end of turn. It's still a land."
 */
class SoilshaperScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Soilshaper + KamiOfTheWaningMoon)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("casting a Spirit animates the target land into a 3/3 that is still a land, until end of turn") {
        val d = driver()
        val p1 = d.player1
        d.putCreatureOnBattlefield(p1, "Soilshaper")
        val forest = d.putLandOnBattlefield(p1, "Forest")
        val kami = d.putCardInHand(p1, "Kami of the Waning Moon")
        d.giveMana(p1, Color.BLACK, 3)

        d.castSpell(p1, kami).error shouldBe null
        withClue("the cast trigger asks for its land target") {
            d.pendingDecision?.playerId shouldBe p1
        }
        d.submitTargetSelection(p1, listOf(forest))
        d.bothPass() // Soilshaper's trigger resolves

        val projected = d.state.projectedState
        projected.isCreature(forest) shouldBe true
        projected.hasType(forest, "LAND") shouldBe true
        projected.getPower(forest) shouldBe 3
        projected.getToughness(forest) shouldBe 3

        d.passPriorityUntil(Phase.ENDING)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.isCreature(forest) shouldBe false
    }

    test("a spell that is neither Spirit nor Arcane does not trigger it") {
        val d = driver()
        val p1 = d.player1
        d.putCreatureOnBattlefield(p1, "Soilshaper")
        val forest = d.putLandOnBattlefield(p1, "Forest")
        val bears = d.putCardInHand(p1, "Grizzly Bears")
        d.giveMana(p1, Color.GREEN, 2)

        d.castSpell(p1, bears).error shouldBe null
        d.pendingDecision shouldBe null
        d.getStackSpellNames() shouldBe listOf("Grizzly Bears")
        d.state.projectedState.isCreature(forest) shouldBe false
    }
})
