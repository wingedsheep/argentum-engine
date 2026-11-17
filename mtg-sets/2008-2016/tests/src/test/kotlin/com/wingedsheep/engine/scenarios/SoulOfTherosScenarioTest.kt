package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.SoulOfTheros
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Soul of Theros: the same ability is activatable from the battlefield, and from the graveyard by
 * exiling the card as part of the cost.
 */
class SoulOfTherosScenarioTest : ScenarioTestBase() {
    init {
        test("activated on the battlefield, creatures you control get +2/+2 and gain first strike and lifelink") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Soul of Theros")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 6)
                .build()
            val soul = game.findPermanent("Soul of Theros")!!
            game.execute(
                ActivateAbility(game.player1Id, soul, SoulOfTheros.activatedAbilities[0].id)
            ).error shouldBe null
            game.resolveStack()
            withClue("the effect applied") {
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }
            game.isOnBattlefield("Soul of Theros") shouldBe true
        }

        test("activated from the graveyard, exiling the card: creatures you control get +2/+2 and gain first strike and lifelink") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInGraveyard(1, "Soul of Theros")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 6)
                .build()
            val soul = game.findCardsInGraveyard(1, "Soul of Theros").single()
            withClue("only the graveyard ability is offered for the card in the graveyard") {
                game.getLegalActions(1).map { it.action }.filterIsInstance<ActivateAbility>()
                    .filter { it.sourceId == soul }.map { it.abilityId }
                    .toSet() shouldBe setOf(SoulOfTheros.activatedAbilities[1].id)
            }
            game.execute(
                ActivateAbility(game.player1Id, soul, SoulOfTheros.activatedAbilities[1].id)
            ).error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Soul of Theros") shouldBe true
            }
            game.resolveStack()
            withClue("the effect applied") {
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }
        }
    }
}
