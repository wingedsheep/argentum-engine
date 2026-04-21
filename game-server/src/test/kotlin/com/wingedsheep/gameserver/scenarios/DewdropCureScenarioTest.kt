package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Regression tests for Dewdrop Cure's cast-time "up to N" targeting.
 *
 * Card text ({2}{W} sorcery):
 *   Gift a card. Return up to two target creature cards each with mana value 2 or less
 *   from your graveyard to the battlefield. If the gift was promised, instead return
 *   up to three.
 *
 * The web-client submits choose-1 modal casts with a flat `targets` list and
 * `chosenModes` populated but an empty `modeTargetsOrdered`. CastSpellHandler then
 * slices the flat list into per-mode groups via `deriveModeTargetsFromFlat`. A prior
 * bug required the flat-target count to exactly equal the mode's max `count`, so
 * picking fewer than the maximum for an "up to N" mode silently dropped every target
 * and nothing returned to the battlefield. These tests pin the partial-selection
 * behavior.
 */
class DewdropCureScenarioTest : ScenarioTestBase() {

    init {
        test("mode 0 (no gift) with one target returns the selected creature") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Dewdrop Cure")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Jungle Lion")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .build()

            val bearsId = game.state.getGraveyard(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
            }

            // Web-client shape: flat `targets` with chosenModes set but modeTargetsOrdered empty.
            // Max for mode 0 is "up to two" — select only one to exercise the partial path.
            val dewdropId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Dewdrop Cure"
            }
            val result = game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = dewdropId,
                targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD)),
                chosenModes = listOf(0)
            ))
            result.isSuccess shouldBe true

            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            // Jungle Lion was not picked — stays in the graveyard.
            game.isInGraveyard(1, "Jungle Lion") shouldBe true
            game.isInGraveyard(1, "Dewdrop Cure") shouldBe true
        }

        test("mode 1 (gift) with two targets returns both and opponent draws a card") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Dewdrop Cure")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Jungle Lion")
                .withCardInGraveyard(1, "Hill Giant") // MV 4 — not a legal target
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .build()

            val p2HandBefore = game.handSize(2)

            val bearsId = game.state.getGraveyard(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val lionId = game.state.getGraveyard(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Jungle Lion"
            }

            // Mode 1 allows "up to three" — pick two to exercise partial-of-max.
            val dewdropId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Dewdrop Cure"
            }
            val result = game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = dewdropId,
                targets = listOf(
                    ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD),
                    ChosenTarget.Card(lionId, game.player1Id, Zone.GRAVEYARD)
                ),
                chosenModes = listOf(1)
            ))
            result.isSuccess shouldBe true

            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Jungle Lion") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            game.isInGraveyard(1, "Jungle Lion") shouldBe false
            // Hill Giant stays — it was never a legal target (MV 4 > 2).
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            // Gift: opponent draws one card.
            game.handSize(2) shouldBe p2HandBefore + 1
        }

        test("mode 0 with both targets still works (sanity: max selection unchanged)") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Dewdrop Cure")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Jungle Lion")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .build()

            val bearsId = game.state.getGraveyard(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val lionId = game.state.getGraveyard(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Jungle Lion"
            }
            val dewdropId = game.state.getHand(game.player1Id).first { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Dewdrop Cure"
            }

            val result = game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = dewdropId,
                targets = listOf(
                    ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD),
                    ChosenTarget.Card(lionId, game.player1Id, Zone.GRAVEYARD)
                ),
                chosenModes = listOf(0)
            ))
            result.isSuccess shouldBe true

            game.resolveStack()

            val battlefieldNames = game.state.getBattlefield().mapNotNull { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name
            }
            battlefieldNames shouldContain "Grizzly Bears"
            battlefieldNames shouldContain "Jungle Lion"
        }
    }
}
