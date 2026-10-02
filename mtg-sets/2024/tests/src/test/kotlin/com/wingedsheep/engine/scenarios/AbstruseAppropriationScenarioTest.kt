package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AbstruseAppropriationScenarioTest : ScenarioTestBase() {
    private fun appropriateSerraAngel(wastes: Int): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Abstruse Appropriation")
            .withCardInHand(1, "Swords to Plowshares")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withLandsOnBattlefield(1, "Wastes", 2 + wastes)
            .withCardOnBattlefield(2, "Serra Angel")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val angel = game.findPermanent("Serra Angel")!!
        game.castSpell(1, "Abstruse Appropriation", angel).error shouldBe null
        game.resolveStack()
        val exiled = game.state.getExile(game.player2Id).single {
            game.state.getEntity(it)?.get<CardComponent>()?.name == "Serra Angel"
        }
        return game to exiled
    }

    init {
        test("exiles the target and lets you cast it paying its white pips with colorless mana") {
            val (game, angel) = appropriateSerraAngel(wastes = 5)
            game.isOnBattlefield("Serra Angel") shouldBe false

            // Only Wastes remain untapped: {3}{W}{W} is paid entirely with colorless mana.
            game.execute(CastSpell(game.player1Id, angel)).error shouldBe null
            game.resolveStack()
            val cast = game.findPermanent("Serra Angel")
            cast shouldNotBe null
            game.state.projectedState.getController(cast!!) shouldBe game.player1Id
        }

        test("colorless mana doesn't stretch — the full cost must still be paid") {
            val (game, angel) = appropriateSerraAngel(wastes = 4)
            game.execute(CastSpell(game.player1Id, angel)).error shouldNotBe null
            (angel in game.state.getExile(game.player2Id)) shouldBe true
        }

        test("the colorless permission covers only the exiled card's spell") {
            val (game, _) = appropriateSerraAngel(wastes = 5)
            val swords = game.findCardsInHand(1, "Swords to Plowshares").single()
            val target = game.findPermanents("Wastes").first()
            game.execute(CastSpell(game.player1Id, swords, targets = listOf(
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(target)
            ))).error shouldNotBe null
        }

        test("Abstruse Appropriation itself is colorless (devoid)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Abstruse Appropriation")
                .build()
            val card = game.findCardsInHand(1, "Abstruse Appropriation").single()
            game.state.getEntity(card)?.get<CardComponent>()?.colors shouldBe emptySet()
        }
    }
}
