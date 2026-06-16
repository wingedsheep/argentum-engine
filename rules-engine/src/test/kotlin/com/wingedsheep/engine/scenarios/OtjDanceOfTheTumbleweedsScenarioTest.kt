package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.DanceOfTheTumbleweeds
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dance of the Tumbleweeds — {1}{G} Sorcery (Spree).
 *
 * + {1} — Search your library for a basic land card or a Desert card, put it onto the battlefield, then shuffle.
 * + {3} — Create an X/X green Elemental creature token, where X is the number of lands you control.
 */
class OtjDanceOfTheTumbleweedsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + DanceOfTheTumbleweeds)
        return driver
    }

    test("Mode 0 fetches a basic land onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val landsBefore = driver.getLands(player).size

        val spell = driver.putCardInHand(player, "Dance of the Tumbleweeds")
        driver.giveMana(player, Color.GREEN, 1) // {G} base
        driver.giveColorlessMana(player, 2)      // {1} base generic + {1} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(emptyList())
            )
        )
        driver.bothPass() // resolve search

        // Find a Forest in the library to put onto the battlefield.
        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        driver.submitCardSelection(player, listOf(decision.options.first()))

        driver.getLands(player).size shouldBe landsBefore + 1
    }

    test("Mode 1 creates an X/X Elemental equal to the number of lands you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(4) { driver.putLandOnBattlefield(player, "Forest") }

        val spell = driver.putCardInHand(player, "Dance of the Tumbleweeds")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 4)      // {1} base + {3} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(emptyList())
            )
        )
        driver.bothPass() // resolve token creation

        val token = driver.findPermanent(player, "Elemental Token")
        token shouldNotBe null
        driver.state.projectedState.getPower(token!!) shouldBe 4
        driver.state.projectedState.getToughness(token) shouldBe 4
    }

    test("Both modes can be chosen together") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(2) { driver.putLandOnBattlefield(player, "Forest") }
        val bfBefore = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).size

        val spell = driver.putCardInHand(player, "Dance of the Tumbleweeds")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 5) // {1} base + {1} mode0 + {3} mode1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(emptyList(), emptyList())
            )
        )
        driver.bothPass() // begin resolving; search decision first

        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        driver.submitCardSelection(player, listOf(decision.options.first()))

        // A land entered (search) AND an Elemental token was created.
        driver.findPermanent(player, "Elemental Token") shouldNotBe null
        // +2: the fetched land and the token.
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).size shouldBe bfBefore + 2
    }
})
