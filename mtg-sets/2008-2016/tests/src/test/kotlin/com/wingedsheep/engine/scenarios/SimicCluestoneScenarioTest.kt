package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.SimicCluestone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Simic Cluestone (DGM #145) — {3} Artifact.
 *
 *   {T}: Add {G} or {U}.
 *   {G}{U}, {T}, Sacrifice this artifact: Draw a card.
 */
class SimicCluestoneScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(SimicCluestone))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("tapping it adds either of its two colours") {
        for ((index, colour) in listOf(0 to "green", 1 to "blue")) {
            val d = driver()
            val you = d.activePlayer!!
            val stone = d.putPermanentOnBattlefield(you, "Simic Cluestone")
            d.submit(
                ActivateAbility(playerId = you, sourceId = stone, abilityId = SimicCluestone.activatedAbilities[index].id)
            ).outcome shouldBe Outcome.Done
            val pool = d.state.getEntity(you)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val amount = if (colour == "green") pool.green else pool.blue
            amount shouldBe 1
        }
    }

    test("paying {G}{U} and sacrificing it draws a card") {
        val d = driver()
        val you = d.activePlayer!!
        val stone = d.putPermanentOnBattlefield(you, "Simic Cluestone")
        d.giveMana(you, Color.GREEN, 1)
        d.giveMana(you, Color.BLUE, 1)
        val handBefore = d.getHandSize(you)

        d.submit(
            ActivateAbility(playerId = you, sourceId = stone, abilityId = SimicCluestone.activatedAbilities[2].id)
        ).outcome shouldBe Outcome.Done
        d.getGraveyardCardNames(you).contains("Simic Cluestone") shouldBe true

        d.bothPass()
        d.getHandSize(you) shouldBe handBefore + 1
    }
})
