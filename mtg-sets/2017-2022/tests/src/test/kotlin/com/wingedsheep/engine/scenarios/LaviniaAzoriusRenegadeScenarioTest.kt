package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Lavinia, Azorius Renegade — "Each opponent can't cast noncreature spells with mana value greater
 * than the number of lands that player controls. Whenever an opponent casts a spell, if no mana was
 * spent to cast it, counter that spell."
 *
 * The lock counts the *caster's* lands, never Lavinia's controller's; creature spells are exempt;
 * an {X} spell's mana value counts the X chosen for it.
 */
class LaviniaAzoriusRenegadeScenarioTest : FunSpec({

    /** Lavinia's controller is the non-active player, with no lands; the opponent is active in main 1. */
    fun setup(opponentLands: Int): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20, startingPlayer = 1)
        val opponent = driver.activePlayer!!
        val lavinia = driver.getOpponent(opponent)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(lavinia, "Lavinia, Azorius Renegade")
        repeat(opponentLands) { driver.putLandOnBattlefield(opponent, "Swamp") }
        return Triple(driver, lavinia, opponent)
    }

    fun GameTestDriver.canCast(playerId: EntityId, cardId: EntityId): Boolean =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .any { (it.action as? CastSpell)?.cardId == cardId }

    test("an opponent can't cast a noncreature spell with mana value above their land count") {
        val (driver, _, opponent) = setup(opponentLands = 2)
        val mindRot = driver.putCardInHand(opponent, "Mind Rot") // sorcery, MV 3
        driver.giveMana(opponent, Color.BLACK, 3)

        driver.canCast(opponent, mindRot) shouldBe false
    }

    test("the cap is the caster's own lands, not Lavinia's controller's") {
        // Lavinia's controller has no lands; the opponent has three, so MV 3 is allowed.
        val (driver, _, opponent) = setup(opponentLands = 3)
        val mindRot = driver.putCardInHand(opponent, "Mind Rot")
        driver.giveMana(opponent, Color.BLACK, 3)

        driver.canCast(opponent, mindRot) shouldBe true
    }

    test("creature spells are exempt whatever their mana value") {
        val (driver, _, opponent) = setup(opponentLands = 0)
        val bears = driver.putCardInHand(opponent, "Grizzly Bears") // creature, MV 2
        driver.giveMana(opponent, Color.GREEN, 2)

        driver.canCast(opponent, bears) shouldBe true
    }

    test("Lavinia's controller is not restricted") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(you, "Lavinia, Azorius Renegade")
        val mindRot = driver.putCardInHand(you, "Mind Rot")
        driver.giveMana(you, Color.BLACK, 3)

        driver.canCast(you, mindRot) shouldBe true
    }

    test("an X spell's mana value counts the chosen X") {
        val (driver, lavinia, opponent) = setup(opponentLands = 2)
        val blaze = driver.putCardInHand(opponent, "Blaze") // {X}{R}
        driver.giveMana(opponent, Color.RED, 4)

        // Offered: some X keeps it within the cap.
        driver.canCast(opponent, blaze) shouldBe true
        // X = 2 makes mana value 3 > 2 lands: rejected.
        driver.castXSpell(opponent, blaze, xValue = 2, targets = listOf(lavinia)).error.shouldNotBeNull()
        // X = 1 makes mana value 2: allowed.
        driver.castXSpell(opponent, blaze, xValue = 1, targets = listOf(lavinia)).error shouldBe null
    }

    test("counters an opponent's spell cast with no mana spent") {
        val (driver, _, opponent) = setup(opponentLands = 1)
        val mox = driver.putCardInHand(opponent, "Mox Ruby") // {0}: no mana spent
        driver.castSpell(opponent, mox).error shouldBe null
        driver.bothPass() // Lavinia's trigger resolves

        driver.state.getGraveyard(opponent).contains(mox) shouldBe true
        driver.findPermanent(opponent, "Mox Ruby") shouldBe null
    }

    test("does not counter an opponent's spell that mana was spent on") {
        val (driver, _, opponent) = setup(opponentLands = 1)
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        driver.giveMana(opponent, Color.GREEN, 2)
        driver.castSpell(opponent, bears).error shouldBe null
        driver.bothPass()

        driver.findPermanent(opponent, "Grizzly Bears").shouldNotBeNull()
    }
})
