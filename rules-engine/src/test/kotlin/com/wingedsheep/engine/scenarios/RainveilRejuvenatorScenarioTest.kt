package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Rainveil Rejuvenator. */
class RainveilRejuvenatorScenarioTest : ScenarioTestBase() {

    private val rainveilManaAbilityId =
        cardRegistry.getCard("Rainveil Rejuvenator")!!.activatedAbilities.first().id
    private val unrootedAbilityId =
        cardRegistry.getCard("Unrooted Ancestor")!!.activatedAbilities.first().id

    init {
        context("Rainveil Rejuvenator") {
            test("ETB may mill three cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rainveil Rejuvenator")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Shock")
                    .withCardInLibrary(1, "Glory Seeker")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Rainveil Rejuvenator")
                withClue("Casting Rainveil Rejuvenator should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("ETB should present a 'may mill' yes/no decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val libBefore = game.librarySize(1)
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Milling three should move three cards from library to graveyard") {
                    game.librarySize(1) shouldBe libBefore - 3
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 1
                }
            }

            test("tap adds {G} equal to power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    // Place it directly so it has no summoning sickness; its {T} is a mana ability.
                    .withCardOnBattlefield(1, "Rainveil Rejuvenator")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rejuvenator = game.findPermanent("Rainveil Rejuvenator")!!
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rejuvenator,
                        abilityId = rainveilManaAbilityId,
                    )
                )
                withClue("Activating the mana ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }

                withClue("Rainveil Rejuvenator should be tapped after the mana ability") {
                    game.state.getEntity(rejuvenator)?.has<TappedComponent>() shouldBe true
                }
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Mana pool should hold 2 green (equal to power)") {
                    (pool?.green ?: 0) shouldBe 2
                }
            }

            test("ETB may mill three can be declined") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rainveil Rejuvenator")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Rainveil Rejuvenator")
                game.resolveStack()

                val libBefore = game.librarySize(1)
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Declining the mill leaves the library untouched") {
                    game.librarySize(1) shouldBe libBefore
                }
            }
        }
    }
}
