package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.SqueeTheImmortal
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A card whose own static says "you may cast this card from exile" (Squee, the Immortal; Eternal
 * Scourge) is flagged [ClientCard.playableFromExile] for its owner, so the client shows it beside the
 * hand as a castable card — the exile twin of a graveyard card with a cast action. A face-down exiled
 * copy has no characteristics (CR 406.3a), so it isn't flagged, and the flag is never the opponent's.
 */
class SelfCastFromExileVisibilityTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SqueeTheImmortal)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    test("Squee in its owner's exile is playable from exile for the owner only") {
        val d = driver()
        val player = d.activePlayer!!
        val squee = d.putCardInExile(player, "Squee, the Immortal")

        transformer(d).transform(d.state, viewingPlayerId = player)
            .cards[squee].shouldNotBeNull().playableFromExile shouldBe true
        transformer(d).transform(d.state, viewingPlayerId = d.getOpponent(player))
            .cards[squee].shouldNotBeNull().playableFromExile shouldBe false
    }

    test("Squee exiled face down is not playable from exile") {
        val d = driver()
        val player = d.activePlayer!!
        val squee = d.putCardInExile(player, "Squee, the Immortal")
        d.replaceState(d.state.withEntity(squee, d.state.getEntity(squee)!!.with(FaceDownComponent)))

        transformer(d).transform(d.state, viewingPlayerId = player)
            .cards[squee]?.playableFromExile shouldBe false
    }
})
