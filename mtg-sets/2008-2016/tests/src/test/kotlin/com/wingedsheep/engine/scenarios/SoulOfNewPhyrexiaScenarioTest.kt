package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m15.cards.SoulOfNewPhyrexia
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Soul of New Phyrexia: the same ability is activatable from the battlefield, and from the graveyard by
 * exiling the card as part of the cost.
 */
class SoulOfNewPhyrexiaScenarioTest : ScenarioTestBase() {
    init {
        test("activated on the battlefield, permanents you control gain indestructible") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Soul of New Phyrexia")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Plains", 5)
                .build()
            val soul = game.findPermanent("Soul of New Phyrexia")!!
            game.execute(
                ActivateAbility(game.player1Id, soul, SoulOfNewPhyrexia.activatedAbilities[0].id)
            ).error shouldBe null
            game.resolveStack()
            withClue("the effect applied") {
                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.INDESTRUCTIBLE) shouldBe true
                game.state.projectedState.hasKeyword(game.findPermanent("Ornithopter")!!, Keyword.INDESTRUCTIBLE) shouldBe true
                game.state.projectedState.hasKeyword(game.findPermanent("Plains")!!, Keyword.INDESTRUCTIBLE) shouldBe true
            }
            game.isOnBattlefield("Soul of New Phyrexia") shouldBe true
        }

        test("activated from the graveyard, exiling the card: permanents you control gain indestructible") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInGraveyard(1, "Soul of New Phyrexia")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Plains", 5)
                .build()
            val soul = game.findCardsInGraveyard(1, "Soul of New Phyrexia").single()
            withClue("only the graveyard ability is offered for the card in the graveyard") {
                game.getLegalActions(1).map { it.action }.filterIsInstance<ActivateAbility>()
                    .filter { it.sourceId == soul }.map { it.abilityId }
                    .toSet() shouldBe setOf(SoulOfNewPhyrexia.activatedAbilities[1].id)
            }
            game.execute(
                ActivateAbility(game.player1Id, soul, SoulOfNewPhyrexia.activatedAbilities[1].id)
            ).error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Soul of New Phyrexia") shouldBe true
            }
            game.resolveStack()
            withClue("the effect applied") {
                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.INDESTRUCTIBLE) shouldBe true
                game.state.projectedState.hasKeyword(game.findPermanent("Ornithopter")!!, Keyword.INDESTRUCTIBLE) shouldBe true
                game.state.projectedState.hasKeyword(game.findPermanent("Plains")!!, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }
    }
}
