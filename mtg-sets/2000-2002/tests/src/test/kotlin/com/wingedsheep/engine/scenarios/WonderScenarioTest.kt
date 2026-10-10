package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jud.cards.Wonder
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Wonder (JUD #54) — "Flying. As long as this card is in your graveyard and you control an
 * Island, creatures you control have flying."
 *
 * The graveyard half is a static that functions only from the graveyard (CR 113.6b). The granted
 * flying is a projected keyword, so blocking legality reads it: a ground creature can't block a
 * creature that flies only because Wonder is in its owner's graveyard.
 */
class WonderScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Wonder)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.flies(id: EntityId) = state.projectedState.hasKeyword(id, Keyword.FLYING)

    test("on the battlefield Wonder flies itself but grants nothing") {
        val driver = newGame()
        val you = driver.activePlayer!!
        driver.putLandOnBattlefield(you, "Island")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val wonder = driver.putCreatureOnBattlefield(you, "Wonder")

        driver.flies(wonder) shouldBe true
        driver.flies(bear) shouldBe false
    }

    test("from the graveyard with an Island, a ground blocker can't block your creature") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putLandOnBattlefield(you, "Island")
        driver.putCardInGraveyard(you, "Wonder")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.removeSummoningSickness(bear)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.flies(bear) shouldBe true
        driver.flies(blocker) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bear), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.state.step shouldBe Step.DECLARE_BLOCKERS
        driver.declareBlockers(opponent, mapOf(blocker to listOf(bear))).outcome shouldNotBe Outcome.Done
    }

    test("without an Island, or once Wonder leaves the graveyard, the grant is off") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val wonder = driver.putCardInGraveyard(you, "Wonder")
        driver.flies(bear) shouldBe false

        driver.putLandOnBattlefield(you, "Island")
        driver.flies(bear) shouldBe true

        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(you, Zone.GRAVEYARD), wonder)
                .addToZone(ZoneKey(you, Zone.EXILE), wonder)
        )
        driver.flies(bear) shouldBe false
    }
})
