package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Black Sun's Twilight ({X}{B}, Instant):
 * "Up to one target creature gets -X/-X until end of turn. If X is 5 or more, return a creature
 *  card with mana value X or less from your graveyard to the battlefield tapped."
 */
class BlackSunsTwilightScenarioTest : ScenarioTestBase() {

    private fun TestGame.castTwilight(target: EntityId?, xValue: Int): ExecutionResult {
        val cardId = state.getHand(player1Id).find {
            state.getEntity(it)?.get<CardComponent>()?.name == "Black Sun's Twilight"
        } ?: error("Black Sun's Twilight not in hand")
        val targets = listOfNotNull(target?.let { ChosenTarget.Permanent(it) })
        return execute(CastSpell(player1Id, cardId, targets, xValue))
    }

    init {
        test("X=2 gives -2/-2 to the target and returns nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Black Sun's Twilight")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castTwilight(bears, 2).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            withClue("X < 5: the graveyard creature stays put") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.findPermanent("Grizzly Bears") shouldBe null
            }
        }

        test("X=5 with no target returns a chosen creature card with MV <= 5, tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Black Sun's Twilight")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInGraveyard(1, "Hill Giant") // MV 4
                .withCardInGraveyard(1, "Grizzly Bears") // MV 2
                .withCardInGraveyard(1, "Craw Wurm") // MV 6 — not eligible
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castTwilight(null, 5).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull()
            val hillGiant = game.findCardsInGraveyard(1, "Hill Giant").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val options = (decision as com.wingedsheep.engine.core.SelectCardsDecision).options
            withClue("Only creature cards with MV <= X are offered") {
                options shouldContainExactlyInAnyOrder listOf(hillGiant, bears)
            }
            game.selectCards(listOf(hillGiant))

            val returned = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.state.getEntity(returned)!!.get<TappedComponent>() shouldNotBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Craw Wurm") shouldBe true
        }

        test("X=5 with a target that becomes illegal fizzles — no creature is returned") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Black Sun's Twilight")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castTwilight(giant, 5).error shouldBe null
            game.state = game.state
                .removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), giant)
                .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), giant)
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
