package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * David and Carol Marcus, Creators: "When David and Carol Marcus enter, create a tapped Planet
 * land token named New Planet with '{T}: Add one mana of any color.'"
 */
class DavidAndCarolMarcusCreatorsScenarioTest : ScenarioTestBase() {

    init {
        context("David and Carol Marcus, Creators") {
            test("entering creates one tapped colorless New Planet land token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "David and Carol Marcus, Creators")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "David and Carol Marcus, Creators").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                val tokens = game.findPermanents("New Planet")
                tokens.size shouldBe 1
                val token = game.state.getEntity(tokens.single())!!
                val card = token.get<CardComponent>()!!
                withClue("a colorless Planet land token, entered tapped") {
                    token.has<TokenComponent>() shouldBe true
                    token.has<TappedComponent>() shouldBe true
                    card.typeLine.isLand shouldBe true
                    card.typeLine.subtypes.map { it.value } shouldBe listOf("Planet")
                    card.colors.isEmpty() shouldBe true
                }
            }

            test("New Planet taps for a color no other land provides") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "David and Carol Marcus, Creators")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "New Planet", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("{2}{G} is payable only if New Planet makes green") {
                    game.castSpell(1, "David and Carol Marcus, Creators").error shouldBe null
                }
                game.resolveStack()
                game.resolveStack()

                game.findPermanents("David and Carol Marcus, Creators").size shouldBe 1
                game.findPermanents("New Planet").size shouldBe 2
            }
        }
    }
}
