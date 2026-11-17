package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.SoulOfZendikar
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Soul of Zendikar: the same ability is activatable from the battlefield, and from the graveyard by
 * exiling the card as part of the cost.
 */
class SoulOfZendikarScenarioTest : ScenarioTestBase() {
    init {
        test("activated on the battlefield, you create a 3/3 green Beast") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Soul of Zendikar")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .build()
            val soul = game.findPermanent("Soul of Zendikar")!!
            game.execute(
                ActivateAbility(game.player1Id, soul, SoulOfZendikar.activatedAbilities[0].id)
            ).error shouldBe null
            game.resolveStack()
            withClue("the effect applied") {
                game.findPermanents("Beast Token").size shouldBe 1
            }
            game.isOnBattlefield("Soul of Zendikar") shouldBe true
        }

        test("activated from the graveyard, exiling the card: you create a 3/3 green Beast") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInGraveyard(1, "Soul of Zendikar")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .build()
            val soul = game.findCardsInGraveyard(1, "Soul of Zendikar").single()
            withClue("only the graveyard ability is offered for the card in the graveyard") {
                game.getLegalActions(1).map { it.action }.filterIsInstance<ActivateAbility>()
                    .filter { it.sourceId == soul }.map { it.abilityId }
                    .toSet() shouldBe setOf(SoulOfZendikar.activatedAbilities[1].id)
            }
            game.execute(
                ActivateAbility(game.player1Id, soul, SoulOfZendikar.activatedAbilities[1].id)
            ).error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Soul of Zendikar") shouldBe true
            }
            game.resolveStack()
            withClue("the effect applied") {
                game.findPermanents("Beast Token").size shouldBe 1
            }
        }
    }
}
