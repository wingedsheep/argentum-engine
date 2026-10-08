package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Transmogrant Altar (BRO #124) — {B}, {T}, Sacrifice a creature: Add {C}{C}{C}.
 * {2}, {T}, Sacrifice a creature: Create a 3/3 colorless Zombie artifact creature token.
 * Activate only as a sorcery.
 */
class TransmograntAltarScenarioTest : ScenarioTestBase() {

    init {
        test("the mana ability sacrifices a creature for three colorless mana") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Transmogrant Altar")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val altar = game.findPermanent("Transmogrant Altar")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Transmogrant Altar")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = altar,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            ).error shouldBe null

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 3
        }

        test("the token ability sacrifices a creature for a 3/3 Zombie artifact creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Transmogrant Altar")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val altar = game.findPermanent("Transmogrant Altar")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Transmogrant Altar")!!.script.activatedAbilities[1].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = altar,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            val zombie = game.findPermanent("Zombie Token")
            zombie shouldNotBe null
            val projected = game.state.projectedState
            projected.hasType(zombie!!, "ARTIFACT") shouldBe true
            projected.isCreature(zombie) shouldBe true
            projected.hasSubtype(zombie, "Zombie") shouldBe true
            projected.getPower(zombie) shouldBe 3
            projected.getToughness(zombie) shouldBe 3
        }
    }
}
