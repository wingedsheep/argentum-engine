package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Goblin-town (HOB #183) — Land.
 *
 * "{T}: Add {B} or {R}.
 *  {2}{B}{R}, {T}, Sacrifice this land: Put two +1/+1 counters on target Goblin or Orc you control.
 *  Activate only as a sorcery."
 *
 * The sacrifice ability is the only new shape in the HOB tapland cycle: a *single* target with an
 * OR over two subtypes. This proves both legs are legal and that a creature of neither type is not,
 * so the OR didn't collapse into "creature you control".
 */
class GoblinTownScenarioTest : ScenarioTestBase() {

    // Vanilla proof creatures — nothing that could confound the target check.
    private val testGoblin = card("Goblin-town Test Goblin") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }
    private val testOrc = card("Goblin-town Test Orc") {
        manaCost = "{B}"
        typeLine = "Creature — Orc"
        power = 1
        toughness = 1
    }
    private val testBear = card("Goblin-town Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(listOf(testGoblin, testOrc, testBear))

        context("Goblin-town") {

            test("the sacrifice ability puts two +1/+1 counters on a Goblin you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin-town")
                    .withCardOnBattlefield(1, "Goblin-town Test Goblin")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = game.findPermanent("Goblin-town")!!
                val goblin = game.findPermanent("Goblin-town Test Goblin")!!
                // ability_1 / ability_2 are the two mana abilities; ability_3 is the sacrifice one.
                val pump = cardRegistry.requireCard("Goblin-town").activatedAbilities.last().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = land,
                        abilityId = pump,
                        targets = listOf(ChosenTarget.Permanent(goblin)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the Goblin got two +1/+1 counters") {
                    game.state.getEntity(goblin)
                        ?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
                withClue("the sacrifice cost consumed the land") {
                    game.findPermanent("Goblin-town") shouldBe null
                    game.isInGraveyard(1, "Goblin-town") shouldBe true
                }
            }

            test("an Orc is a legal target but a creature of neither type is not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin-town")
                    .withCardOnBattlefield(1, "Goblin-town Test Orc")
                    .withCardOnBattlefield(1, "Goblin-town Test Bear")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val orc = game.findPermanent("Goblin-town Test Orc")!!
                val bear = game.findPermanent("Goblin-town Test Bear")!!

                val pumpAction = game.getLegalActions(1)
                    .first { it.description.contains("+1/+1 counters") }
                val validTargets = pumpAction.validTargets

                withClue("the Orc leg of the OR is legal") {
                    validTargets!! shouldContain orc
                }
                withClue("a Bear satisfies neither subtype") {
                    validTargets!! shouldNotContain bear
                }
            }
        }
    }
}
