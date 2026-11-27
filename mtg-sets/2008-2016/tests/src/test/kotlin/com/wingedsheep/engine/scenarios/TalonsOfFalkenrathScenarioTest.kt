package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.mtg.sets.definitions.dka.cards.TalonsOfFalkenrath
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TalonsOfFalkenrathScenarioTest : FunSpec({

    val grantedId = (TalonsOfFalkenrath.staticAbilities.single() as GrantActivatedAbility).ability.id

    test("flashed in at beginning of combat, the enchanted creature pumps itself +2/+0 for {1}{R}") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            passPriorityUntil(Step.BEGIN_COMBAT)
        }
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val talons = driver.putCardInHand(you, "Talons of Falkenrath")
        driver.giveMana(you, Color.RED, 2)
        driver.castSpell(you, talons, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(you, "Talons of Falkenrath") shouldNotBe null

        driver.giveMana(you, Color.RED, 2)
        driver.submitSuccess(ActivateAbility(playerId = you, sourceId = bears, abilityId = grantedId))
        driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }
})
