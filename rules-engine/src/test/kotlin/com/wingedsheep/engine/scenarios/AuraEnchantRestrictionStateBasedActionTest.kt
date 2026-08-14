package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * CR 303.4c/704.5m: an Aura's "enchant" restriction is checked continuously, not only when the
 * Aura spell resolves. Once the permanent it's attached to stops matching that restriction, the
 * Aura is "attached to an illegal object" and is put into its owner's graveyard as a state-based
 * action.
 *
 * For "Enchant creature you control", the Cartouche of Solidarity ruling (2017-04-18) states the
 * consequence outright: "If another player gains control of either the Cartouche or the enchanted
 * creature (but not both), then the Cartouche will be enchanting an illegal permanent and be put
 * into its owner's graveyard as a state-based action." It is symmetric, and note the "but not both"
 * — what matters is whether the Aura's controller controls the host, not whether control changed.
 * Both directions are covered below: Act of Treason steals the host, Blatant Thievery steals the
 * Aura. CR 303.4e is why the two come apart at all — changing control of an enchanted object
 * doesn't change control of its Aura, or vice versa.
 *
 * The last test covers the type-change route instead, where the host stops being a creature at all.
 * Every test also pins the negative half, because an over-eager check would be just as wrong: an
 * unrestricted "Enchant creature" Aura survives the theft, and an Aura that lists the type it turns
 * its own host into survives doing so.
 */
class AuraEnchantRestrictionStateBasedActionTest : ScenarioTestBase() {

    init {
        context("an aura whose enchant restriction stops being met falls off") {
            test("stealing the host sends an 'enchant creature you control' aura to the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Cradle of Safety", "Grizzly Bears")  // enchant creature you control
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")     // enchant creature
                    .withCardInHand(2, "Act of Treason")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpell(2, "Act of Treason", bears)
                withClue("Player2 should be able to steal the enchanted creature: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Act of Treason gave Player2 control of the host") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }
                withClue("Cradle of Safety enchants 'a creature you control' — its host is now an opponent's, so it falls off") {
                    game.isOnBattlefield("Cradle of Safety") shouldBe false
                    game.isInGraveyard(1, "Cradle of Safety") shouldBe true
                }
                withClue("Holy Strength just enchants 'creature' — a stolen creature is still a legal host") {
                    game.isOnBattlefield("Holy Strength") shouldBe true
                }
                withClue("The host itself is unharmed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("stealing the aura instead of the host breaks the restriction just as well") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Cradle of Safety", "Grizzly Bears")  // enchant creature you control
                    .withCardInHand(2, "Blatant Thievery")
                    .withLandsOnBattlefield(2, "Island", 7)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Cradle of Safety")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpell(2, "Blatant Thievery", aura)
                withClue("Player2 should be able to steal the Aura itself: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The host stayed with Player1 — CR 303.4e keeps the two control changes separate") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }
                withClue("Player2 now controls an 'enchant creature you control' Aura on Player1's creature, so it falls off") {
                    game.isOnBattlefield("Cradle of Safety") shouldBe false
                }
                withClue("704.5m sends it to its OWNER's graveyard (Player1), not its new controller's") {
                    game.isInGraveyard(1, "Cradle of Safety") shouldBe true
                    game.isInGraveyard(2, "Cradle of Safety") shouldBe false
                }
                withClue("The host itself is unharmed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("turning the host into a land sends an 'enchant creature' aura to the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")  // enchant creature
                    .withCardInHand(2, "Imprisoned in the Moon")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpell(2, "Imprisoned in the Moon", bears)
                withClue("Imprisoned in the Moon can enchant a creature: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The host is now a land, not a creature") {
                    game.state.projectedState.isCreature(bears) shouldBe false
                    game.state.projectedState.hasType(bears, "LAND") shouldBe true
                }
                withClue("Holy Strength enchants a creature — its host is a land now, so it falls off") {
                    game.isOnBattlefield("Holy Strength") shouldBe false
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                }
                withClue("Imprisoned in the Moon enchants a creature, land, or planeswalker — a land host is still legal") {
                    game.isOnBattlefield("Imprisoned in the Moon") shouldBe true
                }
                withClue("The host itself is unharmed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
