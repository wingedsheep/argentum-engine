package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HighNoon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * High Noon {1}{W} Enchantment (OTJ canonical).
 *
 * "Each player can't cast more than one spell each turn."
 * "{4}{R}, Sacrifice this enchantment: It deals 5 damage to any target."
 *
 * The cast restriction is the global (`eachPlayer = true`) variant of `RestrictSpellsCastPerTurn`,
 * so it binds *every* player — including a player who doesn't control High Noon. The activated
 * ability sacrifices High Noon and deals 5 damage to any target.
 */
class OtjHighNoonScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + HighNoon)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    test("the controller can cast one spell but not a second while High Noon is in play") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putPermanentOnBattlefield(me, "High Noon")

        // First spell of the turn is castable.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castActionsFor(me, bolt1).isNotEmpty() shouldBe true
        driver.castSpellWithTargets(me, bolt1, listOf(ChosenTarget.Player(driver.getOpponent(me))))
        driver.bothPass()

        // Second spell this turn is blocked (one spell already cast).
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castActionsFor(me, bolt2) shouldHaveSize 0
    }

    test("eachPlayer: the OPPONENT who doesn't control High Noon is also limited to one spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // I control High Noon; the opponent does not.
        driver.putPermanentOnBattlefield(me, "High Noon")

        // With no spell cast yet, the opponent's first spell is castable.
        val oppBolt1 = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.castActionsFor(opp, oppBolt1).isNotEmpty() shouldBe true

        // Record that the opponent has already cast one spell this turn. Because High Noon's
        // restriction is global (eachPlayer), this caps the opponent at one spell even though
        // they don't control High Noon. `hasReachedSpellCastLimit` reads the per-player count
        // tally (`playerSpellsCastThisTurn`).
        driver.replaceState(
            driver.state.copy(
                playerSpellsCastThisTurn = mapOf(opp to 1)
            )
        )

        // Now the opponent's second spell this turn is blocked by the global restriction.
        val oppBolt2 = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.castActionsFor(opp, oppBolt2) shouldHaveSize 0
    }

    test("the sacrifice ability sacrifices High Noon and deals 5 damage to any target") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val highNoon = driver.putPermanentOnBattlefield(me, "High Noon")
        // {4}{R}: 5 red covers the colored pip plus the generic.
        driver.giveMana(me, Color.RED, 5)

        val abilityId = HighNoon.activatedAbilities[0].id
        val before = driver.getLifeTotal(opp)

        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = highNoon,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Player(opp))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // High Noon was sacrificed as part of the cost.
        driver.getPermanents(me).contains(highNoon) shouldBe false
        // The targeted player took 5 damage.
        driver.getLifeTotal(opp) shouldBe (before - 5)
    }
})
