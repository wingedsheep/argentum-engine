package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.TergridGodOfFright
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tergrid, God of Fright (KHM #112) — "Whenever an opponent sacrifices a nontoken permanent or
 * discards a permanent card, you may put that card from a graveyard onto the battlefield under
 * your control."
 *
 * Covers the two triggers binding the card in the graveyard as the triggering entity and moving
 * it under Tergrid's controller: the sacrifice half (nontoken only, opponent only) and the discard
 * half (permanent cards only), plus declining the "may".
 */
class TergridGodOfFrightScenarioTest : ScenarioTestBase() {

    private val cullOne = card("Tergrid Test Cull") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.You))
        }
    }

    private val cullLand = card("Tergrid Test Land Cull") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(GameObjectFilter.Land, 1, EffectTarget.PlayerRef(Player.You))
        }
    }

    private val mindRot = card("Tergrid Test Mind Rot") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.EachOpponentDiscards(1)
        }
    }

    private val shockCard = card("Tergrid Test Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }

    private val p1 = EntityId.of("player-1")

    private fun TestGame.resolveAll(acceptMay: Boolean = true): Int {
        var mayPrompts = 0
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 60) {
            when (val d = getPendingDecision()) {
                is YesNoDecision -> { mayPrompts++; answerYesNo(acceptMay) }
                is SelectCardsDecision -> selectCards(d.options.take(maxOf(d.minSelections, 1)))
                null -> resolveStack()
                else ->
                    if (d::class.simpleName?.contains("Mana") == true) submitManaSourcesAutoPay()
                    else error("Unexpected decision: ${d::class.simpleName}")
            }
        }
        return mayPrompts
    }

    private fun TestGame.controllerOf(name: String) =
        findPermanent(name)?.let { state.getEntity(it)?.get<ControllerComponent>()?.playerId }

    init {
        cardRegistry.register(TergridGodOfFright)
        cardRegistry.register(cullOne)
        cardRegistry.register(cullLand)
        cardRegistry.register(mindRot)
        cardRegistry.register(shockCard)

        test("an opponent sacrificing a nontoken creature lets you put it onto the battlefield under your control") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Tergrid, God of Fright")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Tergrid Test Cull")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Tergrid Test Cull")
            game.resolveAll() shouldBe 1

            withClue("the sacrificed Bears left the opponent's graveyard for your battlefield") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                game.controllerOf("Grizzly Bears") shouldBe p1
            }
        }

        test("declining the may leaves the card in its owner's graveyard") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Tergrid, God of Fright")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Tergrid Test Cull")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Tergrid Test Cull")
            game.resolveAll(acceptMay = false) shouldBe 1

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("a sacrificed token does not trigger it") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Tergrid, God of Fright")
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardInHand(2, "Tergrid Test Cull")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Tergrid Test Cull")
            game.resolveAll() shouldBe 0
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("your own sacrifice does not trigger it") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Tergrid, God of Fright")
                .withCardOnBattlefield(1, "Forest")
                .withCardInHand(1, "Tergrid Test Land Cull")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // A land: the only candidate, so the sacrifice needs no choice (Tergrid isn't one).
            game.castSpell(1, "Tergrid Test Land Cull")
            game.resolveAll() shouldBe 0
            game.isInGraveyard(1, "Forest") shouldBe true
        }

        test("an opponent discarding a permanent card lets you put it onto the battlefield under your control") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Tergrid, God of Fright")
                .withCardInHand(1, "Tergrid Test Mind Rot")
                .withCardInHand(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Tergrid Test Mind Rot")
            game.resolveAll() shouldBe 1

            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            game.controllerOf("Grizzly Bears") shouldBe p1
        }

        test("an opponent discarding a nonpermanent card does not trigger it") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Tergrid, God of Fright")
                .withCardInHand(1, "Tergrid Test Mind Rot")
                .withCardInHand(2, "Tergrid Test Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Tergrid Test Mind Rot")
            game.resolveAll() shouldBe 0
            game.isInGraveyard(2, "Tergrid Test Shock") shouldBe true
        }
    }
}
