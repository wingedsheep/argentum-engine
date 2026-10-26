package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nature's Embrace (VOW #211) — {2}{G} Enchantment — Aura
 *
 *   Enchant creature or land
 *   As long as enchanted permanent is a creature, it gets +2/+2.
 *   As long as enchanted permanent is a land, it has "{T}: Add two mana of any one color."
 *
 * Dual-mode Aura: on a creature host only the +2/+2 static switches on; on a land host only the
 * granted "{T}: Add two mana of any one color" ability switches on. Each grant is gated by the
 * host's current type.
 */
class NaturesEmbraceScenarioTest : ScenarioTestBase() {

    init {
        context("Nature's Embrace — dual-mode Aura keyed off the host's type") {

            test("on a creature host, the enchanted creature gets +2/+2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardAttachedTo(1, "Nature's Embrace", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("base 2/2 + 2/2 = 4/4") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }

            test("on a land host, the enchanted land gains a second tap-for-mana ability") {
                val bare = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bareForest = bare.findPermanent("Forest")!!
                val before = bare.getLegalActions(1)
                    .count { (it.action as? ActivateAbility)?.sourceId == bareForest }

                val enchanted = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardAttachedTo(1, "Nature's Embrace", "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enchantedForest = enchanted.findPermanent("Forest")!!
                val after = enchanted.getLegalActions(1)
                    .count { (it.action as? ActivateAbility)?.sourceId == enchantedForest }

                withClue("Nature's Embrace adds an activation the bare Forest doesn't have") {
                    after shouldBe before + 1
                }
            }

            test("the auto-payer uses the enchanted land's granted ability") {
                // Two Forests can't pay {2}{U} on their own; the enchanted one's "{T}: Add two mana
                // of any one color" (UU) plus the other Forest's {G} can.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardAttachedTo(1, "Nature's Embrace", "Forest")
                    .withCardInHand(1, "Divination")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()
                withClue("Divination resolved, paid through the granted ability") {
                    game.isInGraveyard(1, "Divination") shouldBe true
                }
            }
        }
    }
}
