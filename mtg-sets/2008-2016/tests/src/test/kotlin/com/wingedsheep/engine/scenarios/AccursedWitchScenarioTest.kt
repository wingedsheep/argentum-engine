package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Accursed Witch // Infectious Curse (SOI #97).
 *
 *   Front — Spells your opponents cast that target this creature cost {1} less to cast.
 *           When this creature dies, return it to the battlefield transformed under your control
 *           attached to target opponent.
 *   Back  — Enchant player. Spells you cast that target enchanted player cost {1} less to cast.
 *           At the beginning of enchanted player's upkeep, that player loses 1 life and you gain
 *           1 life.
 *
 * Both discounts are source-relative target checks, so the tests that matter are the ones that
 * separate the source from its neighbours: a different creature, the Witch's own controller, a
 * player the Curse doesn't enchant. The Curse's discount is also the corpus's first
 * `FixedIfAnyTargetMatches` matched against a *player* target. Searing Spear ({1}{R}, 3 damage to
 * any target) with a single Mountain is castable only at the discount.
 */
class AccursedWitchScenarioTest : ScenarioTestBase() {

    private fun TestGame.attachedTo(id: EntityId): EntityId? =
        state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    private fun TestGame.nameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        context("Accursed Witch") {

            /** The Witch and a Grizzly Bears under player 1; player 2 active with a Spear and [mountains]. */
            fun board(caster: Int = 2, mountains: Int = 1) = scenario()
                .withPlayers("Witch", "Opponent")
                .withCardOnBattlefield(1, "Accursed Witch")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(caster, "Searing Spear")
                .withLandsOnBattlefield(caster, "Mountain", mountains)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(caster)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("an opponent's spell targeting it costs {1} less; when it dies it returns as a curse on the opponent") {
                val game = board()
                val witch = game.findPermanent("Accursed Witch")!!

                withClue("one Mountain pays Searing Spear's {1}{R} less {1}") {
                    game.castSpell(2, "Searing Spear", witch).error shouldBe null
                }
                game.payIfAsked()
                game.resolveStack()
                if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("the same card is back, showing its other face") {
                    game.nameOf(witch) shouldBe "Infectious Curse"
                    game.isOnBattlefield("Infectious Curse") shouldBe true
                }
                withClue("attached to the targeted opponent") {
                    game.attachedTo(witch) shouldBe game.player2Id
                }
                withClue("'under your control' — the curse is the Witch's controller's") {
                    game.state.getEntity(witch)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
            }

            test("an opponent's spell targeting another creature pays full price") {
                val game = board()
                game.castSpell(2, "Searing Spear", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }

            test("its own controller's spell targeting it pays full price") {
                val game = board(caster = 1)
                game.castSpell(1, "Searing Spear", game.findPermanent("Accursed Witch")!!).error shouldNotBe null
            }
        }

        context("Infectious Curse") {

            /** Infectious Curse under player 1, attached to player 2. */
            fun cursed(activePlayer: Int = 1, phase: Phase = Phase.PRECOMBAT_MAIN, step: Step = Step.PRECOMBAT_MAIN): TestGame {
                val game = scenario()
                    .withPlayers("Curser", "Cursed")
                    .withCardOnBattlefield(1, "Infectious Curse")
                    .withCardInHand(1, "Searing Spear")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(activePlayer)
                    .inPhase(phase, step)
                    .build()
                val curse = game.findPermanent("Infectious Curse")!!
                game.state = game.state.updateEntity(curse) { it.with(AttachedToComponent(game.player2Id)) }
                return game
            }

            test("a spell its controller casts targeting the enchanted player costs {1} less") {
                val game = cursed()
                game.castSpellTargetingPlayer(1, "Searing Spear", 2).error shouldBe null
                game.payIfAsked()
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }

            test("a spell targeting a player it doesn't enchant pays full price") {
                val game = cursed()
                game.castSpellTargetingPlayer(1, "Searing Spear", 1).error shouldNotBe null
            }

            test("at the beginning of the enchanted player's upkeep, they lose 1 life and its controller gains 1") {
                val game = cursed(phase = Phase.ENDING, step = Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 21
            }
        }
    }
}
