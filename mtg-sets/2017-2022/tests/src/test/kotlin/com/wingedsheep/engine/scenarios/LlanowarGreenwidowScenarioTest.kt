package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Llanowar Greenwidow (DMU #169) — {2}{G} 4/3 Spider with reach and trample.
 *
 * "Domain — {7}{G}: Return this card from your graveyard to the battlefield tapped. It gains 'If
 * this permanent would leave the battlefield, exile it instead of putting it anywhere else.' This
 * ability costs {1} less to activate for each basic land type among lands you control."
 *
 * Pins the domain discount on a graveyard-activated ability (priced off the lands, not the card's
 * zone), the tapped return, and the granted exile-instead replacement.
 */
class LlanowarGreenwidowScenarioTest : ScenarioTestBase() {

    private fun TestGame.untappedLands(): Int = state.getBattlefield(player1Id).count { id ->
        val entity = state.getEntity(id)
        entity?.get<CardComponent>()?.typeLine?.isLand == true && !entity.has<TappedComponent>()
    }

    private fun abilityId() = cardRegistry.getCard("Llanowar Greenwidow")!!.activatedAbilities.first().id

    private fun ScenarioBuilder.domainFive(): ScenarioBuilder = this
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Mountain", 3)

    init {
        context("Llanowar Greenwidow") {

            test("with five basic land types it returns tapped for {2}{G}, and is exiled instead of dying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Llanowar Greenwidow")
                    .domainFive()
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val widow = game.state.getGraveyard(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Llanowar Greenwidow"
                }
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = widow, abilityId = abilityId())
                )
                withClue("{7}{G} less 5 for domain is {2}{G}: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("exactly three of the seven lands were tapped for {2}{G}") {
                    game.untappedLands() shouldBe 4
                }
                game.resolveStack()

                val returned = game.findPermanent("Llanowar Greenwidow")
                withClue("it is back on the battlefield, tapped") {
                    returned shouldNotBe null
                    game.state.getEntity(returned!!)?.has<TappedComponent>() shouldBe true
                }

                game.castSpell(1, "Lightning Bolt", returned).error shouldBe null
                game.resolveStack()

                withClue("3 damage kills the 4/3, but it goes to exile rather than the graveyard") {
                    game.isOnBattlefield("Llanowar Greenwidow") shouldBe false
                    game.isInGraveyard(1, "Llanowar Greenwidow") shouldBe false
                    game.isInExile(1, "Llanowar Greenwidow") shouldBe true
                }
            }

            test("one basic land type only discounts {1}, so four Forests can't pay") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Llanowar Greenwidow")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val widow = game.state.getGraveyard(game.player1Id).first()
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = widow, abilityId = abilityId())
                )
                withClue("domain 1 leaves {6}{G} — seven mana, not four") {
                    result.error shouldNotBe null
                }
                game.isInGraveyard(1, "Llanowar Greenwidow") shouldBe true
            }
        }
    }
}
