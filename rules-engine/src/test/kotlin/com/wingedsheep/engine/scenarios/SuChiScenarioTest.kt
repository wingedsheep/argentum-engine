package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the ATQ artifact-creature dies triggers.
 *
 * Su-Chi (ATQ #66) — {4} Artifact Creature — Construct 4/4
 *   "When this creature dies, add {C}{C}{C}{C}."
 *
 * Onulet (ATQ #59) — {3} Artifact Creature — Construct 2/2
 *   "When this creature dies, you gain 2 life."
 */
class SuChiScenarioTest : ScenarioTestBase() {

    // {0} sorcery that destroys a target creature, to send a chosen body to the graveyard.
    private val slayCreature = card("Slay Creature") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.Destroy(t)
        }
    }

    init {
        cardRegistry.register(slayCreature)

        context("ATQ dies triggers") {

            test("Su-Chi dies and adds four colorless mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Su-Chi", summoningSickness = false)
                    .withCardInHand(1, "Slay Creature")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val suChi = game.findPermanent("Su-Chi")!!
                val colorlessBefore =
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.colorless ?: 0

                game.castSpell(1, "Slay Creature", suChi).error shouldBe null
                game.resolveStack()

                withClue("Su-Chi should be in the graveyard") {
                    game.isOnBattlefield("Su-Chi") shouldBe false
                    game.isInGraveyard(1, "Su-Chi") shouldBe true
                }
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Four colorless mana should have been added to the controller's pool") {
                    (pool?.colorless ?: 0) shouldBe colorlessBefore + 4
                }
            }

            test("Onulet dies and its controller gains 2 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Onulet", summoningSickness = false)
                    .withCardInHand(1, "Slay Creature")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val onulet = game.findPermanent("Onulet")!!
                game.castSpell(1, "Slay Creature", onulet).error shouldBe null
                game.resolveStack()

                withClue("Onulet should be in the graveyard") {
                    game.isOnBattlefield("Onulet") shouldBe false
                    game.isInGraveyard(1, "Onulet") shouldBe true
                }
                withClue("Onulet's controller gains 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }
    }
}
