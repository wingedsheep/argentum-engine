package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class RelearnScenarioTest : ScenarioTestBase() {
    init {
        for (cardName in listOf("Giant Growth", "Divination")) {
            test("returns $cardName from your graveyard to your hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Relearn")
                    .withCardInGraveyard(1, cardName)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.state.getGraveyard(game.player1Id).single()
                val spell = game.state.getHand(game.player1Id).single()
                val cast = game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = spell,
                    targets = listOf(ChosenTarget.Card(target, game.player1Id, Zone.GRAVEYARD))
                ))
                cast.error shouldBe null
                game.resolveStack()
                (target in game.state.getHand(game.player1Id)) shouldBe true
                (target in game.state.getGraveyard(game.player1Id)) shouldBe false
                (spell in game.state.getGraveyard(game.player1Id)) shouldBe true
            }
        }

        test("does not return a target exiled in response") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Relearn")
                .withCardInGraveyard(1, "Giant Growth")
                .withCardOnBattlefield(1, "Tormod's Crypt")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val target = game.state.getGraveyard(game.player1Id).single()
            val spell = game.state.getHand(game.player1Id).single()
            game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = spell,
                targets = listOf(ChosenTarget.Card(target, game.player1Id, Zone.GRAVEYARD))
            )).error shouldBe null
            game.execute(ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent("Tormod's Crypt")!!,
                abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(game.player1Id))
            )).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Giant Growth") shouldBe true
            (target in game.state.getHand(game.player1Id)) shouldBe false
            (spell in game.state.getGraveyard(game.player1Id)) shouldBe true
        }

        for ((owner, cardName) in listOf(1 to "Grizzly Bears", 2 to "Giant Growth")) {
            test("rejects $cardName in player $owner graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Relearn")
                    .withCardInGraveyard(owner, cardName)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val ownerId = if (owner == 1) game.player1Id else game.player2Id
                val target = game.state.getGraveyard(ownerId).single()
                val cast = game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = game.state.getHand(game.player1Id).single(),
                    targets = listOf(ChosenTarget.Card(target, ownerId, Zone.GRAVEYARD))
                ))
                (cast.error != null) shouldBe true
            }
        }
    }
}
