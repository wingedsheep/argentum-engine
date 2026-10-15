package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Halo Scarab (SNC #239) — {2} Artifact Creature — Insect, 2/1.
 *
 *   {2}, Exile this card from your graveyard: Create a Treasure token.
 */
class HaloScarabScenarioTest : ScenarioTestBase() {

    init {
        test("{2}, exiling it from the graveyard, creates a Treasure token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Halo Scarab")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scarab = game.findCardsInGraveyard(1, "Halo Scarab").single()
            val abilityId = cardRegistry.getCard("Halo Scarab")!!.activatedAbilities.first().id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = scarab, abilityId = abilityId))
                .error shouldBe null
            withClue("exiling the card is part of the cost") {
                game.isInExile(1, "Halo Scarab") shouldBe true
            }
            game.resolveStack()

            game.findPermanents("Treasure") shouldHaveSize 1
        }
    }
}
