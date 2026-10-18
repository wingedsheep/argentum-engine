package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Born to Drive (NEO #6) — {2}{W} Enchantment — Aura.
 *
 *   Enchant artifact or creature
 *   As long as enchanted permanent is a creature, it gets +1/+1 for each creature and/or Vehicle
 *   you control.
 *   Channel — {2}{W}, Discard this card: Create two 1/1 colorless Pilot creature tokens with
 *   "This token crews Vehicles as though its power were 2 greater."
 *
 * Sleek Schooner is a 4/3 Vehicle with crew 1.
 */
class BornToDriveScenarioTest : ScenarioTestBase() {

    private fun channelAbilityId() = cardRegistry.getCard("Born to Drive")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    init {
        context("Born to Drive") {

            test("counts each creature and Vehicle you control, the enchanted creature included") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(1, "Sleek Schooner")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Born to Drive", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Bears + Lions + uncrewed Schooner = 3; the opponent's Hill Giant doesn't count") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
            }

            test("a crewed Vehicle is counted once, and an enchanted Vehicle gets the bonus while crewed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sleek Schooner")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardAttachedTo(1, "Born to Drive", "Sleek Schooner")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val schooner = game.findPermanent("Sleek Schooner")!!
                val lions = game.findPermanent("Savannah Lions")!!
                game.state.projectedState.isCreature(schooner) shouldBe false

                game.execute(CrewVehicle(game.player1Id, schooner, listOf(lions))).error shouldBe null
                game.resolveStack()
                game.state.projectedState.isCreature(schooner) shouldBe true

                withClue("4/3 + Schooner (creature and Vehicle, once) + Lions = +2/+2") {
                    game.state.projectedState.getPower(schooner) shouldBe 6
                    game.state.projectedState.getToughness(schooner) shouldBe 5
                }
            }

            test("on an opponent's creature, it counts only what the Aura's controller controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Born to Drive", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("only Savannah Lions is a creature the Aura's controller controls") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("channel discards it to create two 1/1 Pilot tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Born to Drive")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handCard = game.findCardsInHand(1, "Born to Drive").first()
                game.execute(ActivateAbility(game.player1Id, handCard, channelAbilityId())).error shouldBe null
                game.resolveStack()

                val pilots = game.findPermanents("Pilot Token")
                pilots.size shouldBe 2
                pilots.all {
                    game.state.projectedState.getPower(it) == 1 && game.state.projectedState.getToughness(it) == 1
                } shouldBe true
                game.isInGraveyard(1, "Born to Drive") shouldBe true
            }
        }
    }
}
