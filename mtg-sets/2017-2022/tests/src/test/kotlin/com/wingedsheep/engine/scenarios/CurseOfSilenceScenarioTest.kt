package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.CurseOfSilence
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Curse of Silence (MID #15) — {W} Enchantment — Aura Curse.
 *
 * "Spells with the chosen name enchanted player casts cost {2} more to cast" and the matching
 * "whenever enchanted player casts a spell with the chosen name" trigger are both keyed to the
 * player the Curse is *attached to*: the tax and the trigger leave the Curse's controller — and
 * spells of other names — alone.
 */
class CurseOfSilenceScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(CurseOfSilence))

        /** The Curse on player 1's battlefield, enchanting player 2, naming Grizzly Bears. */
        fun cursedOpponent(activePlayer: Int): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Curse of Silence")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(2, "Forest", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(activePlayer)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val curse = game.findPermanent("Curse of Silence")!!
            game.state = game.state.updateEntity(curse) {
                it.with(AttachedToComponent(game.player2Id))
                    .with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CARD_NAME to ChoiceValue.TextChoice("Grizzly Bears"))))
            }
            return game
        }

        fun TestGame.castCost(playerNumber: Int, name: String): String? {
            val cardId = findCardsInHand(playerNumber, name).first()
            return getLegalActions(playerNumber).first { (it.action as? CastSpell)?.cardId == cardId }.manaCostString
        }

        test("cast on an opponent: it enchants that player and records the chosen name") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Curse of Silence")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Curse of Silence", 2).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Grizzly Bears")))

            val curse = game.findPermanent("Curse of Silence")!!
            withClue("the Curse enchants the opponent") {
                game.state.getEntity(curse)?.get<AttachedToComponent>()?.targetId shouldBe game.player2Id
            }
            game.state.getEntity(curse)?.chosenCardName() shouldBe "Grizzly Bears"
        }

        test("enchanted player's spells with the chosen name cost {2} more; other names don't") {
            val game = cursedOpponent(activePlayer = 2)
            withClue("Grizzly Bears, the chosen name, is taxed") { game.castCost(2, "Grizzly Bears") shouldBe "{3}{G}" }
            withClue("Llanowar Elves is another name") { game.castCost(2, "Llanowar Elves") shouldBe "{G}" }
        }

        test("the Curse's controller casting the chosen name isn't taxed") {
            val game = cursedOpponent(activePlayer = 1)
            game.castCost(1, "Grizzly Bears") shouldBe "{1}{G}"
        }

        test("enchanted player casting the chosen name lets you sacrifice the Curse to draw") {
            val game = cursedOpponent(activePlayer = 2)
            val handBefore = game.handSize(1)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            withClue("the trigger offers the sacrifice") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true)
            game.resolveStack()

            withClue("the Curse was sacrificed") { game.findPermanent("Curse of Silence") shouldBe null }
            withClue("its controller drew a card") { game.handSize(1) shouldBe handBefore + 1 }
        }

        test("declining keeps the Curse and draws nothing") {
            val game = cursedOpponent(activePlayer = 2)
            val handBefore = game.handSize(1)

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.findPermanent("Curse of Silence") shouldNotBe null
            game.handSize(1) shouldBe handBefore
        }

        test("no trigger for another name, or for the chosen name cast by a player it doesn't enchant") {
            val opp = cursedOpponent(activePlayer = 2)
            opp.castSpell(2, "Llanowar Elves").error shouldBe null
            if (opp.hasPendingDecision()) opp.submitManaSourcesAutoPay()
            opp.resolveStack()
            withClue("Llanowar Elves isn't the chosen name") { opp.hasPendingDecision() shouldBe false }

            val you = cursedOpponent(activePlayer = 1)
            you.castSpell(1, "Grizzly Bears").error shouldBe null
            if (you.hasPendingDecision()) you.submitManaSourcesAutoPay()
            you.resolveStack()
            withClue("the Curse's controller isn't the enchanted player") { you.hasPendingDecision() shouldBe false }
            you.findPermanent("Curse of Silence") shouldNotBe null
        }
    }
}
