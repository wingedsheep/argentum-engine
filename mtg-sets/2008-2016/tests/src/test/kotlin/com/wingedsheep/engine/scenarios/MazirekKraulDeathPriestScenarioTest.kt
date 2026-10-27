package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.c15.cards.MazirekKraulDeathPriest
import com.wingedsheep.sdk.core.CounterType
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
 * Mazirek, Kraul Death Priest — "Whenever a player sacrifices another permanent, put a +1/+1
 * counter on each creature you control."
 *
 * Exercises the any-player, per-permanent, OTHER-binding sacrifice trigger: an opponent
 * sacrificing two permanents at once fires it twice, it only grows *your* creatures, Mazirek
 * sacrificing itself alone doesn't fire it, and Mazirek sacrificed alongside another permanent
 * still triggers for that other permanent (ruling; leaves-the-battlefield look-back, CR 603.10a).
 */
class MazirekKraulDeathPriestScenarioTest : ScenarioTestBase() {

    private val doubleSacrifice = card("Double Sacrifice") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(GameObjectFilter.Creature, 2, EffectTarget.PlayerRef(Player.You))
        }
    }

    private val selfSacrifice = card("Self Sacrifice") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(
                GameObjectFilter.Creature.withSubtype("Insect"),
                1,
                EffectTarget.PlayerRef(Player.You)
            )
        }
    }

    private fun TestGame.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.controllerOf(id: EntityId): EntityId? =
        state.getEntity(id)?.get<ControllerComponent>()?.playerId

    private fun TestGame.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 60) {
            when (val d = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections))
                null -> resolveStack()
                else -> error("Unexpected decision: ${d::class.simpleName}")
            }
        }
    }

    init {
        cardRegistry.register(MazirekKraulDeathPriest)
        cardRegistry.register(doubleSacrifice)
        cardRegistry.register(selfSacrifice)

        test("an opponent sacrificing two creatures at once puts two counters on each creature you control") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Mazirek, Kraul Death Priest", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(2, "Double Sacrifice")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mazirek = game.findPermanent("Mazirek, Kraul Death Priest")!!
            val myBears = game.findPermanents("Grizzly Bears").first { game.controllerOf(it) == game.player1Id }

            game.castSpell(2, "Double Sacrifice")
            game.resolveAll()

            game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 2
            withClue("one trigger per sacrificed permanent (CR 603.2c)") {
                game.plusOne(mazirek) shouldBe 2
                game.plusOne(myBears) shouldBe 2
            }
            val opponentBears = game.findPermanents("Grizzly Bears").filter { game.controllerOf(it) == game.player2Id }
            opponentBears.size shouldBe 1
            withClue("only creatures Mazirek's controller controls get counters") {
                opponentBears.sumOf { game.plusOne(it) } shouldBe 0
            }
        }

        test("Mazirek sacrificing itself alone doesn't trigger it") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Mazirek, Kraul Death Priest", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Self Sacrifice")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Self Sacrifice")
            game.resolveAll()

            game.findPermanent("Mazirek, Kraul Death Priest") shouldBe null
            game.plusOne(bears) shouldBe 0
        }

        test("Mazirek sacrificed alongside another creature still triggers for the other one") {
            val game = scenario()
                .withPlayers("You", "Opponent")
                .withCardOnBattlefield(1, "Mazirek, Kraul Death Priest", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Double Sacrifice")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mazirek = game.findPermanent("Mazirek, Kraul Death Priest")!!
            val (sacrificedBears, survivingBears) = game.findPermanents("Grizzly Bears")

            game.castSpell(1, "Double Sacrifice")
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 60) {
                when (val d = game.getPendingDecision()) {
                    is SelectCardsDecision -> game.selectCards(listOf(mazirek, sacrificedBears))
                    null -> game.resolveStack()
                    else -> error("Unexpected decision: ${d::class.simpleName}")
                }
            }

            game.findPermanent("Mazirek, Kraul Death Priest") shouldBe null
            game.findPermanents("Grizzly Bears") shouldBe listOf(survivingBears)
            withClue("one trigger for the other sacrificed Bears; none for Mazirek itself") {
                game.plusOne(survivingBears) shouldBe 1
            }
        }
    }
}
