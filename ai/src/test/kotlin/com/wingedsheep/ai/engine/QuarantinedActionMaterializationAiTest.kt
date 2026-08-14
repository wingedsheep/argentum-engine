package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ConditionalSelectionMinimum
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.ModalEnumerationMode
import com.wingedsheep.engine.legalactions.ModalLegalEnumeration
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class QuarantinedActionMaterializationAiTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
    }

    test("AI materializes choose-N modes and their ordered targets") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val first = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        val action = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpellModal",
            description = "modal",
            modalEnumeration = ModalLegalEnumeration(
                chooseCount = 2,
                minChooseCount = 2,
                allowRepeat = false,
                unavailableIndices = emptyList(),
                modes = listOf(
                    ModalEnumerationMode(0, "first", true, targetRequirements = listOf(
                        TargetInfo(0, "first", 1, 1, listOf(first))
                    )),
                    ModalEnumerationMode(1, "second", true, targetRequirements = listOf(
                        TargetInfo(0, "second", 1, 1, listOf(second))
                    )),
                ),
            ),
        )

        val chosen = TargetSelection.fillHeuristically(driver.state, action, player, true) as CastSpell

        chosen.chosenModes shouldContainExactly listOf(0, 1)
        chosen.modeTargetsOrdered.map { targets -> (targets.single() as ChosenTarget.Permanent).entityId } shouldBe
            listOf(first, second)
    }

    test("AI honors TargetOther distinctness across target slots") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val best = driver.putCreatureOnBattlefield(driver.player2, "Force of Nature")
        val other = driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        val action = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpell",
            description = "two different targets",
            requiresTargets = true,
            targetRequirements = listOf(
                TargetInfo(0, "first", 1, 1, listOf(best, other)),
                TargetInfo(1, "other", 1, 1, listOf(best, other), mustDifferFromEarlier = true),
            ),
        )

        val chosen = TargetSelection.fillHeuristically(driver.state, action, player, true) as CastSpell
        chosen.targets.map { (it as ChosenTarget.Permanent).entityId }.distinct().size shouldBe 2
    }

    test("conditional minimum selects a matching card when discarding fewer cards") {
        val driver = driver()
        val player = driver.activePlayer!!
        val creature = driver.putCardInHand(player, "Grizzly Bears")
        val land = driver.putCardInHand(player, "Forest")
        val decision = SelectCardsDecision(
            id = "conditional-discard",
            playerId = player,
            prompt = "Discard two cards",
            context = DecisionContext(),
            options = listOf(land, creature),
            minSelections = 1,
            maxSelections = 2,
            conditionalMinimums = listOf(
                ConditionalSelectionMinimum(
                    requiredSelections = 2,
                    minimumSelections = 1,
                    matchingOptions = listOf(creature),
                    description = "may select as few as 1 if selecting a creature card",
                )
            ),
        )
        val responder = DecisionResponder(GameSimulator(driver.cardRegistry), AIPlayer.defaultEvaluator())

        val response = responder.respond(driver.state, decision, player) as CardsSelectedResponse

        response.selectedCards shouldContainExactly listOf(creature)
    }

    test("AI never submits an action whose authoritative simulation is illegal") {
        val driver = driver()
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Grizzly Bears")
        val falselyAffordable = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpell",
            description = "cast without mana",
            affordable = true,
        )
        val pass = LegalAction(
            action = PassPriority(player),
            actionType = "PassPriority",
            description = "Pass",
        )

        val chosen = AIPlayer.create(driver.cardRegistry, player)
            .chooseFrom(driver.state, listOf(falselyAffordable, pass))

        chosen.action shouldBe PassPriority(player)
    }
})
