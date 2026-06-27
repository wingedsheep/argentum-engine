package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.StiltzkinMoogleMerchant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Stiltzkin, Moogle Merchant (FIN #34) — {W} Legendary Creature — Moogle.
 *
 * "Lifelink
 *  {2}, {T}: Target opponent gains control of another target permanent you control. If they do,
 *  you draw a card."
 *
 * The "if they do" rider is gated on the control change actually happening
 * (`SuccessCriterion.ControlChanged`):
 *  - Happy path: a legal donated permanent moves to the opponent, so you draw.
 *  - Edge case: the donated permanent leaves the battlefield in response (so no control moves at
 *    resolution) while the opponent target stays legal — you must NOT draw.
 */
class StiltzkinMoogleMerchantScenarioTest : FunSpec({

    val donateAbilityId = StiltzkinMoogleMerchant.activatedAbilities.first().id
    val projector = StateProjector()

    test("opponent gains control of the donated permanent and you draw") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StiltzkinMoogleMerchant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stiltzkin = driver.putCreatureOnBattlefield(player, "Stiltzkin, Moogle Merchant")
        driver.removeSummoningSickness(stiltzkin)
        val mountain = driver.putLandOnBattlefield(player, "Mountain")
        driver.giveMana(player, Color.WHITE, 2)

        val handBefore = driver.getHandSize(player)

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = stiltzkin,
                abilityId = donateAbilityId,
                targets = listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(mountain)),
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve the ability

        // The opponent now controls the donated Mountain...
        projector.project(driver.state).getController(mountain) shouldBe opponent
        // ...and the control change happened, so you drew a card.
        driver.getHandSize(player) shouldBe handBefore + 1
    }

    test("no draw when the donated permanent leaves the battlefield before resolution") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(StiltzkinMoogleMerchant))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val stiltzkin = driver.putCreatureOnBattlefield(player, "Stiltzkin, Moogle Merchant")
        driver.removeSummoningSickness(stiltzkin)
        val mountain = driver.putLandOnBattlefield(player, "Mountain")
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = stiltzkin,
                abilityId = donateAbilityId,
                targets = listOf(ChosenTarget.Player(opponent), ChosenTarget.Permanent(mountain)),
            )
        ).isSuccess shouldBe true

        // In response, the donated permanent leaves the battlefield: at resolution the control
        // change can't happen even though the opponent target is still legal.
        driver.moveToGraveyard(mountain)
        val handBefore = driver.getHandSize(player)

        driver.bothPass() // resolve the ability

        // No control change occurred, so the "if they do" draw is withheld.
        driver.getHandSize(player) shouldBe handBefore
        // The Mountain is in the graveyard, controlled by no one on the battlefield.
        driver.findPermanent(opponent, "Mountain") shouldBe null
    }
})
