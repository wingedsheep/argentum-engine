package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.PetraSphinx
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Petra Sphinx (LEG) — "{T}: Target player chooses a card name, then reveals the top card of their
 * library. If that card has the chosen name, that player puts it into their hand. If it doesn't, the
 * player puts it into their graveyard."
 *
 * The load-bearing claim is that the *target player* names the card, not the Sphinx's controller.
 */
class PetraSphinxScenarioTest : FunSpec({

    val cards = TestCards.all + PetraSphinx

    fun GameTestDriver.activateOn(sphinx: com.wingedsheep.sdk.model.EntityId, target: com.wingedsheep.sdk.model.EntityId) {
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = sphinx,
                abilityId = PetraSphinx.script.activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Player(target))
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.name(cardName: String) {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("the target player names the card, not the Sphinx's controller") {
            decision.playerId shouldBe player2
        }
        submitDecision(player2, OptionChosenResponse(decision.id, decision.options.indexOf(cardName)))
            .error shouldBe null
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("a correctly named top card goes to the target player's hand") {
        val d = driver()
        val sphinx = d.putCreatureOnBattlefield(d.player1, "Petra Sphinx")
        d.removeSummoningSickness(sphinx)
        val top = d.putCardOnTopOfLibrary(d.player2, "Grizzly Bears")

        d.activateOn(sphinx, d.player2)
        d.name("Grizzly Bears")

        d.getHand(d.player2) shouldContain top
        d.getGraveyard(d.player2) shouldNotContain top
    }

    test("a misnamed top card goes to the target player's graveyard") {
        val d = driver()
        val sphinx = d.putCreatureOnBattlefield(d.player1, "Petra Sphinx")
        d.removeSummoningSickness(sphinx)
        val top = d.putCardOnTopOfLibrary(d.player2, "Grizzly Bears")

        d.activateOn(sphinx, d.player2)
        d.name("Hill Giant")

        d.getGraveyard(d.player2) shouldContain top
        d.getHand(d.player2) shouldNotContain top
    }
})
