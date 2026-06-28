package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.CidTimelessArtificer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Cid, Timeless Artificer (FIN) — {2}{W}{U} Legendary Creature — Human Artificer 4/4.
 *
 *  "Artifact creatures and Heroes you control get +1/+1 for each Artificer you control and each
 *   Artificer card in your graveyard.
 *   A deck can have any number of cards named Cid, Timeless Artificer.
 *   Cycling {W}{U}"
 *
 * Covers the dynamic lord over the union "artifact creatures OR Heroes you control", the additive
 * battlefield + graveyard Artificer count (Cid counts itself), the `youControl` scoping, and Cycling.
 */
class CidTimelessArtificerScenarioTest : FunSpec({

    // A plain Hero creature to exercise the "Heroes you control" branch of the union filter.
    val testHero = CardDefinition.creature(
        name = "Test Hero",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Hero")),
        power = 2,
        toughness = 2,
    )

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CidTimelessArtificer)
        driver.registerCard(testHero)
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("buffs artifact creatures and Heroes you control by battlefield + graveyard Artificer count") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        // Cid is itself an Artificer you control → contributes 1 to the count on its own.
        driver.putCreatureOnBattlefield(me, "Cid, Timeless Artificer")

        val myArtifactCreature = driver.putCreatureOnBattlefield(me, "Artifact Creature") // base 2/2
        val myHero = driver.putCreatureOnBattlefield(me, "Test Hero")                     // base 2/2
        val myVanilla = driver.putCreatureOnBattlefield(me, "Centaur Courser")            // base 3/3, no buff
        val oppArtifactCreature = driver.putCreatureOnBattlefield(opp, "Artifact Creature") // opponent's, no buff

        withClue("Only Cid on battlefield, empty graveyard → bonus = 1 (Cid itself)") {
            driver.state.projectedState.getPower(myArtifactCreature) shouldBe 3
            driver.state.projectedState.getToughness(myArtifactCreature) shouldBe 3
            driver.state.projectedState.getPower(myHero) shouldBe 3
            driver.state.projectedState.getToughness(myHero) shouldBe 3
        }

        withClue("Vanilla (non-artifact, non-Hero) creature you control is unaffected") {
            driver.state.projectedState.getPower(myVanilla) shouldBe 3
            driver.state.projectedState.getToughness(myVanilla) shouldBe 3
        }

        withClue("Opponent's artifact creature is unaffected (the lord only buffs creatures you control)") {
            driver.state.projectedState.getPower(oppArtifactCreature) shouldBe 2
            driver.state.projectedState.getToughness(oppArtifactCreature) shouldBe 2
        }

        // Add two Artificer cards (more Cids) to YOUR graveyard → bonus becomes 1 + 2 = 3.
        driver.putCardInGraveyard(me, "Cid, Timeless Artificer")
        driver.putCardInGraveyard(me, "Cid, Timeless Artificer")

        withClue("Cid on battlefield (1) + two Artificer cards in graveyard (2) → bonus = 3") {
            driver.state.projectedState.getPower(myArtifactCreature) shouldBe 5
            driver.state.projectedState.getToughness(myArtifactCreature) shouldBe 5
            driver.state.projectedState.getPower(myHero) shouldBe 5
            driver.state.projectedState.getToughness(myHero) shouldBe 5
        }

        withClue("An Artificer card in the OPPONENT's graveyard must not count for you") {
            driver.putCardInGraveyard(opp, "Cid, Timeless Artificer")
            driver.state.projectedState.getPower(myArtifactCreature) shouldBe 5
        }
    }

    test("Cycling {W}{U} discards Cid and draws a card") {
        val driver = newDriver()
        val me = driver.player1

        val cid = driver.putCardInHand(me, "Cid, Timeless Artificer")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)

        val handBefore = driver.getHandSize(me)
        driver.submit(CycleCard(playerId = me, cardId = cid)).isSuccess shouldBe true
        driver.bothPass() // cycling ability resolves: draw a card

        withClue("Cid is discarded to the graveyard and a replacement card is drawn (net hand unchanged)") {
            driver.getGraveyardCardNames(me) shouldContain "Cid, Timeless Artificer"
            driver.getHandSize(me) shouldBe handBefore
        }
    }
})
