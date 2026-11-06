package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "You may cast target Zombie creature card from your graveyard this turn" (Zul Ashur, Lich Lord):
 * a pipeline that gathers its chosen target and grants a cast of it
 * ([com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect], or a cast on resolution,
 * [com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect]). The card
 * named is the one we get to cast — recursion through the stack, so the slot is a gift.
 *
 * Unread, the slot fell to the removal ranking, which ranks a card off the battlefield by its
 * *negated* value and names the worst card in the graveyard.
 */
class CastTargetFromGraveyardReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a cast or a granted cast of a target graveyard card reads its slot as a gift") {
            for (name in listOf("Torrential Gearhulk", "Heiko Yamazaki, the General", "Tinybones, the Pickpocket", "Ogre Battlecaster")) {
                val trigger = cardRegistry.getCard(name)!!.script.triggeredAbilities.single()
                withClue(name) {
                    intents.effectTargetPolarities(trigger.effect, trigger.allTargetRequirements) shouldBe
                        listOf(TargetPolarity.BENEFICIAL)
                }
            }
        }

        test("a card cast out of an opponent's graveyard is ours to take, one out of our own is not") {
            val tinybones = cardRegistry.getCard("Tinybones, the Pickpocket")!!.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.takenSlots(tinybones.effect, tinybones.allTargetRequirements) shouldBe setOf(0)
            val heiko = cardRegistry.getCard("Heiko Yamazaki, the General")!!.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.takenSlots(heiko.effect, heiko.allTargetRequirements) shouldBe emptySet()
        }

        test("the heuristic fill names the best Zombie card for Zul Ashur to recast, not the cheapest") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Zul Ashur, Lich Lord")
                .withCardInGraveyard(1, "Scathe Zombies")
                .withCardInGraveyard(1, "Rotting Regisaur")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val enumerator = LegalActionEnumerator(
                services.cardRegistry, services.manaSolver, services.costCalculator,
                services.predicateEvaluator, services.conditionEvaluator, services.turnManager,
            )
            val activation = enumerator.enumerate(game.state, game.player1Id).first {
                (it.action as? ActivateAbility)?.let { a ->
                    game.state.getEntity(a.sourceId)?.get<CardComponent>()?.name
                } == "Zul Ashur, Lich Lord"
            }

            val filled = TargetSelection.fillHeuristically(
                game.state, activation, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility
            val named = filled.targets.filterIsInstance<ChosenTarget.Card>().single().cardId
            withClue("casting a target card from our graveyard reads as recursion, a gift: the best card is named") {
                game.state.getEntity(named)?.get<CardComponent>()?.name shouldBe "Rotting Regisaur"
            }
        }
    }
}
