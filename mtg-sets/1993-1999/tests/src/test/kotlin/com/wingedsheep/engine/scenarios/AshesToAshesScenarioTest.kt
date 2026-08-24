package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class AshesToAshesScenarioTest : ScenarioTestBase() {
    init {
        test("exiles two nonartifact creatures and deals 5 damage to its controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Ashes to Ashes")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLifeTotal(1, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Ashes to Ashes").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    listOf(
                        entityIdToChosenTarget(game.state, bears),
                        entityIdToChosenTarget(game.state, giant),
                    ),
                )
            )

            result.error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 15
        }

        test("still exiles the legal creature and deals damage when one target becomes illegal") {
            val game = createTargetedGame()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            castTargetingBoth(game, bears, giant)

            game.state = game.state.moveToZone(
                bears,
                ZoneKey(game.player2Id, Zone.BATTLEFIELD),
                ZoneKey(game.player2Id, Zone.GRAVEYARD),
            )
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 15
        }

        test("is countered and deals no damage when both targets become illegal") {
            val game = createTargetedGame()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            castTargetingBoth(game, bears, giant)

            game.state = game.state
                .moveToZone(
                    bears,
                    ZoneKey(game.player2Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player2Id, Zone.GRAVEYARD),
                )
                .moveToZone(
                    giant,
                    ZoneKey(game.player2Id, Zone.BATTLEFIELD),
                    ZoneKey(game.player2Id, Zone.GRAVEYARD),
                )
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }

    private fun createTargetedGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Ashes to Ashes")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLifeTotal(1, 20)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun castTargetingBoth(game: TestGame, first: com.wingedsheep.sdk.model.EntityId, second: com.wingedsheep.sdk.model.EntityId) {
        val spell = game.findCardsInHand(1, "Ashes to Ashes").single()
        game.execute(
            CastSpell(
                game.player1Id,
                spell,
                listOf(
                    entityIdToChosenTarget(game.state, first),
                    entityIdToChosenTarget(game.state, second),
                ),
            )
        ).error shouldBe null
    }
}
