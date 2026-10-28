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

            test("sacrificing another creature drains each opponent for its last-known power") {
                val game = base()
                    .withCardOnBattlefield(1, "Jarad, Golgari Lich Lord")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()
                val jarad = game.findPermanent("Jarad, Golgari Lich Lord")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.projectedState.getPower(giant) shouldBe 6

                val result = game.execute(ActivateAbility(game.player1Id, jarad, drainAbility))
                withClue("activation is legal: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                withClue("Hill Giant is the only other creature, so it goes without a prompt") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                game.resolveStack()

                withClue("the pumped 6 power it last had on the battlefield, not its printed 3") {
                    game.getLifeTotal(2) shouldBe 14
                }
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

            test("with a real choice, the Swamp prompt holds back the dual the Forest slot needs") {
                val game = base()
                    .withCardInGraveyard(1, "Jarad, Golgari Lich Lord")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Overgrown Tomb", 1)
                    .build()
                val jarad = game.findCardsInGraveyard(1, "Jarad, Golgari Lich Lord").single()
                val swamps = game.findPermanents("Swamp")
                val tomb = game.findPermanent("Overgrown Tomb")!!

                game.execute(ActivateAbility(game.player1Id, jarad, returnAbility)).error shouldBe null
                val swampPick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Overgrown Tomb is a Swamp, but it is the only Forest, so only the basics are offered") {
                    swampPick.options.toSet() shouldBe swamps.toSet()
                    swampPick.options shouldNotContain tomb
                }
                game.selectCards(listOf(swamps.first())).error shouldBe null

                withClue("the Tomb is the only Forest left — sacrificed for the Forest slot without a prompt") {
                    game.getPendingDecision() shouldBe null
                }
                game.isOnBattlefield("Overgrown Tomb") shouldBe false
                game.findPermanents("Swamp") shouldBe listOf(swamps.last())
                game.resolveStack()
                game.isInHand(1, "Jarad, Golgari Lich Lord") shouldBe true
            }
        }
    }
}
