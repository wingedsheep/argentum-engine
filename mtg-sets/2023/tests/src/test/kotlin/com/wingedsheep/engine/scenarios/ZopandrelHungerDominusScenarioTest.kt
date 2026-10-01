package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.ZopandrelHungerDominus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zopandrel, Hunger Dominus (ONE #195) — {5}{G}{G} 4/6 Legendary Creature — Phyrexian Horror.
 *
 *   Reach
 *   At the beginning of each combat, double the power and toughness of each creature you control
 *   until end of turn.
 *   {G/P}{G/P}, Sacrifice two other creatures: Put an indestructible counter on Zopandrel.
 */
class ZopandrelHungerDominusScenarioTest : ScenarioTestBase() {

    init {
        test("at the beginning of your combat, your creatures double; opponent's do not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zopandrel, Hunger Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val projected = game.state.projectedState
            val zop = game.findPermanent("Zopandrel, Hunger Dominus")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            withClue("Zopandrel 4/6 -> 8/12") {
                projected.getPower(zop) shouldBe 8
                projected.getToughness(zop) shouldBe 12
            }
            withClue("Bears 2/2 -> 4/4") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
            }
            withClue("Opponent's Hill Giant unchanged") {
                projected.getPower(giant) shouldBe 3
                projected.getToughness(giant) shouldBe 3
            }
        }

        test("it also triggers at the beginning of an opponent's combat") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zopandrel, Hunger Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
        }

        test("the doubling wears off at end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zopandrel, Hunger Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()
            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("sacrificing two other creatures puts an indestructible counter on Zopandrel") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zopandrel, Hunger Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zop = game.findPermanent("Zopandrel, Hunger Dominus")!!
            val sacrificed = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!)
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = zop,
                    abilityId = ZopandrelHungerDominus.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = sacrificed)
                )
            )
            withClue("activation: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(zop)?.get<CountersComponent>()?.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1
            game.state.projectedState.hasKeyword(zop, Keyword.INDESTRUCTIBLE) shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
        }

        test("Zopandrel cannot be one of the two sacrificed creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Zopandrel, Hunger Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zop = game.findPermanent("Zopandrel, Hunger Dominus")!!
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = zop,
                    abilityId = ZopandrelHungerDominus.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(
                        sacrificedPermanents = listOf(zop, game.findPermanent("Grizzly Bears")!!)
                    )
                )
            )
            result.error shouldNotBe null
            game.isOnBattlefield("Zopandrel, Hunger Dominus") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
