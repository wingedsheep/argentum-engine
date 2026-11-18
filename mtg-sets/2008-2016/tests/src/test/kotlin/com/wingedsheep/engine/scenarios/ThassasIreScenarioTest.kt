package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.ThassasIre
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thassa's Ire (JOU #54) — {U} Enchantment.
 *
 *   {3}{U}: You may tap or untap target creature.
 */
class ThassasIreScenarioTest : ScenarioTestBase() {
    init {
        fun activateOn(game: TestGame, creature: EntityId) {
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Thassa's Ire")!!,
                    abilityId = ThassasIre.activatedAbilities.first().id,
                    targets = listOf(ChosenTarget.Permanent(creature)),
                )
            ).error shouldBe null
            game.resolveStack()
        }

        test("you may tap the target creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Thassa's Ire")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            activateOn(game, bears)

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            val mode = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(mode.id, mode.options.indexOfFirst { it.startsWith("Tap") }))
                .error shouldBe null
            game.resolveStack()

            withClue("the opponent's creature is tapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }

        test("you may untap the target creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Thassa's Ire")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            activateOn(game, bears)

            game.answerYesNo(true).error shouldBe null
            val mode = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(mode.id, mode.options.indexOfFirst { it.startsWith("Untap") }))
                .error shouldBe null
            game.resolveStack()

            withClue("your tapped creature is untapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }
        }

        test("declining the may leaves the creature as it was") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Thassa's Ire")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            activateOn(game, bears)

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            withClue("no mode was chosen and the Bears stay untapped") {
                game.hasPendingDecision() shouldBe false
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
