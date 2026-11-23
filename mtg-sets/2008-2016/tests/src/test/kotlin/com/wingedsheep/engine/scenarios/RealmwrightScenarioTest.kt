package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.Realmwright
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Realmwright (GTC #45):
 *   As this creature enters, choose a basic land type.
 *   Lands you control are the chosen type in addition to their other types.
 *
 * The group form of the chosen-land-type grant: every land its controller controls gains the
 * type (keeping its own) and that type's intrinsic mana ability; opponents' lands don't.
 */
class RealmwrightScenarioTest : FunSpec({

    test("lands you control gain the chosen basic land type and its mana ability; opponents' lands don't") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Realmwright)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        val island = driver.putLandOnBattlefield(p1, "Island")
        val mountain = driver.putLandOnBattlefield(p1, "Mountain")
        val opposingIsland = driver.putLandOnBattlefield(p2, "Island")

        val realmwright = driver.putCardInHand(p1, "Realmwright")
        driver.castSpell(p1, realmwright).error shouldBe null
        driver.bothPass()

        val choice = driver.pendingDecision
        choice.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.options shouldContain "Forest"
        driver.submitDecision(p1, OptionChosenResponse(choice.id, choice.options.indexOf("Forest")))

        val projected = driver.state.projectedState
        projected.hasSubtype(mountain, "Forest").shouldBeTrue()
        projected.hasSubtype(mountain, "Mountain").shouldBeTrue()
        projected.hasSubtype(island, "Forest").shouldBeTrue()
        projected.hasSubtype(opposingIsland, "Forest").shouldBeFalse()

        driver.submitSuccess(ActivateAbility(p1, mountain, AbilityId.intrinsicMana('G')))
        (driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.green ?: 0) shouldBe 1
    }
})
