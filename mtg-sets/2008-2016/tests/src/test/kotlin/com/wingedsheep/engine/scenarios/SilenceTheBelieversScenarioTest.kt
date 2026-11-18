package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Silence the Believers (JOU #82) — {2}{B}{B} Instant.
 *
 * "Strive — This spell costs {2}{B} more to cast for each target beyond the first.
 *  Exile any number of target creatures and all Auras attached to them."
 *
 * Pins the strive tax ({4}{B}{B}{B} for two targets) and that an Aura on a target goes to exile
 * with its host — to its *owner's* exile — rather than to the graveyard as a state-based action.
 */
class SilenceTheBelieversScenarioTest : ScenarioTestBase() {

    private fun TestGame.castSilence(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Silence the Believers"
            },
            targets = targets.map { ChosenTarget.Permanent(it) },
        )
    )

    init {
        context("Silence the Believers") {

            test("two targets for {4}{B}{B}{B} exile both creatures and the Aura on one of them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Silence the Believers")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    // Player 1's Aura on player 2's creature: it must land in player 1's exile.
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSilence(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost {4}{B}{B}{B}, payable with seven Swamps: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("both targeted creatures are exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Savannah Lions") shouldBe true
                }
                withClue("the Aura is exiled with its host, into its owner's exile") {
                    game.isInExile(1, "Holy Strength") shouldBe true
                    game.isInGraveyard(1, "Holy Strength") shouldBe false
                    game.isOnBattlefield("Holy Strength") shouldBe false
                }
            }

            test("a second target is rejected when only six mana is available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Silence the Believers")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSilence(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost seven mana; six Swamps can't pay it") { cast.error.shouldNotBeNull() }
                withClue("a rejected cast leaves the spell in hand") { game.handSize(1) shouldBe 1 }
            }

            test("one target costs the printed {2}{B}{B} and leaves the other creature alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Silence the Believers")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSilence(listOf(game.findPermanent("Grizzly Bears")!!))
                withClue("a single target owes nothing extra: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Savannah Lions") shouldBe true
            }
        }
    }
}
