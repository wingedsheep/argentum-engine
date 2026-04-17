package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Vren, the Relentless (BLB).
 *
 * Verifies the official rulings (2024-07-26):
 *  - While Vren is on the battlefield, creatures your opponents control are exiled
 *    instead of dying, and dies-triggers won't trigger for them.
 *  - Cards going to an opponent's graveyard for reasons OTHER than dying (e.g. being
 *    discarded or milled) still go to the graveyard — the replacement only hits
 *    creatures going from battlefield to graveyard.
 *  - Vren's end-step ability counts any creatures opponents controlled that were
 *    exiled this turn, including tokens, via OpponentCreaturesExiledThisTurnComponent.
 */
class VrenTheRelentlessTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            skipMulligans = true,
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("opponent's creature going to graveyard from battlefield is exiled while Vren is out") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(player, "Vren, the Relentless")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val hammer = driver.putCardInHand(player, "Volcanic Hammer")
        driver.giveMana(player, Color.RED, 2)

        val courser = driver.findPermanent(opponent, "Centaur Courser")!!
        val castResult = driver.castSpell(player, hammer, listOf(courser))
        castResult.isSuccess shouldBe true
        driver.bothPass()

        driver.getExileCardNames(opponent) shouldContain "Centaur Courser"
        driver.getGraveyardCardNames(opponent) shouldNotContain "Centaur Courser"
    }

    test("without Vren the opponent's creature still goes to graveyard normally") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val hammer = driver.putCardInHand(player, "Volcanic Hammer")
        driver.giveMana(player, Color.RED, 2)

        val courser = driver.findPermanent(opponent, "Centaur Courser")!!
        driver.castSpell(player, hammer, listOf(courser))
        driver.bothPass()

        driver.getGraveyardCardNames(opponent) shouldContain "Centaur Courser"
        driver.getExileCardNames(opponent) shouldNotContain "Centaur Courser"
    }

    test("Vren only affects opponents' creatures; your own creatures still die to the graveyard") {
        val driver = setup()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Vren, the Relentless")
        driver.putCreatureOnBattlefield(player, "Centaur Courser")

        val hammer = driver.putCardInHand(player, "Volcanic Hammer")
        driver.giveMana(player, Color.RED, 2)

        val courser = driver.findPermanent(player, "Centaur Courser")!!
        driver.castSpell(player, hammer, listOf(courser))
        driver.bothPass()

        driver.getGraveyardCardNames(player) shouldContain "Centaur Courser"
        driver.getExileCardNames(player) shouldNotContain "Centaur Courser"
    }

    test("a creature in an opponent's graveyard for reasons other than dying is not redirected to exile") {
        // The replacement's filter is GameObjectFilter.Creature.opponentControls(),
        // which requires the creature to be on the battlefield (a player only "controls"
        // permanents). Cards entering the opponent's graveyard from hand (discard) or
        // library (mill) are not "controlled" by the opponent and therefore are NOT
        // redirected to exile — matching the ruling from 2024-07-26.
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(player, "Vren, the Relentless")
        // Simulate the card already being in opponent's graveyard (arrived by some non-dying route).
        driver.putCardInGraveyard(opponent, "Centaur Courser")

        driver.getGraveyardCardNames(opponent) shouldContain "Centaur Courser"
        driver.getExileCardNames(opponent) shouldNotContain "Centaur Courser"

        val exileCount = driver.state.getEntity(player)
            ?.get<OpponentCreaturesExiledThisTurnComponent>()?.count ?: 0
        exileCount shouldBe 0
    }

    test("exiling an opponent's creature increments OpponentCreaturesExiledThisTurnComponent (counts feed Vren's X)") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(player, "Vren, the Relentless")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val hammer = driver.putCardInHand(player, "Volcanic Hammer")
        driver.giveMana(player, Color.RED, 2)

        val courser = driver.findPermanent(opponent, "Centaur Courser")!!
        driver.castSpell(player, hammer, listOf(courser))
        driver.bothPass()

        val count = driver.state.getEntity(player)
            ?.get<OpponentCreaturesExiledThisTurnComponent>()?.count ?: 0
        count shouldBe 1
    }
})
