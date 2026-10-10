package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jud.cards.Anger
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Anger (JUD #77) — "Haste. As long as this card is in your graveyard and you control a Mountain,
 * creatures you control have haste."
 *
 * The graveyard half is a static that functions only from the graveyard (CR 113.6b). On the
 * battlefield Anger is just a hasty 2/2; once it dies, every creature its owner controls has haste
 * while they control a Mountain — so a creature cast this turn can attack.
 */
class AngerScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Anger)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.hasHaste(id: com.wingedsheep.sdk.model.EntityId) =
        state.projectedState.hasKeyword(id, Keyword.HASTE)

    test("on the battlefield Anger has haste itself but grants nothing") {
        val driver = newGame()
        val you = driver.activePlayer!!
        driver.putLandOnBattlefield(you, "Mountain")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val anger = driver.putCreatureOnBattlefield(you, "Anger")

        driver.hasHaste(anger) shouldBe true
        driver.hasHaste(bear) shouldBe false
    }

    test("once Anger dies, a creature cast this turn can attack") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putLandOnBattlefield(you, "Mountain")
        val anger = driver.putCreatureOnBattlefield(you, "Anger")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(anger)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.assertInGraveyard(you, "Anger")

        val bears = driver.putCardInHand(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, bears).outcome shouldBe Outcome.Done
        driver.bothPass()
        val bear = driver.findPermanent(you, "Grizzly Bears")!!
        driver.hasHaste(bear) shouldBe true

        val round = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("declare attackers is skipped when nothing can attack, so this round proves the bear could") {
            driver.state.turnNumber shouldBe round
        }
        driver.declareAttackers(you, listOf(bear), opponent).outcome shouldBe Outcome.Done
    }

    test("no Mountain, no haste — and the opponent's creatures never get it") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val yourBear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putLandOnBattlefield(opponent, "Mountain")
        driver.putCardInGraveyard(you, "Anger")

        driver.hasHaste(yourBear) shouldBe false

        driver.putLandOnBattlefield(you, "Mountain")
        driver.hasHaste(yourBear) shouldBe true
        driver.hasHaste(theirBear) shouldBe false
    }
})
