package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.GhoulcallersAccomplice
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ghoulcaller's Accomplice (SOI #112) — {1}{B} 2/2 Human Rogue.
 *
 *   {3}{B}, Exile this card from your graveyard: Create a 2/2 black Zombie creature token.
 *   Activate only as a sorcery.
 */
class GhoulcallersAccompliceScenarioTest : ScenarioTestBase() {

    private val abilityId = GhoulcallersAccomplice.activatedAbilities.first().id

    init {
        test("from the graveyard, it exiles itself to make a 2/2 Zombie") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Ghoulcaller's Accomplice")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val accomplice = game.findCardsInGraveyard(1, "Ghoulcaller's Accomplice").single()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = accomplice, abilityId = abilityId)
            ).error shouldBe null
            game.isInExile(1, "Ghoulcaller's Accomplice") shouldBe true
            game.resolveStack()

            val zombie = game.findPermanent("Zombie Token").shouldNotBeNull()
            game.state.projectedState.getPower(zombie) shouldBe 2
            game.state.projectedState.getToughness(zombie) shouldBe 2
        }

        test("it can only be activated as a sorcery") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Ghoulcaller's Accomplice")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            val accomplice = game.findCardsInGraveyard(1, "Ghoulcaller's Accomplice").single()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = accomplice, abilityId = abilityId)
            ).error shouldNotBe null
            game.isInGraveyard(1, "Ghoulcaller's Accomplice") shouldBe true
        }
    }
}
