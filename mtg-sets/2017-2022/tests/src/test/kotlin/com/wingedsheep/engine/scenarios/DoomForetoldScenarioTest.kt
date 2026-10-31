package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Doom Foretold (ELD #187) — {2}{W}{B} Enchantment.
 *
 * "At the beginning of each player's upkeep, that player sacrifices a nonland, nontoken permanent
 * of their choice. If that player can't, they discard a card, they lose 2 life, you draw a card,
 * you gain 2 life, you create a 2/2 white Knight creature token with vigilance, then you
 * sacrifice this enchantment."
 *
 * The fallback splits its consequences between two players — "they" is the upkeep's player,
 * "you" is Doom Foretold's controller — and only fires when the edict took nothing. Both
 * directions are pinned: an opponent with only a land suffers the full chain, and an opponent
 * with a creature just loses it.
 */
class DoomForetoldScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        return d
    }

    fun GameTestDriver.advanceToUpkeepOf(player: EntityId) {
        passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (activePlayer != player) {
            passPriorityUntil(Step.DRAW, maxPasses = 200)
            passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        currentStep shouldBe Step.UPKEEP
        activePlayer shouldBe player
    }

    test("an opponent with only lands can't sacrifice, so the whole fallback happens") {
        val d = driver()
        val controller = d.player1
        val opponent = d.player2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val doom = d.putPermanentOnBattlefield(controller, "Doom Foretold")
        val land = d.putLandOnBattlefield(opponent, "Swamp")

        d.advanceToUpkeepOf(opponent)
        d.stackSize shouldBe 1
        val opponentHand = d.getHandSize(opponent)
        val controllerHand = d.getHandSize(controller)
        val opponentLife = d.getLifeTotal(opponent)
        val controllerLife = d.getLifeTotal(controller)
        val controllerCreatures = d.getCreatures(controller).size
        d.bothPass()

        // The opponent has a full hand, so the discard pauses for their choice.
        val discard = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        discard.playerId shouldBe opponent
        d.submitCardSelection(opponent, listOf(discard.options.first()))

        withClue("lands aren't legal sacrifices, so the opponent keeps theirs") {
            d.state.getBattlefield().contains(land) shouldBe true
        }
        withClue("the upkeep's player discards and loses 2 life") {
            d.getHandSize(opponent) shouldBe opponentHand - 1
            d.getLifeTotal(opponent) shouldBe opponentLife - 2
        }
        withClue("Doom Foretold's controller draws, gains 2 life and gets a Knight") {
            d.getHandSize(controller) shouldBe controllerHand + 1
            d.getLifeTotal(controller) shouldBe controllerLife + 2
            d.getCreatures(controller).size shouldBe controllerCreatures + 1
        }
        withClue("then Doom Foretold is sacrificed") {
            d.state.getBattlefield().contains(doom) shouldBe false
            d.getGraveyard(controller).contains(doom) shouldBe true
        }
    }

    test("an opponent with a nontoken creature sacrifices it and nothing else happens") {
        val d = driver()
        val controller = d.player1
        val opponent = d.player2
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val doom = d.putPermanentOnBattlefield(controller, "Doom Foretold")
        val bear = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        d.advanceToUpkeepOf(opponent)
        d.stackSize shouldBe 1
        val opponentLife = d.getLifeTotal(opponent)
        val controllerLife = d.getLifeTotal(controller)
        d.bothPass()

        withClue("the only legal sacrifice is taken, so the fallback never fires") {
            d.state.getBattlefield().contains(bear) shouldBe false
            d.state.getBattlefield().contains(doom) shouldBe true
            d.getLifeTotal(opponent) shouldBe opponentLife
            d.getLifeTotal(controller) shouldBe controllerLife
        }
    }
})
