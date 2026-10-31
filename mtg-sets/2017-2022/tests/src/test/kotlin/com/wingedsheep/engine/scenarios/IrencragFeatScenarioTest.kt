package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.IrencragFeat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Irencrag Feat — "Add seven {R}. You can cast only one more spell this turn."
 *
 * The Feat itself and spells cast before it don't count against the allowance: after it resolves
 * exactly one more spell may be cast this turn.
 */
class IrencragFeatScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(IrencragFeat)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.boltOffered(you: EntityId, bolt: EntityId) =
        legalActions(you).any { (it.action as? CastSpell)?.cardId == bolt }

    test("adds seven red, then allows exactly one more spell this turn") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        // A spell cast before the Feat doesn't use up its allowance.
        val early = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.submitSuccess(CastSpell(you, early, listOf(ChosenTarget.Player(opponent))))
        driver.resolveStack()

        val feat = driver.putCardInHand(you, "Irencrag Feat")
        driver.giveMana(you, Color.RED, 4)
        driver.submitSuccess(CastSpell(you, feat))
        driver.resolveStack()
        withClue("seven red mana floating") {
            driver.state.getEntity(you)?.get<ManaPoolComponent>()?.getAmount(Color.RED) shouldBe 7
        }

        val first = driver.putCardInHand(you, "Lightning Bolt")
        withClue("one more spell is allowed") { driver.boltOffered(you, first) shouldBe true }
        driver.submitSuccess(CastSpell(you, first, listOf(ChosenTarget.Player(opponent))))
        driver.resolveStack()
        driver.getLifeTotal(opponent) shouldBe 14

        val second = driver.putCardInHand(you, "Lightning Bolt")
        withClue("a second spell after the Feat is not") { driver.boltOffered(you, second) shouldBe false }
        driver.submitExpectFailure(CastSpell(you, second, listOf(ChosenTarget.Player(opponent))))
    }
})
