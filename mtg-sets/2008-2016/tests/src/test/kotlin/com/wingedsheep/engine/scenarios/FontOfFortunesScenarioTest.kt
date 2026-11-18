package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.FontOfFortunes
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Font of Fortunes (JOU #38) — {1}{U} Enchantment.
 *
 *   {1}{U}, Sacrifice this enchantment: Draw two cards.
 */
class FontOfFortunesScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing it draws two cards") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Font of Fortunes")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Font of Fortunes")!!,
                    abilityId = FontOfFortunes.activatedAbilities.first().id,
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("two cards drawn") {
                game.handSize(1) shouldBe handBefore + 2
                game.librarySize(1) shouldBe 1
            }
            withClue("the Font was sacrificed") {
                game.isInGraveyard(1, "Font of Fortunes") shouldBe true
            }
        }
    }
}
