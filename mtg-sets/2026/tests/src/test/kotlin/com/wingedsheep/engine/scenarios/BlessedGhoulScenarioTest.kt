package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fra.cards.BlessedGhoul
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class BlessedGhoulScenarioTest : FunSpec({
    for (color in listOf(Color.WHITE, Color.BLACK)) {
        test("returns from the graveyard paying the hybrid cost with $color") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(BlessedGhoul)
            driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val player = driver.activePlayer!!
            val ghoul = driver.putCreatureOnBattlefield(player, "Blessed Ghoul")
            driver.state.projectedState.hasKeyword(ghoul, Keyword.LIFELINK) shouldBe true
            driver.giveMana(player, color, 1)
            driver.giveColorlessMana(player, 2)
            val ability = BlessedGhoul.script.activatedAbilities.single().id
            driver.submit(ActivateAbility(player, ghoul, ability)).error.shouldNotBeNull()
            driver.moveToGraveyard(ghoul)
            driver.submit(ActivateAbility(player, ghoul, ability)).error shouldBe null
            driver.bothPass()
            driver.findCardInHand(player, "Blessed Ghoul") shouldBe ghoul
        }
    }
})
