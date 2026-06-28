package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.AmbrosiaWhiteheart
import com.wingedsheep.mtg.sets.definitions.fin.cards.ChocoSeekerOfParadise
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Choco, Seeker of Paradise (FIN) — {1}{G}{W}{U} Legendary Creature — Bird, 3/5.
 *
 *  - Whenever one or more Birds you control attack, look at that many cards from the top of your
 *    library. You may put one of them into your hand. Then put any number of land cards from among
 *    them onto the battlefield tapped and the rest into your graveyard.
 *  - Landfall — Whenever a land you control enters, Choco gets +1/+0 until end of turn.
 *
 * Both abilities are pure composition over existing primitives (group-attack trigger +
 * GatherCards / SelectFromCollection / MoveCollection pipeline; LandYouControlEnters + ModifyStats),
 * so this scenario test is the behavioural gate.
 */
class ChocoSeekerOfParadiseScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ChocoSeekerOfParadise)
        driver.registerCard(AmbrosiaWhiteheart) // a second Bird, so two Birds can attack
        return driver
    }

    /** Drain priority/stack until a player decision is pending (or nothing is left to resolve). */
    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 20) {
            bothPass()
        }
    }

    test("Landfall — a land entering gives Choco +1/+0 until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val choco = driver.putCreatureOnBattlefield(player, "Choco, Seeker of Paradise")
        projector.project(driver.state).getPower(choco) shouldBe 3

        // Play a land — landfall fires.
        val forest = driver.putCardInHand(player, "Forest")
        driver.playLand(player, forest)
        driver.resolveUntilDecision()

        withClue("Landfall should pump Choco to 4/5") {
            projector.project(driver.state).getPower(choco) shouldBe 4
        }
    }

    test("attack trigger — two Birds: look at two, decline hand, land enters tapped, the rest to graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two attacking Birds → "that many" = 2 cards looked at.
        val choco = driver.putCreatureOnBattlefield(player, "Choco, Seeker of Paradise")
        val ambrosia = driver.putCreatureOnBattlefield(player, "Ambrosia Whiteheart")
        driver.removeSummoningSickness(choco)
        driver.removeSummoningSickness(ambrosia)

        // Top of library: a non-land (Grizzly Bears), then a natural Forest (land).
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")

        val landsBefore = driver.getLands(player).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(choco, ambrosia), opponent)
        driver.resolveUntilDecision()

        // First decision: "you may put one of them into your hand." Decline.
        val handPick = driver.pendingDecision
        withClue("expected the optional hand-pick selection, got $handPick") {
            (handPick is SelectCardsDecision) shouldBe true
        }
        handPick as SelectCardsDecision
        withClue("two attacking Birds should reveal exactly two cards") {
            handPick.options.size shouldBe 2
        }
        driver.submitCardSelection(player, emptyList())
        driver.resolveUntilDecision()

        // Second decision: "put any number of land cards onto the battlefield tapped." Pick the Forest.
        val landPick = driver.pendingDecision as SelectCardsDecision
        val forestOption = landPick.options.first { driver.getCardName(it) == "Forest" }
        driver.submitCardSelection(player, listOf(forestOption))
        driver.resolveUntilDecision()

        // The land entered tapped; the non-land "rest" went to the graveyard.
        withClue("the chosen land should have entered the battlefield") {
            driver.getLands(player).size shouldBe landsBefore + 1
        }
        val newLand = driver.getLands(player).first { driver.getCardName(it) == "Forest" }
        withClue("land cards from this effect enter tapped") {
            driver.isTapped(newLand) shouldBe true
        }
        driver.getGraveyardCardNames(player) shouldContain "Grizzly Bears"
    }

    test("attack trigger — may put one of the looked-at cards into hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val choco = driver.putCreatureOnBattlefield(player, "Choco, Seeker of Paradise")
        val ambrosia = driver.putCreatureOnBattlefield(player, "Ambrosia Whiteheart")
        driver.removeSummoningSickness(choco)
        driver.removeSummoningSickness(ambrosia)

        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(choco, ambrosia), opponent)
        driver.resolveUntilDecision()

        // Put Grizzly Bears into hand.
        val handPick = driver.pendingDecision as SelectCardsDecision
        val bearOption = handPick.options.first { driver.getCardName(it) == "Grizzly Bears" }
        driver.submitCardSelection(player, listOf(bearOption))
        driver.resolveUntilDecision()

        // Then the land step (no lands need to be chosen — decline to keep the test focused on the hand pick).
        val landPick = driver.pendingDecision as SelectCardsDecision
        driver.submitCardSelection(player, emptyList())
        driver.resolveUntilDecision()

        withClue("Grizzly Bears should be in hand after the optional pick") {
            driver.getHand(player).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
            driver.getHandSize(player) shouldBe handBefore + 1
        }
    }
})
