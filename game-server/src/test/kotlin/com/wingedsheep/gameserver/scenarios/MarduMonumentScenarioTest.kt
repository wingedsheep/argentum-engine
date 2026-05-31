package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Mardu Monument (TDM #245).
 *
 * "{2} Artifact — When this artifact enters, search your library for a basic Mountain, Plains,
 *  or Swamp card, reveal it, put it into your hand, then shuffle.
 *  {2}{R}{W}{B}, {T}, Sacrifice this artifact: Create three 1/1 red Warrior creature tokens.
 *  They gain menace and haste until end of turn. Activate only as a sorcery."
 */
class MarduMonumentScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Mardu Monument") {

            test("ETB searches for a basic Mountain/Plains/Swamp and puts it into hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mardu Monument")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Mardu Monument")
                withClue("Casting Mardu Monument should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // ETB search surfaces a selection; the only eligible basic is the Swamp.
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Only the basic Swamp should be a legal choice (Forest is excluded)") {
                    decision.options.mapNotNull {
                        game.state.getEntity(it)?.get<CardComponent>()?.name
                    }.toSet() shouldBe setOf("Swamp")
                }
                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()

                withClue("The searched Swamp should be in hand") {
                    game.state.getHand(game.player1Id).count { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Swamp"
                    } shouldBe 1
                }
            }

            test("sacrifice ability makes three Warrior tokens with menace and haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mardu Monument")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monumentId = game.findPermanent("Mardu Monument")!!
                val cardDef = cardRegistry.getCard("Mardu Monument")!!
                val tokenAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monumentId,
                        abilityId = tokenAbility.id
                    )
                )
                withClue("Activating the token ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Mardu Monument should be sacrificed") {
                    game.isOnBattlefield("Mardu Monument") shouldBe false
                }

                val tokens = game.findPermanents("Warrior Token")
                withClue("Three 1/1 red Warrior tokens should exist") {
                    tokens.size shouldBe 3
                }

                val projected = stateProjector.project(game.state)
                tokens.forEach { id ->
                    withClue("Each token should be 1/1") {
                        projected.getPower(id) shouldBe 1
                        projected.getToughness(id) shouldBe 1
                    }
                    withClue("Each token should have menace until end of turn") {
                        projected.hasKeyword(id, Keyword.MENACE) shouldBe true
                    }
                    withClue("Each token should have haste until end of turn") {
                        projected.hasKeyword(id, Keyword.HASTE) shouldBe true
                    }
                }
            }
        }
    }
}
