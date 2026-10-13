package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe

/**
 * Sheoldred's Restoration — Dominaria United #108, {3}{B} Sorcery, Kicker {2}{W}
 *
 * "Return target creature card from your graveyard to the battlefield. If this spell was kicked,
 *  you gain life equal to that card's mana value. Otherwise, you lose that much life.
 *  Exile Sheoldred's Restoration."
 *
 * Hill Giant ({3}{R}) is mana value 4: unkicked costs 4 life, kicked gains 4. Either way the spell
 * exiles itself on resolution.
 */
class SheoldredsRestorationScenarioTest : ScenarioTestBase() {

    init {
        context("Sheoldred's Restoration") {

            fun cast(kicked: Boolean): TestGame {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Sheoldred's Restoration")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Plains", if (kicked) 3 else 0)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val spell = game.findCardsInHand(1, "Sheoldred's Restoration").single()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)),
                        declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                        paymentStrategy = PaymentStrategy.AutoPay,
                    )
                ).error shouldBe null
                game.resolveStack()
                return game
            }

            test("unkicked, it reanimates and you lose life equal to the card's mana value") {
                val game = cast(kicked = false)

                game.isOnBattlefield("Hill Giant") shouldBe true
                game.getLifeTotal(1) shouldBe 16
                game.isInExile(1, "Sheoldred's Restoration") shouldBe true
                game.isInGraveyard(1, "Sheoldred's Restoration") shouldBe false
            }

            test("kicked, it reanimates and you gain that much life instead") {
                val game = cast(kicked = true)

                game.isOnBattlefield("Hill Giant") shouldBe true
                game.getLifeTotal(1) shouldBe 24
                game.isInExile(1, "Sheoldred's Restoration") shouldBe true
            }
        }
    }
}
