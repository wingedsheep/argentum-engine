package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cleaver Skaab (MIC #11) — "{3}, {T}, Sacrifice another Zombie: Create two tokens that are copies
 * of the sacrificed creature."
 *
 * Pins the cost-sacrifice → copy path: the Zombie is gone by resolution, so both tokens are built
 * from the copiable values its cost-sacrifice snapshot froze (`SacrificedAsCost(0)`).
 */
class CleaverSkaabScenarioTest : ScenarioTestBase() {

    init {
        test("sacrificing another Zombie creates two token copies of it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Cleaver Skaab", summoningSickness = false)
                .withCardOnBattlefield(1, "Walking Corpse")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val skaab = game.findPermanent("Cleaver Skaab")!!
            val corpse = game.findPermanent("Walking Corpse")!!
            val abilityId = cardRegistry.getCard("Cleaver Skaab")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = skaab,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(corpse)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Walking Corpse") shouldBe true
            val copies = game.findAllPermanents("Walking Corpse")
            copies.size shouldBe 2
            val projected = game.state.projectedState
            copies.forEach { copy ->
                game.state.getEntity(copy)!!.has<TokenComponent>() shouldBe true
                projected.hasSubtype(copy, "Zombie") shouldBe true
                projected.getPower(copy) shouldBe 2
                projected.getToughness(copy) shouldBe 2
            }
        }

        test("Cleaver Skaab can't sacrifice itself to its own ability") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Cleaver Skaab", summoningSickness = false)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val skaab = game.findPermanent("Cleaver Skaab")!!
            val abilityId = cardRegistry.getCard("Cleaver Skaab")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = skaab,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(skaab)),
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Cleaver Skaab") shouldBe true
        }
    }
}
