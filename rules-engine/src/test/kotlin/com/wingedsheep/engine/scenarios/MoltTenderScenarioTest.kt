package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Molt Tender (DFT #171) — {G} Creature — Insect Druid, 1/1.
 *
 *   "{T}: Mill a card."
 *   "{T}, Exile a card from your graveyard: Add one mana of any color."
 *
 * Two tap abilities competing for one untap. The second is a mana ability (no target, could add
 * mana — CR 605.1a), so it resolves without the stack after the color choice.
 */
class MoltTenderScenarioTest : ScenarioTestBase() {

    init {
        context("Molt Tender") {

            test("{T}: Mill a card puts the top card of your library into your graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Molt Tender")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tender = game.findPermanent("Molt Tender")!!
                val millAbilityId = cardRegistry.getCard("Molt Tender")!!
                    .activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tender, abilityId = millAbilityId)
                )
                withClue("Activating the mill ability should not error: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The library card is now in the graveyard") {
                    game.librarySize(1) shouldBe 0
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Paying {T} tapped Molt Tender") {
                    game.state.getEntity(tender)?.has<TappedComponent>() shouldBe true
                }
            }

            test("{T}, Exile a card from your graveyard: Add one mana of any color") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Molt Tender")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tender = game.findPermanent("Molt Tender")!!
                val manaAbilityId = cardRegistry.getCard("Molt Tender")!!
                    .activatedAbilities[1].id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tender, abilityId = manaAbilityId)
                )
                withClue("Activating the mana ability should not error: ${result.error}") {
                    result.error shouldBe null
                }

                // The exile cost may surface as a card-choice; with a single graveyard card there is
                // only one legal payment either way.
                (game.state.pendingDecision as? SelectCardsDecision)?.let { selection ->
                    game.selectCards(game.findCardsInGraveyard(1, "Grizzly Bears"))
                    withClue("The graveyard card was the only exile candidate") {
                        selection.options.size shouldBe 1
                    }
                }

                val colorDecision = game.state.pendingDecision as? ChooseColorDecision
                withClue("Adding one mana of any color pauses for the color choice") {
                    colorDecision shouldNotBe null
                }
                game.submitDecision(ColorChosenResponse(colorDecision!!.id, Color.BLUE))

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Exactly one mana of the chosen color, nothing else") {
                    pool.getAmount(Color.BLUE) shouldBe 1
                    pool.getAmount(Color.GREEN) shouldBe 0
                }
                withClue("The graveyard card was exiled as a cost") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
                withClue("Paying {T} tapped Molt Tender") {
                    game.state.getEntity(tender)?.has<TappedComponent>() shouldBe true
                }
            }

            test("the mana ability can't be activated with an empty graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Molt Tender")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tender = game.findPermanent("Molt Tender")!!
                val manaAbilityId = cardRegistry.getCard("Molt Tender")!!
                    .activatedAbilities[1].id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = tender, abilityId = manaAbilityId)
                )
                withClue("No card to exile, so the cost is unpayable") {
                    result.error shouldNotBe null
                }
                withClue("An illegal activation neither taps the creature nor adds mana") {
                    game.state.getEntity(tender)?.has<TappedComponent>() shouldBe false
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                        .getAmount(Color.GREEN) shouldBe 0
                }
            }
        }
    }
}
