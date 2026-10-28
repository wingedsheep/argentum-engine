package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jarad, Golgari Lich Lord — its three abilities, with the graveyard one carrying the weight: a
 * composite "Sacrifice a Swamp and a Forest" cost, paid by one disjoint assignment (a single Swamp
 * Forest can't pay both atoms — the 2012-10-01 ruling), activated from the graveyard.
 */
class JaradGolgariLichLordScenarioTest : ScenarioTestBase() {

    private val abilities
        get() = cardRegistry.getCard("Jarad, Golgari Lich Lord")!!.script.activatedAbilities
    private val drainAbility get() = abilities[0].id
    private val returnAbility get() = abilities[1].id

    private fun TestGame.returnOffered() =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.abilityId == returnAbility && it.isAffordable }

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Jarad, Golgari Lich Lord") {

            test("gets +1/+1 for each creature card in your graveyard") {
                val game = base()
                    .withCardOnBattlefield(1, "Jarad, Golgari Lich Lord")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .build()
                val jarad = game.findPermanent("Jarad, Golgari Lich Lord")!!
                withClue("two creature cards in *your* graveyard; the instant and the opponent's card don't count") {
                    game.state.projectedState.getPower(jarad) shouldBe 4
                    game.state.projectedState.getToughness(jarad) shouldBe 4
                }
            }

            test("sacrificing another creature drains each opponent for its power") {
                val game = base()
                    .withCardOnBattlefield(1, "Jarad, Golgari Lich Lord")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .build()
                val jarad = game.findPermanent("Jarad, Golgari Lich Lord")!!

                val result = game.execute(ActivateAbility(game.player1Id, jarad, drainAbility))
                withClue("activation is legal: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                withClue("Hill Giant is the only other creature, so it goes without a prompt") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
                withClue("Jarad itself was not a legal sacrifice") {
                    game.findPermanent("Jarad, Golgari Lich Lord") shouldNotBe null
                }
            }

            test("returns from the graveyard only for a Swamp *and* a Forest — one dual is not both") {
                val game = base()
                    .withCardInGraveyard(1, "Jarad, Golgari Lich Lord")
                    .withLandsOnBattlefield(1, "Overgrown Tomb", 1)
                    .build()
                withClue("Overgrown Tomb is a Swamp and a Forest, but it can pay only one of the two sacrifices") {
                    game.returnOffered() shouldBe false
                }
                val jarad = game.findCardsInGraveyard(1, "Jarad, Golgari Lich Lord").single()
                game.execute(ActivateAbility(game.player1Id, jarad, returnAbility)).error shouldNotBe null
                game.isOnBattlefield("Overgrown Tomb") shouldBe true
            }

            test("Overgrown Tomb and a Forest pay it: the Tomb takes the Swamp slot, and Jarad returns to hand") {
                val game = base()
                    .withCardInGraveyard(1, "Jarad, Golgari Lich Lord")
                    .withLandsOnBattlefield(1, "Overgrown Tomb", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .build()
                game.returnOffered() shouldBe true
                val jarad = game.findCardsInGraveyard(1, "Jarad, Golgari Lich Lord").single()

                game.execute(ActivateAbility(game.player1Id, jarad, returnAbility)).error shouldBe null
                withClue("the only assignment is forced, so nothing is asked") {
                    game.getPendingDecision() shouldBe null
                }
                game.isOnBattlefield("Overgrown Tomb") shouldBe false
                game.isOnBattlefield("Forest") shouldBe false
                game.resolveStack()

                game.isInHand(1, "Jarad, Golgari Lich Lord") shouldBe true
            }

            test("with a real choice, each land is asked for in turn and the needed dual is held back") {
                val game = base()
                    .withCardInGraveyard(1, "Jarad, Golgari Lich Lord")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Overgrown Tomb", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .build()
                val jarad = game.findCardsInGraveyard(1, "Jarad, Golgari Lich Lord").single()
                val swamp = game.findPermanent("Swamp")!!
                val tomb = game.findPermanent("Overgrown Tomb")!!
                val forest = game.findPermanent("Forest")!!

                game.execute(ActivateAbility(game.player1Id, jarad, returnAbility)).error shouldBe null
                val swampPick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("both Swamps leave a Forest behind, so both are offered") {
                    swampPick.options.toSet() shouldBe setOf(swamp, tomb)
                    swampPick.options shouldNotContain forest
                }
                game.selectCards(listOf(tomb)).error shouldBe null

                withClue("only the basic Forest is left for the Forest slot — forced") {
                    game.getPendingDecision() shouldBe null
                }
                game.isOnBattlefield("Swamp") shouldBe true
                game.isOnBattlefield("Overgrown Tomb") shouldBe false
                game.isOnBattlefield("Forest") shouldBe false
                game.resolveStack()
                game.isInHand(1, "Jarad, Golgari Lich Lord") shouldBe true
            }
        }
    }
}
