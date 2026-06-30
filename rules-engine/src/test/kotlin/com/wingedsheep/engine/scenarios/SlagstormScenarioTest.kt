package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mbs.cards.Slagstorm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Slagstorm {1}{R}{R} Sorcery (MBS canonical; reprinted in FDN).
 *
 * Choose one —
 * • Slagstorm deals 3 damage to each creature.
 * • Slagstorm deals 3 damage to each player.
 */
class SlagstormScenarioTest : FunSpec({

    val Bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Slagstorm, Bear))
        return driver
    }

    test("mode 0 — deals 3 damage to each creature, leaving players untouched") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Test Bear")
        driver.putCreatureOnBattlefield(opp, "Test Bear")
        driver.giveMana(me, Color.RED, 3)
        val slag = driver.putCardInHand(me, "Slagstorm")

        driver.submit(CastSpell(playerId = me, cardId = slag, chosenModes = listOf(0))).isSuccess shouldBe true
        driver.bothPass()

        // 3 damage kills every 2/2; both players are untouched.
        driver.getCreatures(me).size shouldBe 0
        driver.getCreatures(opp).size shouldBe 0
        driver.getLifeTotal(me) shouldBe 20
        driver.getLifeTotal(opp) shouldBe 20
    }

    test("mode 1 — deals 3 damage to each player, leaving creatures untouched") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(me, "Test Bear")
        driver.giveMana(me, Color.RED, 3)
        val slag = driver.putCardInHand(me, "Slagstorm")

        driver.submit(CastSpell(playerId = me, cardId = slag, chosenModes = listOf(1))).isSuccess shouldBe true
        driver.bothPass()

        // Each player (including the caster) takes 3; the creature survives.
        driver.getLifeTotal(me) shouldBe 17
        driver.getLifeTotal(opp) shouldBe 17
        driver.getCreatures(me).size shouldBe 1
    }
})
