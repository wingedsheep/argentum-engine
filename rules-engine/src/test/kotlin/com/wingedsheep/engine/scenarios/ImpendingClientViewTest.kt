package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The client view of an impending card (CR 702.176) must carry its impending alternative cost so
 * the action menu can always present the impending cast option next to the normal cast — graying
 * out whichever the player can't pay for — and annotate it with a time-counter glyph. The cost and
 * time-counter count are intrinsic to the card definition, so they ride on [ClientCard] in every
 * zone, independent of the legal actions (which only surface affordable casts).
 */
class ImpendingClientViewTest : ScenarioTestBase() {

    private val transformer = com.wingedsheep.engine.view.ClientStateTransformer(cardRegistry)
    private val p1 = EntityId.of("player-1")

    init {
        test("an impending card in hand exposes its impending cost and time-counter count") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Overlord of the Mistmoors")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.state.getHand(p1).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Overlord of the Mistmoors"
            }
            val card = transformer.transform(game.state, p1).cards[cardId]!!

            withClue("Overlord of the Mistmoors has Impending 4—{2}{W}{W}") {
                val impending = card.impending
                impending.shouldNotBeNull()
                impending.cost shouldBe "{2}{W}{W}"
                impending.time shouldBe 4
            }
        }

        test("a card without impending exposes no impending option") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.state.getHand(p1).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val card = transformer.transform(game.state, p1).cards[cardId]!!

            card.impending.shouldBeNull()
        }
    }
}
