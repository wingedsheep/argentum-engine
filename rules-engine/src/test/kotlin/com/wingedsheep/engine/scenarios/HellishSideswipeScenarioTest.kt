package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hellish Sideswipe (DFT #90).
 *
 * "As an additional cost to cast this spell, sacrifice an artifact or creature.
 *  Destroy target creature or Vehicle. If the sacrificed permanent was a Vehicle, draw a card."
 *
 * This is the first card to use `SacrificedPermanentHadSubtype`, so these tests pin that condition
 * end to end: the rider reads the cost-time **snapshot** of the sacrificed permanent (CR 601.2h),
 * not the battlefield — by resolution the sacrificed permanent is already in the graveyard. Both
 * branches matter: sacrificing a Vehicle draws, sacrificing a non-Vehicle artifact or creature
 * doesn't.
 */
class HellishSideswipeScenarioTest : ScenarioTestBase() {

    private val testVehicle = card("Sideswipe Test Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.crew(1))
    }

    private val testArtifact = card("Sideswipe Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    init {
        cardRegistry.register(testVehicle)
        cardRegistry.register(testArtifact)

        context("Hellish Sideswipe") {

            test("sacrificing a Vehicle destroys the target and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hellish Sideswipe")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Sideswipe Test Vehicle")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Grizzly Bears")!!
                val sacrifice = game.findPermanent("Sideswipe Test Vehicle")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hellish Sideswipe"
                }
                val handBefore = game.handSize(1)

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(target)),
                        additionalCostPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(sacrifice)
                        )
                    )
                )
                withClue("Casting Hellish Sideswipe should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The sacrificed Vehicle is in the graveyard") {
                    game.isInGraveyard(1, "Sideswipe Test Vehicle") shouldBe true
                }
                withClue("The target creature is destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                // Hand: -1 for the Sideswipe leaving, +1 for the rider draw ⇒ net -1 + 1 = handBefore - 1 + 1.
                withClue("Sacrificing a Vehicle draws a card") {
                    game.handSize(1) shouldBe handBefore
                    game.isInHand(1, "Mountain") shouldBe true
                }
            }

            test("sacrificing a non-Vehicle artifact destroys the target but draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hellish Sideswipe")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Sideswipe Test Trinket")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Grizzly Bears")!!
                val sacrifice = game.findPermanent("Sideswipe Test Trinket")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hellish Sideswipe"
                }
                val handBefore = game.handSize(1)

                game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(target)),
                        additionalCostPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(sacrifice)
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("The target creature is still destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("No draw — the sacrificed permanent was not a Vehicle") {
                    game.handSize(1) shouldBe handBefore - 1
                    game.isInHand(1, "Mountain") shouldBe false
                }
            }

            test("can destroy an uncrewed Vehicle, which is not a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hellish Sideswipe")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sideswipe Test Vehicle")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Sideswipe Test Vehicle")!!
                val sacrifice = game.findPermanent("Grizzly Bears")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hellish Sideswipe"
                }

                game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(target)),
                        additionalCostPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(sacrifice)
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("An uncrewed Vehicle is a legal target and is destroyed") {
                    game.isInGraveyard(2, "Sideswipe Test Vehicle") shouldBe true
                }
                withClue("Sacrificing a creature draws nothing") {
                    game.isInHand(1, "Mountain") shouldBe false
                }
            }
        }
    }
}
