package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.FontOfIre
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Font of Ire (JOU #97) — {1}{R} Enchantment.
 *
 *   {3}{R}, Sacrifice this enchantment: It deals 5 damage to target player or planeswalker.
 */
class FontOfIreScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing it deals 5 damage to the targeted player") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Font of Ire")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Font of Ire")!!,
                    abilityId = FontOfIre.activatedAbilities.first().id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the sacrificed Font still deals its 5 damage") {
                game.getLifeTotal(2) shouldBe 15
                game.isInGraveyard(1, "Font of Ire") shouldBe true
            }
        }
    }
}
