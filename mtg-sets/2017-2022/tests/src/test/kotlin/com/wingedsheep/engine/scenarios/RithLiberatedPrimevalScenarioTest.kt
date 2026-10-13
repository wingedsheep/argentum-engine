package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.RithLiberatedPrimeval
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rith, Liberated Primeval (DMU #214) — "At the beginning of your end step, if a creature or
 * planeswalker an opponent controlled was dealt excess damage this turn, create a 4/4 red Dragon
 * creature token with flying."
 */
class RithLiberatedPrimevalScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + RithLiberatedPrimeval)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Rith, Liberated Primeval")
        return d
    }

    fun GameTestDriver.dragonTokens(): List<EntityId> =
        getCreatures(player1).filter { getCardName(it) == "Dragon Token" || getCardName(it) == "Dragon" }

    fun GameTestDriver.bolt(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.throughEndStep() {
        passPriorityUntil(Step.END)
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("excess damage to an opponent's creature makes a 4/4 flying Dragon at your end step, though it died") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Savannah Lions"))
        d.throughEndStep()
        val dragons = d.dragonTokens()
        dragons.size shouldBe 1
        val dragon = dragons.single()
        d.state.projectedState.getPower(dragon) shouldBe 4
        d.state.projectedState.getToughness(dragon) shouldBe 4
        d.state.projectedState.hasKeyword(dragon, Keyword.FLYING) shouldBe true
    }

    test("exactly lethal damage is not excess: no Dragon") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Centaur Courser"))
        d.throughEndStep()
        d.dragonTokens().size shouldBe 0
    }

    test("excess damage to your own creature doesn't count") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player1, "Savannah Lions"))
        d.throughEndStep()
        d.dragonTokens().size shouldBe 0
    }

    test("the token is a Dragon, so Rith gives it ward") {
        val d = driver()
        d.bolt(d.putCreatureOnBattlefield(d.player2, "Savannah Lions"))
        d.throughEndStep()
        withClue("other Dragons you control have ward {2}") {
            d.state.projectedState.hasKeyword(d.dragonTokens().single(), Keyword.WARD) shouldBe true
        }
    }
})
