package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vengeful Townsfolk (OTJ #37) — "Whenever one or more other creatures you control die, put a
 * +1/+1 counter on this creature."
 *
 * Exercises the once-per-batch death trigger ([com.wingedsheep.sdk.dsl.Triggers.OneOrMoreCreaturesYouControlDie]).
 * The critical case is a board wipe: several of your creatures dying simultaneously must add a
 * single counter, not one per creature (the over-counting a per-creature death trigger suffers).
 *
 * Pyroclasm ("2 damage to each creature") is the simultaneous-death engine: it kills the 2/2s
 * outright while the 3/3 Vengeful Townsfolk survives, so its counter is observable.
 */
class VengefulTownsfolkScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame): Int {
        val id = game.findPermanent("Vengeful Townsfolk")!!
        return game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    /**
     * A {1}{B} 2/2 with the same once-per-batch trigger as Vengeful Townsfolk, but a *non-self*
     * payoff — "Whenever one or more other creatures you control die, you gain 2 life." It dies to
     * Pyroclasm itself, so it exercises the Rule 603.10 "look back in time" path: a trigger source
     * that dies in the same batch as another qualifying creature must still see that creature dying.
     * (Vengeful Townsfolk's own +1/+1 is a no-op once it is in the graveyard, so it cannot prove this.)
     */
    private val grievingSentinel = CardDefinition.creature(
        name = "Grieving Sentinel",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 2,
        oracleText = "Whenever one or more other creatures you control die, you gain 2 life.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = EventPattern.CreaturesYouControlDiedEvent(excludeSelf = true),
                binding = TriggerBinding.ANY,
                effect = GainLifeEffect(2)
            )
        )
    )

    init {
        cardRegistry.register(grievingSentinel)

        context("Vengeful Townsfolk") {
            test("board wipe killing two of your creatures adds exactly one +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Townsfolk")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Pyroclasm")
                withClue("Casting Pyroclasm should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Both 2/2 Glory Seekers should have died to 2 damage") {
                    game.findPermanents("Glory Seeker").size shouldBe 0
                }
                withClue("The 3/3 Vengeful Townsfolk should survive 2 damage") {
                    (game.findPermanent("Vengeful Townsfolk") != null) shouldBe true
                }
                withClue("Two simultaneous deaths form one batch → exactly one +1/+1 counter, not two") {
                    plusOneCounters(game) shouldBe 1
                }
            }

            test("a single other creature dying adds one +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Townsfolk")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pyroclasm")
                game.resolveStack()

                withClue("The single Glory Seeker should have died") {
                    game.findPermanents("Glory Seeker").size shouldBe 0
                }
                withClue("One other creature died → one +1/+1 counter") {
                    plusOneCounters(game) shouldBe 1
                }
            }

            test("creatures an opponent controls dying does not trigger your Vengeful Townsfolk") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vengeful Townsfolk")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pyroclasm")
                game.resolveStack()

                withClue("The opponent's Glory Seekers should have died") {
                    game.findPermanents("Glory Seeker").size shouldBe 0
                }
                withClue("Only creatures the opponent controls died → no counter on your Vengeful Townsfolk") {
                    plusOneCounters(game) shouldBe 0
                }
            }
        }

        context("OneOrMoreCreaturesYouControlDie — source dies in the same batch (CR 603.10)") {
            test("a source that dies alongside another creature still sees that death (non-self payoff fires)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grieving Sentinel") // 2/2, dies to Pyroclasm
                    .withCardOnBattlefield(1, "Glory Seeker")       // 2/2, dies to Pyroclasm
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                val cast = game.castSpell(1, "Pyroclasm")
                withClue("Casting Pyroclasm should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Both 2/2s should have died to 2 damage in one batch") {
                    (game.findPermanent("Grieving Sentinel") == null) shouldBe true
                    game.findPermanents("Glory Seeker").size shouldBe 0
                }
                withClue(
                    "Grieving Sentinel died in the same batch as Glory Seeker; its 'one or more " +
                        "other creatures you control die' trigger must still fire (look-back), gaining 2 life"
                ) {
                    game.getLifeTotal(1) shouldBe (lifeBefore + 2)
                }
            }

            test("a source dying alone with no other creatures does not fire its 'other creatures die' trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grieving Sentinel") // only creature in play
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpell(1, "Pyroclasm")
                game.resolveStack()

                withClue("Grieving Sentinel should have died") {
                    (game.findPermanent("Grieving Sentinel") == null) shouldBe true
                }
                withClue("No *other* creature died, so the excludeSelf trigger must not fire — no life gained") {
                    game.getLifeTotal(1) shouldBe lifeBefore
                }
            }
        }
    }
}
