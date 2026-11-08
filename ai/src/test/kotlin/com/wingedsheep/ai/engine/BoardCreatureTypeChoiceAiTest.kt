package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An open creature-type choice whose effect reads the battlefield ("destroy all creatures that
 * aren't of the chosen type") has to consider the types actually on the board, not just the ones
 * most common across each player's deck.
 */
class BoardCreatureTypeChoiceAiTest : ScenarioTestBase() {
    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("Kindred Judgment names the type that spares the AI's own creature on a wide board") {
            val names = listOf(
                "Kindred Judgment", "Graft Surgeon", "Mindseeker Oculus", "Thrull Wizard",
                "Emergency Phytomedic", "Unflinching Hortimancer", "Woodwork Prodigy", "Bloombrute",
                "Greenhouse Propagator",
            )
            val d = GameTestDriver()
            d.registerCards(TestCards.all + names.map(::card))
            d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            val opponent = d.getOpponent(me)

            d.putCreatureOnBattlefield(me, "Graft Surgeon")
            d.putCreatureOnBattlefield(me, "Mindseeker Oculus")
            // The deck is mostly Thrull Wizards, so the deck-wide type counts rank Thrull and
            // Wizard far above Homunculus — the type of the creature worth keeping.
            repeat(10) { d.putCardOnTopOfLibrary(me, "Thrull Wizard") }
            for (name in listOf(
                "Emergency Phytomedic", "Emergency Phytomedic", "Unflinching Hortimancer",
                "Unflinching Hortimancer", "Woodwork Prodigy", "Bloombrute", "Greenhouse Propagator",
                "Greenhouse Propagator",
            )) d.putCreatureOnBattlefield(opponent, name)

            val judgment = d.putCardInHand(me, "Kindred Judgment")
            d.giveMana(me, Color.WHITE, 7)
            d.castSpell(me, judgment)
            d.bothPass()

            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            val response = AIPlayer.create(cardRegistry, me, AiProfile.PRODUCTION_CHOICES)
                .respondToDecision(d.state, decision).shouldBeInstanceOf<OptionChosenResponse>()
            // Homunculus wipes every opposing creature and keeps Mindseeker Oculus; Wizard (the
            // old answer) wiped the AI's own board too.
            decision.options[response.optionIndex] shouldBe "Homunculus"
        }
    }
}
