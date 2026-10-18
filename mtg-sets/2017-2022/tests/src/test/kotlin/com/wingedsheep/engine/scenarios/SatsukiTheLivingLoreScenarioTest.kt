package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Satsuki, the Living Lore (NEO #235) — {G}{W} Legendary Creature — Human Druid, 1/3.
 *
 *   {T}: Put a lore counter on each Saga you control. Activate only as a sorcery.
 *   When Satsuki dies, choose up to one —
 *   • Return target Saga or enchantment creature you control to its owner's hand.
 *   • Return target Saga card from your graveyard to your hand.
 *
 * Pins that a lore counter added outside the precombat main phase still triggers the newly
 * reached chapter (The Flame of Keld's chapter II draws two), and the dies modal's graveyard mode.
 */
class SatsukiTheLivingLoreScenarioTest : ScenarioTestBase() {

    init {
        context("Satsuki, the Living Lore") {

            test("tap ability in the postcombat main phase advances a Saga and triggers its next chapter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Satsuki, the Living Lore")
                    .withCardInHand(1, "The Flame of Keld")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.castSpell(1, "The Flame of Keld").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 0

                val satsuki = game.findPermanent("Satsuki, the Living Lore")!!
                val abilityId = cardRegistry.getCard("Satsuki, the Living Lore")!!
                    .activatedAbilities.first().id
                game.execute(ActivateAbility(game.player1Id, satsuki, abilityId)).error shouldBe null
                game.resolveStack()

                val saga = game.findPermanent("The Flame of Keld")!!
                withClue("the Saga now has two lore counters") {
                    game.state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 2
                }
                withClue("chapter II triggered and drew two cards") {
                    game.handSize(1) shouldBe 2
                }
            }

            test("when Satsuki dies, the second mode returns a Saga card from the graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Satsuki, the Living Lore")
                    .withCardInGraveyard(1, "The Flame of Keld")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val satsuki = game.findPermanent("Satsuki, the Living Lore")!!
                game.castSpell(1, "Lightning Bolt", satsuki).error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected the dies-trigger mode choice; got ${game.getPendingDecision()}")
                // Modes with no legal target are filtered out, so locate the graveyard mode by its text.
                val graveyardMode = modeDecision.options.indexOfFirst { it.contains("from your graveyard") }
                withClue("options were ${modeDecision.options}") { (graveyardMode >= 0) shouldBe true }
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = graveyardMode))

                val sagaCard = game.findCardsInGraveyard(1, "The Flame of Keld").first()
                if (game.hasPendingDecision()) game.selectTargets(listOf(sagaCard))
                game.resolveStack()

                withClue("the Saga card returned to hand") {
                    game.isInHand(1, "The Flame of Keld") shouldBe true
                }
            }
        }
    }
}
