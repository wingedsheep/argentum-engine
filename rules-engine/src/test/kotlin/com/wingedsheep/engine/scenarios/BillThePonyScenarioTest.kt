package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bill the Pony — Gap 27 (assign combat damage by toughness).
 *
 * "When Bill the Pony enters, create two Food tokens.
 *  Sacrifice a Food: Until end of turn, target creature you control assigns combat damage equal to
 *  its toughness rather than its power."
 *
 * The combat clause is granted as the [com.wingedsheep.sdk.core.AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS]
 * floating flag; [com.wingedsheep.engine.mechanics.combat.CombatDamageUtils] returns toughness when the
 * attacker carries it. Bill is a 1/4, so the grant turns its 1 combat damage into 4.
 */
class BillThePonyScenarioTest : ScenarioTestBase() {

    private val billAbilityId by lazy {
        cardRegistry.requireCard("Bill the Pony").activatedAbilities[0].id
    }

    init {
        test("enters: creates two Food tokens") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Bill the Pony")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bill the Pony").error shouldBe null
            game.resolveStack()

            game.findPermanents("Food").size shouldBe 2
        }

        test("sacrifice a Food: target creature assigns combat damage equal to toughness") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bill the Pony")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bill = game.findPermanent("Bill the Pony")!!
            val food = game.findPermanent("Food")!!

            // Sacrifice the Food, granting Bill (1/4) the toughness-assigns flag.
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = bill,
                    abilityId = billAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bill)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(food))
                )
            )
            withClue("Activating Bill's ability should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Food") shouldBe false

            // Attack player 2 with the unblocked 1/4. It assigns toughness (4), not power (1).
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Bill the Pony" to 2))
            // No blockers; passUntilPhase auto-resolves the combat damage step.
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Bill assigns combat damage equal to toughness (4), not power (1)") {
                game.getLifeTotal(2) shouldBe 16
            }
        }

        test("without the grant: Bill assigns combat damage equal to power") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bill the Pony")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Bill the Pony" to 2))
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Ungranted Bill deals its power (1)") {
                game.getLifeTotal(2) shouldBe 19
            }
        }
    }
}
