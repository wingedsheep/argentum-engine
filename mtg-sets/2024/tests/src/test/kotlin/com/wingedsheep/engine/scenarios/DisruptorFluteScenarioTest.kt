package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Disruptor Flute ({2}, Artifact — MH3 #209):
 *   Flash
 *   As this artifact enters, choose a card name.
 *   Spells with the chosen name cost {3} more to cast.
 *   Activated abilities of sources with the chosen name can't be activated unless they're mana abilities.
 */
class DisruptorFluteScenarioTest : ScenarioTestBase() {

    private fun TestGame.castFluteNaming(name: String) {
        castSpell(1, "Disruptor Flute")
        resolveStack()
        val decision = getPendingDecision()
        withClue("Disruptor Flute must present an as-enters card-name choice") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        decision.options shouldContain name
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    private fun TestGame.castCostOf(cardId: EntityId): String? =
        getLegalActions(1).firstOrNull { (it.action as? CastSpell)?.cardId == cardId }?.manaCostString

    private fun TestGame.nonManaAbilityCount(sourceId: EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { !it.isManaAbility }

    private fun TestGame.manaAbilityCount(sourceId: EntityId): Int =
        getLegalActions(1)
            .filter { (it.action as? ActivateAbility)?.sourceId == sourceId }
            .count { it.isManaAbility }

    init {
        test("spells with the chosen name cost {3} more, including its controller's") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInHand(1, "Grizzly Bears").first()
            game.castCostOf(bears) shouldBe "{1}{G}"

            game.castFluteNaming("Grizzly Bears")
            game.state.getEntity(game.findPermanent("Disruptor Flute")!!)?.chosenCardName() shouldBe "Grizzly Bears"

            withClue("Grizzly Bears now costs {3} more") {
                game.castCostOf(bears) shouldBe "{4}{G}"
            }
        }

        test("naming a different card leaves other spells untaxed") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInHand(1, "Grizzly Bears").first()
            game.castFluteNaming("Keldon Necropolis")
            game.castCostOf(bears) shouldBe "{1}{G}"
        }

        test("named source's non-mana abilities are locked but its mana abilities still work") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Disruptor Flute")
                .withCardOnBattlefield(1, "Keldon Necropolis", tapped = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Mountain", 10)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val necropolis = game.findPermanent("Keldon Necropolis")!!
            game.nonManaAbilityCount(necropolis) shouldBe 1

            game.castFluteNaming("Keldon Necropolis")

            withClue("Non-mana ability locked") { game.nonManaAbilityCount(necropolis) shouldBe 0 }
            withClue("Mana ability unaffected") { game.manaAbilityCount(necropolis) shouldNotBe 0 }
        }
    }
}
