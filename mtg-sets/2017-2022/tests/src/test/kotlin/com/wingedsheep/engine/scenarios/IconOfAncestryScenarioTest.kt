package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.IconOfAncestry
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Icon of Ancestry (M20 #229) — {3} Artifact.
 *
 * "As this artifact enters, choose a creature type.
 *  Creatures you control of the chosen type get +1/+1.
 *  {3}, {T}: Look at the top three cards of your library. You may reveal a creature card of the
 *  chosen type from among them and put it into your hand. Put the rest on the bottom of your
 *  library in a random order."
 *
 * The creature-type choice is seeded directly on the permanent (Roaming Throne test shape).
 */
class IconOfAncestryScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy { IconOfAncestry.activatedAbilities.single().id }

    private fun TestGame.chooseType(type: String) {
        val icon = findPermanent("Icon of Ancestry")!!
        state = state.updateEntity(icon) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    init {
        context("Icon of Ancestry") {
            test("creatures you control of the chosen type get +1/+1, others do not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Icon of Ancestry")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseType("Goblin")

                val projected = game.state.projectedState
                val myGoblin = game.findPermanents("Raging Goblin").single {
                    game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id
                }
                val theirGoblin = game.findPermanents("Raging Goblin").single { it != myGoblin }
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("your Goblin 1/1 -> 2/2") {
                    projected.getPower(myGoblin) shouldBe 2
                    projected.getToughness(myGoblin) shouldBe 2
                }
                withClue("non-Goblin and the opponent's Goblin are unchanged") {
                    projected.getPower(bears) shouldBe 2
                    projected.getPower(theirGoblin) shouldBe 1
                }
            }

            test("{3}, {T}: takes a creature of the chosen type from the top three into hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Icon of Ancestry")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Raging Goblin")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.chooseType("Goblin")

                val goblin = game.findCardsInLibrary(1, "Raging Goblin").single()

                game.execute(
                    ActivateAbility(game.player1Id, game.findPermanent("Icon of Ancestry")!!, abilityId)
                ).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(goblin)).error shouldBe null
                if (game.hasPendingDecision()) game.resolveStack()

                game.isInHand(1, "Raging Goblin") shouldBe true
                withClue("the other two go to the bottom of the library") {
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                    game.librarySize(1) shouldBe 2
                }
            }
        }
    }
}
