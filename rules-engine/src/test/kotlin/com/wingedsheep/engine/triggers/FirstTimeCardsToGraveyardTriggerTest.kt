package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.state.components.player.CardsPutIntoGraveyardThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * The `firstTimeEachTurn` axis of the batched "one or more [filter] cards are put into your
 * graveyard from anywhere" trigger. The window is turn history kept on the owner
 * ([CardsPutIntoGraveyardThisTurnComponent]); it closes on the turn's first matching card and
 * reopens when the turn's cleanup clears the record.
 */
class FirstTimeCardsToGraveyardTriggerTest : ScenarioTestBase() {

    private val watcher = card("First Land Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.oneOrMore(GameObjectFilter.Land).putIntoYourGraveyard(firstTimeEachTurn = true)
            effect = Effects.GainLife(1)
        }
    }

    private val razeLand = card("Test Raze Land") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Land)
            effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
        }
    }

    init {
        cardRegistry.register(listOf(watcher, razeLand))

        test("closes on the turn's first land and reopens next turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "First Land Watcher")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Swamp")
                .withCardInHand(1, "Test Raze Land")
                .withCardInHand(1, "Test Raze Land")
                .withCardInHand(2, "Test Raze Land")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val life = game.getLifeTotal(1)

            game.castSpell(1, "Test Raze Land", game.findPermanent("Plains")!!).error shouldBe null
            game.resolveStack()
            withClue("first land this turn triggers") { game.getLifeTotal(1) shouldBe life + 1 }
            withClue("the owner's turn history lists the Plains") {
                game.state.getEntity(game.player1Id)!!
                    .get<CardsPutIntoGraveyardThisTurnComponent>()!!.cardIds.size shouldBe 1
            }

            game.castSpell(1, "Test Raze Land", game.findPermanent("Island")!!).error shouldBe null
            game.resolveStack()
            withClue("second land the same turn doesn't") { game.getLifeTotal(1) shouldBe life + 1 }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("now the opponent's turn, and cleanup cleared the record") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getEntity(game.player1Id)!!.get<CardsPutIntoGraveyardThisTurnComponent>().shouldBeNull()
            }

            game.castSpell(2, "Test Raze Land", game.findPermanent("Swamp")!!).error shouldBe null
            game.resolveStack()
            withClue("a new turn reopens the window") { game.getLifeTotal(1) shouldBe life + 2 }
        }
    }
}
