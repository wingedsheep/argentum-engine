package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Target land gains '{T}: Add {C}{C}'" ([com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect]
 * granting a mana ability) hands that permanent's controller a mana source, so the slot is
 * [TargetPolarity.BENEFICIAL] and the effect is ramp.
 *
 * Unread, the slot fell back to the removal ranking, which puts any opponent's permanent ahead of
 * ours: Emrakul, the Exigent Doom's hand ability gave the opponent's Forest the two extra mana.
 *
 * The same change taught the readers the other shapes Reality Fracture leaned on — hand
 * disruption, regrowth pipelines, emblems, granted flashback — and the first test pins them.
 * A non-mana grant reads as its own effect for the holder's controller (Quicksmith Spy's
 * "{T}: Draw a card"), and as that effect's intent when it lasts beyond the turn.
 * Granted flashback reads right but changes no pick yet: an instant or sorcery card off the
 * battlefield has no value reading, so the best spell to give it to is a tie.
 */
class GrantActivatedAbilityReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    private fun tags(name: String) = CardIntentAnalyzer.analyze(card(name)).tags

    init {
        test("hand disruption, regrowth pipelines, emblems, granted mana and flashback each read") {
            withClue("Stingcaster Mage: granting flashback is a recursion aimed at our own card") {
                val trigger = card("Stingcaster Mage").script.triggeredAbilities.single()
                TargetPolarityAnalyzer.forRequirements(trigger.effect, listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements) shouldBe
                    listOf(TargetPolarity.BENEFICIAL)
                tags("Stingcaster Mage") shouldContain IntentTag.RECURSION
            }
            withClue("Mind Rot: 'target player discards two cards' is aimed at someone else's hand") {
                TargetPolarityAnalyzer.forSpell(card("Mind Rot")) shouldBe listOf(TargetPolarity.HARMFUL)
                tags("Mind Rot") shouldContain IntentTag.DISCARD
            }
            withClue("Compulsive Research: a draw-then-discard on a target player is a loot, not discard") {
                TargetPolarityAnalyzer.forSpell(card("Compulsive Research")) shouldBe listOf(TargetPolarity.BENEFICIAL)
                tags("Compulsive Research") shouldNotContain IntentTag.DISCARD
            }
            withClue("Rank Rat: 'each opponent discards a card' iterates opponents, so 'you' is the opponent") {
                tags("Rank Rat") shouldContain IntentTag.DISCARD
            }
            withClue("Wheel of Fortune: a symmetric discard is the wheel's price, not an attack on a hand") {
                tags("Wheel of Fortune") shouldNotContain IntentTag.DISCARD
            }
            withClue("Return to the Light Realms: our graveyard returned to the battlefield is recursion") {
                tags("Return to the Light Realms") shouldContain IntentTag.RECURSION
            }
            withClue("Ajani Resolute: the +2/+2 emblem is an anthem") {
                tags("Ajani Resolute") shouldContain IntentTag.ANTHEM
            }
            withClue("Chandra, Chill of Compliance: the 'whenever you cast a spell, draw' emblem is card draw") {
                tags("Chandra, Chill of Compliance") shouldContain IntentTag.DRAW
            }
            withClue("Emrakul, the Exigent Doom: untapping our lands and granting a land a mana ability are ramp") {
                tags("Emrakul, the Exigent Doom") shouldContain IntentTag.RAMP
            }
        }

        test("Quicksmith Spy: a lasting granted '{T}: Draw a card' is a draw engine for our artifact") {
            val trigger = card("Quicksmith Spy").script.triggeredAbilities.single()
            withClue("the holder's controller activates the granted draw, so the slot is a gift") {
                TargetPolarityAnalyzer.forRequirements(
                    trigger.effect, listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements,
                ) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            withClue("granted for as long as we control the Spy, the ability is ours from then on: card draw") {
                tags("Quicksmith Spy") shouldContain IntentTag.DRAW
            }
        }

        test("Emrakul, the Exigent Doom hands its mana ability to our land, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardInHand(1, "Emrakul, the Exigent Doom")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val emrakul = game.state.getZone(game.player1Id, Zone.HAND)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Emrakul, the Exigent Doom" }
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == emrakul }

            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as ActivateAbility

            val target = (filled.targets.single() as ChosenTarget.Permanent).entityId
            withClue("a granted '{T}: Add {C}{C}' is mana for the land's controller, so the land is ours to pick") {
                game.state.projectedState.getController(target) shouldBe game.player1Id
            }
        }
    }
}
