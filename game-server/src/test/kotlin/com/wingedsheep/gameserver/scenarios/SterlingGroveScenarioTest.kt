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

/**
 * Scenario tests for Sterling Grove.
 *
 * Sterling Grove: {G}{W}
 * Enchantment
 * Other enchantments you control have shroud.
 * {1}, Sacrifice this enchantment: Search your library for an enchantment card, reveal it,
 *   then shuffle and put that card on top.
 */
class SterlingGroveScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Sterling Grove") {

            test("grants shroud to other enchantments you control but not to itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sterling Grove")
                    .withCardOnBattlefield(1, "Collective Restraint")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sterlingGrove = game.findPermanent("Sterling Grove")!!
                val otherEnchantment = game.findPermanent("Collective Restraint")!!
                val projected = stateProjector.project(game.state)

                withClue("the other enchantment you control should have shroud") {
                    projected.hasKeyword(otherEnchantment, Keyword.SHROUD) shouldBe true
                }
                withClue("Sterling Grove itself should NOT have shroud (\"other\" excludes the source)") {
                    projected.hasKeyword(sterlingGrove, Keyword.SHROUD) shouldBe false
                }
            }

            test("activated ability searches for an enchantment and puts it on top after shuffling") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sterling Grove")
                    .withLandsOnBattlefield(1, "Forest", 1) // {1} for the activation cost
                    .withCardInLibrary(1, "Collective Restraint") // the only enchantment in the library
                    .withCardInLibrary(1, "Jungle Lion")          // non-enchantment — must not be offered
                    .withCardInLibrary(1, "Forest")               // non-enchantment — must not be offered
                    .withCardInLibrary(2, "Forest")               // opponent needs a library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLibrarySize = game.state.getLibrary(game.player1Id).size

                val sterlingGrove = game.findPermanent("Sterling Grove")!!
                val ability = cardRegistry.getCard("Sterling Grove")!!.script.activatedAbilities.first()

                val activate = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sterlingGrove,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )
                withClue("activation should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                withClue("Sterling Grove should be sacrificed as part of the cost") {
                    game.isOnBattlefield("Sterling Grove") shouldBe false
                    game.isInGraveyard(1, "Sterling Grove") shouldBe true
                }

                game.resolveStack()

                withClue("the search should ask which enchantment to find") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                val offeredNames = decision.options.mapNotNull { decision.cardInfo?.get(it)?.name }.toSet()
                withClue("only the enchantment card may be offered (Enchantment filter), not the creature/land") {
                    offeredNames shouldBe setOf("Collective Restraint")
                }

                val enchantmentId = decision.options.first { decision.cardInfo?.get(it)?.name == "Collective Restraint" }
                game.selectCards(listOf(enchantmentId))

                withClue("library size is unchanged — the card moved within the library, not out of it") {
                    game.state.getLibrary(game.player1Id).size shouldBe initialLibrarySize
                }
                val topCard = game.state.getLibrary(game.player1Id).firstOrNull()
                    ?.let { game.state.getEntity(it) }?.get<CardComponent>()
                withClue("the found enchantment should end up on top of the library after the shuffle") {
                    topCard?.name shouldBe "Collective Restraint"
                }
            }
        }
    }
}
