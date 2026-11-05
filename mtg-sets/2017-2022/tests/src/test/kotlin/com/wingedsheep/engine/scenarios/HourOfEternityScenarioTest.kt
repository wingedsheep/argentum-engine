package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hour of Eternity ({X}{X}{U}{U}{U}, Sorcery):
 * "Exile X target creature cards from your graveyard. For each card exiled this way, create a token
 *  that's a copy of that card, except it's a 4/4 black Zombie."
 */
class HourOfEternityScenarioTest : ScenarioTestBase() {

    private fun TestGame.castHour(targets: List<EntityId>, xValue: Int): ExecutionResult {
        val cardId = state.getHand(player1Id).find {
            state.getEntity(it)?.get<CardComponent>()?.name == "Hour of Eternity"
        } ?: error("Hour of Eternity not in hand")
        return execute(
            CastSpell(player1Id, cardId, targets.map { ChosenTarget.Card(it, player1Id, Zone.GRAVEYARD) }, xValue)
        )
    }

    init {
        test("X=2 exiles both targets and makes 4/4 black Zombie copies that keep their mana cost") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Hour of Eternity")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.castHour(listOf(bears, giant), 2).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Hill Giant") shouldBe true
            withClue("the untargeted card stays in the graveyard and is not copied") {
                game.isInGraveyard(1, "Llanowar Elves") shouldBe true
                game.findPermanent("Llanowar Elves") shouldBe null
            }

            val projected = game.state.projectedState
            for ((name, manaValue) in listOf("Grizzly Bears" to 2, "Hill Giant" to 4)) {
                val token = game.findPermanent(name)
                token shouldNotBe null
                token!!
                withClue("$name token") {
                    game.state.getEntity(token)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
                    projected.getPower(token) shouldBe 4
                    projected.getToughness(token) shouldBe 4
                    projected.getColors(token) shouldBe setOf("BLACK")
                    projected.getSubtypes(token) shouldBe setOf("Zombie")
                    projected.isCreature(token) shouldBe true
                    game.state.getEntity(token)!!.get<CardComponent>()!!.manaCost.cmc shouldBe manaValue
                }
            }
        }

        test("X=1 cannot target two creature cards") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Hour of Eternity")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            (game.castHour(listOf(bears, giant), 1).error != null) shouldBe true
        }
    }
}
