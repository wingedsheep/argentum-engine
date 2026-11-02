package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenCardName
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Alpine Moon (M19 #128):
 *   As this enchantment enters, choose a nonbasic land card name.
 *   Lands your opponents control with the chosen name lose all land types and abilities, and they
 *   gain "{T}: Add one mana of any color."
 *
 * Player 2 casts it on their turn naming Tropical Island; Player 1 (the opponent) owns one copy and
 * Player 2 owns another, so the test sees both the affected and the exempt land.
 */
class AlpineMoonScenarioTest : ScenarioTestBase() {

    private fun TestGame.manaAbilities(player: Int, source: EntityId) =
        getLegalActions(player).filter { (it.action as? ActivateAbility)?.sourceId == source && it.isManaAbility }

    init {
        test("names only nonbasic lands, and strips the opponents' copies down to one any-color mana ability") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(2, "Alpine Moon")
                .withCardOnBattlefield(2, "Mountain")
                .withCardOnBattlefield(2, "Tropical Island")
                .withCardOnBattlefield(1, "Tropical Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (p1Island, p2Island) = game.findPermanents("Tropical Island").let { ids ->
                val p1 = ids.single { game.state.projectedState.getController(it) == game.player1Id }
                p1 to ids.single { it != p1 }
            }

            game.castSpell(2, "Alpine Moon")
            game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("Alpine Moon asks for a card name as it enters") {
                (decision is ChooseOptionDecision) shouldBe true
            }
            decision as ChooseOptionDecision
            withClue("Only nonbasic land names are offered") {
                decision.options shouldContain "Tropical Island"
                decision.options shouldNotContain "Mountain"
                decision.options shouldNotContain "Grizzly Bears"
            }
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Tropical Island")))

            game.state.getEntity(game.findPermanent("Alpine Moon")!!)?.chosenCardName() shouldBe "Tropical Island"

            val projected = game.state.projectedState
            withClue("The opponent's Tropical Island loses its land types but stays a land") {
                projected.getSubtypes(p1Island) shouldBe emptySet()
                projected.getTypes(p1Island) shouldContain "LAND"
            }
            withClue("Alpine Moon's controller's own copy is untouched") {
                projected.getSubtypes(p2Island) shouldContainExactlyInAnyOrder listOf("Forest", "Island")
            }

            // Hand priority to Player 1 so their mana abilities are enumerable.
            game.passPriority()
            withClue("The affected land keeps exactly the granted {T}: Add one mana of any color") {
                game.manaAbilities(1, p1Island).size shouldBe 1
            }
        }
    }
}
