package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.FontOfFertility
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Font of Fertility (JOU #123) — {G} Enchantment.
 *
 *   {1}{G}, Sacrifice this enchantment: Search your library for a basic land card, put it onto
 *   the battlefield tapped, then shuffle.
 */
class FontOfFertilityScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing it fetches a basic land onto the battlefield tapped") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Font of Fertility")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val fetched = game.findCardsInLibrary(1, "Forest").single()
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Font of Fertility")!!,
                    abilityId = FontOfFertility.activatedAbilities.first().id,
                )
            ).error shouldBe null

            withClue("the Font is sacrificed as a cost") {
                game.isInGraveyard(1, "Font of Fertility") shouldBe true
            }

            game.resolveStack()
            game.selectCards(listOf(fetched)).error shouldBe null
            game.resolveStack()

            withClue("the basic land entered the battlefield tapped") {
                game.findPermanents("Forest").contains(fetched) shouldBe true
                game.state.getEntity(fetched)?.has<TappedComponent>() shouldBe true
            }
            withClue("the non-land stays in the library") {
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }
    }
}
