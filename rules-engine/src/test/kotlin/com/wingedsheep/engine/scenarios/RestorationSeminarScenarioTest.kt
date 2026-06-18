package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.ParadigmComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Restoration Seminar (Secrets of Strixhaven #30) — {5}{W}{W} Sorcery — Lesson.
 *
 * "Return target nonland permanent card from your graveyard to the battlefield.
 *  Paradigm (...)"
 *
 * Confirms the reanimation returns the targeted nonland permanent card to the battlefield, and
 * that — because of Paradigm — the resolved spell exiles itself (carrying the [ParadigmComponent]
 * marker) instead of going to the graveyard, seeding the recurring free-recast ability.
 */
class RestorationSeminarScenarioTest : ScenarioTestBase() {

    init {
        context("Restoration Seminar") {

            test("returns a nonland permanent card from graveyard and exiles itself via Paradigm") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Restoration Seminar")
                    .withCardInGraveyard(1, "Hill Giant") // 3/3 nonland permanent card
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(1, "Restoration Seminar", 1, "Hill Giant")
                withClue("Casting Restoration Seminar should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant is returned to the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                withClue("Restoration Seminar is NOT in the graveyard (Paradigm exiles it)") {
                    game.isInGraveyard(1, "Restoration Seminar") shouldBe false
                }

                // The resolved Paradigm spell lands in exile carrying the ParadigmComponent marker,
                // which seeds the recurring free-recast ability.
                val player1Id = game.state.turnOrder.first()
                val exiled = game.state.getZone(player1Id, Zone.EXILE)
                    .mapNotNull { game.state.getEntity(it) }
                    .filter { it.get<CardComponent>()?.name == "Restoration Seminar" }
                withClue("Restoration Seminar is in exile with the Paradigm marker") {
                    exiled.any { it.get<ParadigmComponent>() != null } shouldBe true
                }
            }
        }
    }
}
