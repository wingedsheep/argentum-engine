package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.PhoenixDown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Phoenix Down (FIN #29) — {W} Artifact.
 *
 *   "{1}{W}, {T}, Exile this artifact: Choose one —
 *    • Return target creature card with mana value 4 or less from your graveyard to the battlefield tapped.
 *    • Exile target Skeleton, Spirit, or Zombie."
 *
 * Modal activated ability with an Exile-self cost. Mode 0 reanimates a small creature
 * (entering tapped); Mode 1 exiles a Skeleton/Spirit/Zombie.
 */
class PhoenixDownScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PhoenixDown)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("mode 0: reanimate a small creature from your graveyard, entering tapped") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Centaur Courser is a 3/3 for {2}{G} — mana value 3, within the "4 or less" gate.
        driver.putCardInGraveyard(me, "Centaur Courser")
        val down = driver.putPermanentOnBattlefield(me, "Phoenix Down")
        driver.giveMana(me, Color.WHITE, 2) // {1}{W}

        val abilityId = PhoenixDown.activatedAbilities[0].id
        (driver.submit(ActivateAbility(playerId = me, sourceId = down, abilityId = abilityId)).outcome is Outcome.Paused) shouldBe true

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(me, OptionChosenResponse(modeDecision.id, 0))
        // Per-mode target: the creature in the graveyard.
        val courser = driver.getGraveyard(me).first { driver.getCardName(it) == "Centaur Courser" }
        driver.submitTargetSelection(me, listOf(courser))
        driver.bothPass()

        // Reanimated onto the battlefield, tapped; the artifact exiled itself.
        val reanimated = driver.getCreatures(me).first { driver.getCardName(it) == "Centaur Courser" }
        driver.isTapped(reanimated) shouldBe true
        driver.getExile(me).contains(down) shouldBe true
    }

    test("mode 1: exile a target Zombie") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Black Creature is a 2/2 Zombie.
        val zombie = driver.putCreatureOnBattlefield(opp, "Black Creature")
        val down = driver.putPermanentOnBattlefield(me, "Phoenix Down")
        driver.giveMana(me, Color.WHITE, 2)

        val abilityId = PhoenixDown.activatedAbilities[0].id
        (driver.submit(ActivateAbility(playerId = me, sourceId = down, abilityId = abilityId)).outcome is Outcome.Paused) shouldBe true

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(me, OptionChosenResponse(modeDecision.id, modeDecision.options.indexOfFirst { it.contains("Exile", ignoreCase = true) }))
        driver.submitTargetSelection(me, listOf(zombie))
        driver.bothPass()

        driver.findPermanent(opp, "Black Creature") shouldBe null
        driver.getExileCardNames(opp) shouldContain "Black Creature"
    }
})
