package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.sos.cards.CauldronOfEssence
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the SOS Lifegain & Graveyard batch:
 *  - Blech, Loafing Pest        — "Whenever you gain life, put a +1/+1 counter on each Pest, Bat,
 *                                  Insect, Snake, and Spider you control."
 *  - Tenured Concocter          — becomes-targeted-by-opponent "may draw"; Infusion +2/+0 conditional.
 *  - Colossus of the Blood Age  — ETB 3 damage to each opponent + gain 3 life; dies discard-any-then-draw+1.
 *  - Cauldron of Essence        — creature-you-control-dies drain; sorcery-speed sac-reanimate.
 *
 * Lifegain is driven by Sacred Nectar ("You gain 4 life"); deaths are driven through the engine
 * via Doom Blade ("Destroy target creature") so the real ZoneChange/LifeGain events fire the triggers.
 */
class SosLifegainGraveyardScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int {
        val id = findPermanent(name) ?: return 0
        return state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        // -----------------------------------------------------------------------------------------
        // Blech, Loafing Pest
        // -----------------------------------------------------------------------------------------
        context("Blech, Loafing Pest") {
            test("gaining life puts a +1/+1 counter on each matching tribe you control (incl. Blech)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blech, Loafing Pest", summoningSickness = false)
                    .withCardOnBattlefield(1, "Pincer Spider", summoningSickness = false) // a Spider
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)    // a Bear, no match
                    .withCardInHand(1, "Sacred Nectar")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sacred Nectar").error shouldBe null
                game.resolveStack()

                withClue("Blech is a Pest — gets a counter") { game.plusCounters("Blech, Loafing Pest") shouldBe 1 }
                withClue("Pincer Spider is a Spider — gets a counter") { game.plusCounters("Pincer Spider") shouldBe 1 }
                withClue("Grizzly Bears is not a matching tribe — no counter") { game.plusCounters("Grizzly Bears") shouldBe 0 }
            }

            test("does nothing when no life is gained") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blech, Loafing Pest", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.resolveStack()
                game.plusCounters("Blech, Loafing Pest") shouldBe 0
            }
        }

        // -----------------------------------------------------------------------------------------
        // Tenured Concocter
        // -----------------------------------------------------------------------------------------
        context("Tenured Concocter") {
            test("opponent targeting it with a spell lets you may-draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Player 2 controls the Concocter; Player 1 (active) targets it with removal.
                    .withCardOnBattlefield(2, "Tenured Concocter", summoningSickness = false)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .build()

                val concocter = game.findPermanent("Tenured Concocter")!!
                val libBefore = game.librarySize(2)

                game.castSpell(1, "Doom Blade", targetId = concocter).error shouldBe null
                game.resolveStack()

                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()

                withClue("Player 2 drew from the may-draw trigger") {
                    game.librarySize(2) shouldBe (libBefore - 1)
                }
            }

            test("Infusion grants +2/+0 only after you have gained life this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tenured Concocter", summoningSickness = false)
                    .withCardInHand(1, "Sacred Nectar")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val concocter = game.findPermanent("Tenured Concocter")!!
                withClue("base 4/5 before any life gain") {
                    game.state.projectedState.getPower(concocter) shouldBe 4
                    game.state.projectedState.getToughness(concocter) shouldBe 5
                }

                game.castSpell(1, "Sacred Nectar").error shouldBe null
                game.resolveStack()

                withClue("after gaining life this turn, Infusion grants +2/+0 → 6/5") {
                    game.state.projectedState.getPower(concocter) shouldBe 6
                    game.state.projectedState.getToughness(concocter) shouldBe 5
                }
            }
        }

        // -----------------------------------------------------------------------------------------
        // Colossus of the Blood Age
        // -----------------------------------------------------------------------------------------
        context("Colossus of the Blood Age") {
            test("ETB: deals 3 to each opponent and you gain 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Colossus of the Blood Age")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Colossus of the Blood Age").error shouldBe null
                game.resolveStack()

                withClue("each opponent took 3 damage") { game.getLifeTotal(2) shouldBe 17 }
                withClue("you gained 3 life") { game.getLifeTotal(1) shouldBe 23 }
            }

            test("dies: discard any number, then draw that many plus one (declining = draw one)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Colossus of the Blood Age", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears") // a hand card so the discard-any-number decision is raised
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val colossus = game.findPermanent("Colossus of the Blood Age")!!
                val handBefore = game.handSize(1)

                game.castSpell(2, "Doom Blade", targetId = colossus).error shouldBe null
                game.resolveStack()

                // Dies trigger: discard-any-number decision — choose zero cards.
                withClue("a discard-any-number decision should be pending") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("discarded 0 → drew 0 + 1 = 1 card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }

        // -----------------------------------------------------------------------------------------
        // Cauldron of Essence
        // -----------------------------------------------------------------------------------------
        context("Cauldron of Essence") {
            test("a creature you control dying drains each opponent 1 and you gain 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cauldron of Essence")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Doom Blade", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("each opponent lost 1 life") { game.getLifeTotal(2) shouldBe 19 }
                withClue("you gained 1 life") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("sorcery-speed: sacrifice a creature to reanimate a creature card from your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cauldron of Essence")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // sacrifice fodder
                    .withCardInGraveyard(1, "Savannah Lions")                              // reanimation target
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1) // generic {1}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cauldron = game.findPermanent("Cauldron of Essence")!!
                val fodder = game.findPermanent("Grizzly Bears")!!
                val reanimee = game.state.getGraveyard(game.player1Id)
                    .first { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Savannah Lions" }

                val abilityId = CauldronOfEssence.activatedAbilities.first().id
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cauldron,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(reanimee, game.player1Id, Zone.GRAVEYARD)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the reanimated Savannah Lions is on the battlefield") {
                    game.findPermanent("Savannah Lions") shouldBe reanimee
                }
                withClue("it is no longer in the graveyard") {
                    game.isInGraveyard(1, "Savannah Lions") shouldBe false
                }
                withClue("the sacrificed Grizzly Bears is gone from the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }
        }
    }
}
