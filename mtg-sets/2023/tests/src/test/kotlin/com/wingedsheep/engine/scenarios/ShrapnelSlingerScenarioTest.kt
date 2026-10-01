package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.ShrapnelSlinger
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Shrapnel Slinger (ONE #148) — "When this creature enters, you may sacrifice a creature.
 * When you do, destroy target artifact an opponent controls."
 */
class ShrapnelSlingerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShrapnelSlinger))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castSlinger(driver: GameTestDriver, playerId: EntityId): EntityId {
        val card = driver.putCardInHand(playerId, "Shrapnel Slinger")
        driver.giveMana(playerId, Color.RED, 1)
        driver.giveColorlessMana(playerId, 1)
        driver.castSpell(playerId, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the ETB trigger
        return card
    }

    test("sacrificing a creature destroys target artifact an opponent controls") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppArtifact = driver.putCreatureOnBattlefield(opp, "Artifact Creature")

        val slinger = castSlinger(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true).error shouldBe null
        driver.submitTargetSelection(me, listOf(fodder)).error shouldBe null
        driver.submitTargetSelection(me, listOf(oppArtifact)).error shouldBe null
        driver.bothPass()

        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.getGraveyard(opp).contains(oppArtifact) shouldBe true
        driver.state.getBattlefield(me).contains(slinger) shouldBe true
    }

    test("the Slinger can sacrifice itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val oppArtifact = driver.putCreatureOnBattlefield(opp, "Artifact Creature")

        val slinger = castSlinger(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true).error shouldBe null
        // The Slinger is the only creature, so the sacrifice choice auto-picks it.
        driver.getGraveyard(me).contains(slinger) shouldBe true
        driver.submitTargetSelection(me, listOf(oppArtifact)).error shouldBe null
        driver.bothPass()

        driver.getGraveyard(me).contains(slinger) shouldBe true
        driver.getGraveyard(opp).contains(oppArtifact) shouldBe true
    }

    test("declining the sacrifice destroys nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppArtifact = driver.putCreatureOnBattlefield(opp, "Artifact Creature")

        castSlinger(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false).error shouldBe null

        driver.state.getBattlefield(me).contains(fodder) shouldBe true
        driver.state.getBattlefield(opp).contains(oppArtifact) shouldBe true
    }
})
