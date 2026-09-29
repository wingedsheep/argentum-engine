package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Grafted Butcher (MOM #109) — {1}{B} Creature — Phyrexian Samurai 2/2
 *
 * When this creature enters, Phyrexians you control gain menace until end of turn.
 * Other Phyrexians you control get +1/+1.
 * {3}{B}, Sacrifice an artifact or creature: Return this card from your graveyard to the
 * battlefield. Activate only as a sorcery.
 */
class GraftedButcherScenarioTest : ScenarioTestBase() {

    init {
        context("Grafted Butcher") {

            test("ETB grants menace to Phyrexians only; lord pumps other Phyrexians only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Grafted Butcher")
                    .withCardOnBattlefield(1, "Dreg Recycler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grafted Butcher").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                val butcher = game.findPermanent("Grafted Butcher")!!
                val recycler = game.findPermanent("Dreg Recycler")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Phyrexians (including the Butcher) gain menace") {
                    projected.hasKeyword(butcher, Keyword.MENACE) shouldBe true
                    projected.hasKeyword(recycler, Keyword.MENACE) shouldBe true
                }
                withClue("A non-Phyrexian doesn't") {
                    projected.hasKeyword(bears, Keyword.MENACE) shouldBe false
                }
                withClue("Other Phyrexians get +1/+1; the Butcher and non-Phyrexians don't") {
                    projected.getPower(recycler) shouldBe 3
                    projected.getToughness(recycler) shouldBe 3
                    projected.getPower(butcher) shouldBe 2
                    projected.getToughness(butcher) shouldBe 2
                    projected.getPower(bears) shouldBe 2
                }
            }

            test("{3}{B}, sacrifice a creature returns it from the graveyard to the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Grafted Butcher")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val butcher = game.findCardsInGraveyard(1, "Grafted Butcher").single()
                val fodder = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Grafted Butcher")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = butcher,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                withClue("The graveyard ability should activate: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The sacrificed creature is gone") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("The Butcher returned to the battlefield") {
                    game.isOnBattlefield("Grafted Butcher") shouldBe true
                    game.isInGraveyard(1, "Grafted Butcher") shouldBe false
                }
            }

            test("the graveyard ability can't be activated at instant speed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Grafted Butcher")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val butcher = game.findCardsInGraveyard(1, "Grafted Butcher").single()
                val fodder = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Grafted Butcher")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = butcher,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                result.error shouldNotBe null
                game.isInGraveyard(1, "Grafted Butcher") shouldBe true
            }
        }
    }
}
