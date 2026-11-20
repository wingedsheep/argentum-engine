package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.GruulCluestone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gruul Cluestone (DGM #140) — {3} Artifact.
 *
 *   {T}: Add {R} or {G}.
 *   {R}{G}, {T}, Sacrifice this artifact: Draw a card.
 */
class GruulCluestoneScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(GruulCluestone))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("tapping it adds either of its two colours") {
        for ((index, colour) in listOf(0 to "red", 1 to "green")) {
            val d = driver()
            val you = d.activePlayer!!
            val stone = d.putPermanentOnBattlefield(you, "Gruul Cluestone")
            d.submit(
                ActivateAbility(playerId = you, sourceId = stone, abilityId = GruulCluestone.activatedAbilities[index].id)
            ).outcome shouldBe Outcome.Done
            val pool = d.state.getEntity(you)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val amount = if (colour == "red") pool.red else pool.green
            amount shouldBe 1
        }
    }

    test("paying {R}{G} and sacrificing it draws a card") {
        val d = driver()
        val you = d.activePlayer!!
        val stone = d.putPermanentOnBattlefield(you, "Gruul Cluestone")
        d.giveMana(you, Color.RED, 1)
        d.giveMana(you, Color.GREEN, 1)
        val handBefore = d.getHandSize(you)

        d.submit(
            ActivateAbility(playerId = you, sourceId = stone, abilityId = GruulCluestone.activatedAbilities[2].id)
        ).outcome shouldBe Outcome.Done
        d.getGraveyardCardNames(you).contains("Gruul Cluestone") shouldBe true

        d.bothPass()
        d.getHandSize(you) shouldBe handBefore + 1
    }
})
