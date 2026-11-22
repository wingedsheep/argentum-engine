package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.MelekIzzetParagon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Melek, Izzet Paragon {4}{U}{R} — Legendary Creature — Weird Wizard 2/4 (DGM #84).
 * "Play with the top card of your library revealed.
 *  You may cast instant and sorcery spells from the top of your library.
 *  Whenever you cast an instant or sorcery spell from your library, copy it. You may choose new
 *  targets for the copy."
 *
 * Pins the composition: the top-of-library permission covers instants and sorceries only (no
 * lands, no creatures), and the copy trigger keys off the zone the spell was cast from — a
 * Lightning Bolt cast from the library is doubled, the same Bolt cast from hand is not.
 */
class MelekIzzetParagonScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MelekIzzetParagon))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Melek, Izzet Paragon")
        return Triple(driver, you, driver.getOpponent(you))
    }

    fun copyTriggers(driver: GameTestDriver) = driver.state.stack.mapNotNull {
        driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
    }.count { it.effect is CopyTargetSpellEffect }

    fun libraryCasts(driver: GameTestDriver, you: EntityId) =
        driver.legalActions(you).filter { it.sourceZone == "LIBRARY" }

    /** Empty the stack, keeping each copy aimed at [retargetTo]. */
    fun drain(driver: GameTestDriver, you: EntityId, retargetTo: EntityId) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) && guard++ < 40) {
            if (driver.state.pendingDecision is ChooseTargetsDecision) {
                driver.submitTargetSelection(you, listOf(retargetTo)).error shouldBe null
            } else {
                driver.bothPass()
            }
        }
    }

    test("an instant cast from the top of the library is copied") {
        val (driver, you, opponent) = setup()
        val bolt = driver.putCardOnTopOfLibrary(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)

        libraryCasts(driver, you).any { it.description.contains("Lightning Bolt") } shouldBe true
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        copyTriggers(driver) shouldBe 1

        drain(driver, you, opponent)
        driver.getLifeTotal(opponent) shouldBe 14
    }

    test("the same instant cast from hand is not copied") {
        val (driver, you, opponent) = setup()
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)

        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        copyTriggers(driver) shouldBe 0

        drain(driver, you, opponent)
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("a creature on top of the library can't be cast from there") {
        val (driver, you, _) = setup()
        driver.putCardOnTopOfLibrary(you, "Goblin Guide")
        driver.giveMana(you, Color.RED, 1)

        libraryCasts(driver, you).isEmpty() shouldBe true
    }

    test("a land on top of the library can't be played from there") {
        val (driver, you, _) = setup()
        driver.putCardOnTopOfLibrary(you, "Mountain")

        libraryCasts(driver, you).isEmpty() shouldBe true
    }
})
