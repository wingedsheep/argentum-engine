package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * The client view of an evoke card (CR 702.74) must carry its evoke cost, for the same reason
 * impending does: the enumerator only offers the casts the player can currently *afford*, so a
 * Mulldrifter on five lands arrives with an evoke cast and nothing else. Without the cost on the
 * card, the menu can't draw the hard cast next to it, and the drag-to-play path silently fires the
 * lone action — evoking (and sacrificing) a creature the player meant to hard-cast.
 *
 * Evoke is intrinsic to the card definition, so it rides on `ClientCard` in every zone.
 */
class EvokeClientViewTest : ScenarioTestBase() {

    private val transformer = com.wingedsheep.engine.view.ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator)
    private val p1 = EntityId.of("player-1")

    private fun clientCardInHand(name: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, name)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .let { game ->
            val cardId = game.state.getHand(p1).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == name
            }
            transformer.transform(game.state, p1).cards[cardId]!!
        }

    init {
        test("an evoke card in hand exposes its evoke cost") {
            withClue("Mulldrifter has Evoke {2}{U} against a printed {4}{U}") {
                clientCardInHand("Mulldrifter").evoke?.cost shouldBe "{2}{U}"
            }
        }

        test("a single-pip evoke cost survives the round-trip verbatim") {
            // Ingot Chewer's {R} against a printed {4}{R} — the widest gap the menu has to show as
            // two prices side by side.
            clientCardInHand("Ingot Chewer").evoke?.cost shouldBe "{R}"
        }

        test("a card without evoke exposes no evoke option") {
            clientCardInHand("Grizzly Bears").evoke.shouldBeNull()
        }
    }
}
