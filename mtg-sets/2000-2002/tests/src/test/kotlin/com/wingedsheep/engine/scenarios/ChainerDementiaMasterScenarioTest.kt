package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Chainer, Dementia Master (TOR #56, {3}{B}{B}, Legendary Creature — Human Minion 3/3).
 *
 *   All Nightmares get +1/+1.
 *   {B}{B}{B}, Pay 3 life: Put target creature card from a graveyard onto the battlefield under
 *   your control. That creature is black and is a Nightmare in addition to its other creature types.
 *   When Chainer leaves the battlefield, exile all Nightmares.
 *
 * The reanimation reaches into *any* graveyard, so the test steals from the opponent's. The color
 * rewrite replaces (black only, per the 2022-12-08 ruling) while the Nightmare type is additive.
 * The leaves trigger reads projected subtypes, so it exiles the granted Nightmare as well as a
 * printed one on the other side of the table.
 */
class ChainerDementiaMasterScenarioTest : ScenarioTestBase() {

    private fun TestGame.reanimateOpponentsBears() {
        val chainer = findPermanent("Chainer, Dementia Master")!!
        val bears = findCardsInGraveyard(2, "Grizzly Bears").single()
        val ability = cardRegistry.getCard("Chainer, Dementia Master")!!.script.activatedAbilities.single()
        withClue("activating Chainer's {B}{B}{B}, Pay 3 life ability succeeds") {
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = chainer,
                    abilityId = ability.id,
                    targets = listOf(ChosenTarget.Card(bears, player2Id, Zone.GRAVEYARD)),
                )
            ).error shouldBe null
        }
        resolveStack()
    }

    init {
        context("Chainer, Dementia Master") {

            test("reanimates an opponent's creature as a black-only Nightmare under your control, pumped by Chainer") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Chainer, Dementia Master")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.reanimateOpponentsBears()

                withClue("3 life was paid") { game.getLifeTotal(1) shouldBe 17 }
                withClue("the Bears left Bob's graveyard") { game.isInGraveyard(2, "Grizzly Bears") shouldBe false }

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                withClue("Alice controls the reanimated creature") {
                    projected.getController(bears) shouldBe game.player1Id
                }
                withClue("it is black only — no longer green") {
                    projected.getColors(bears) shouldBe setOf("BLACK")
                }
                withClue("it is a Nightmare in addition to being a Bear") {
                    projected.getSubtypes(bears) shouldContainAll setOf("Nightmare", "Bear")
                }
                withClue("Chainer's lord gives the new Nightmare +1/+1") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
                withClue("Chainer is not a Nightmare, so stays 3/3") {
                    val chainer = game.findPermanent("Chainer, Dementia Master")!!
                    projected.getPower(chainer) shouldBe 3
                    projected.getToughness(chainer) shouldBe 3
                }
            }

            test("all Nightmares get +1/+1, including an opponent's printed Nightmare") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Chainer, Dementia Master")
                    .withCardOnBattlefield(2, "Ashiok's Skulker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skulker = game.findPermanent("Ashiok's Skulker")!!
                game.state.projectedState.getPower(skulker) shouldBe 4
                game.state.projectedState.getToughness(skulker) shouldBe 6
            }

            test("when Chainer leaves the battlefield, all Nightmares are exiled") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Chainer, Dementia Master")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ashiok's Skulker")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.reanimateOpponentsBears()
                withClue("the reanimated Bears are on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                val chainer = game.findPermanent("Chainer, Dementia Master")!!
                withClue("Lightning Bolt on Chainer is cast") {
                    game.castSpell(1, "Lightning Bolt", chainer).error shouldBe null
                }
                game.resolveStack()

                withClue("Chainer died") { game.isInGraveyard(1, "Chainer, Dementia Master") shouldBe true }
                withClue("the granted Nightmare is exiled to its owner's exile") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("the printed Nightmare is exiled too") {
                    game.isOnBattlefield("Ashiok's Skulker") shouldBe false
                    game.isInExile(2, "Ashiok's Skulker") shouldBe true
                }
                withClue("a non-Nightmare is untouched") { game.isOnBattlefield("Savannah Lions") shouldBe true }
            }
        }
    }
}
