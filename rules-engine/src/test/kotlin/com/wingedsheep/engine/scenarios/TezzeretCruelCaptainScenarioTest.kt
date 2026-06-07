package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tezzeret, Cruel Captain (EOE, {3}, Loyalty 4).
 *
 *   Whenever an artifact you control enters, put a loyalty counter on Tezzeret.
 *   0: Untap target artifact or creature. If it's an artifact creature, put a +1/+1 counter on it.
 *   −3: Search your library for an artifact card with mana value 1 or less, reveal it, put it into
 *       your hand, then shuffle.
 *   −7: You get an emblem with "At the beginning of combat on your turn, put three +1/+1 counters
 *       on target artifact you control. If it's not a creature, it becomes a 0/0 Robot artifact
 *       creature."
 *
 * Exercises the artifact-enters trigger (must fire only for artifacts), the 0 ability's
 * "if it's an artifact creature" projection-gated counter, and the −7 emblem's grant-then-
 * counter-then-BecomeCreature sequence (which is what makes a Vehicle/non-creature artifact
 * end up as a 3/3 Robot, per the 2025-07-25 ruling).
 */
class TezzeretCruelCaptainScenarioTest : ScenarioTestBase() {

    // {1} non-creature artifact, used as the cheap artifact in the enters-trigger test and as
    // the target of the −7 emblem's "if it's not a creature" branch.
    private val ironTrinket = CardDefinition.artifact(
        name = "Iron Trinket",
        manaCost = ManaCost.parse("{1}")
    )

    init {
        cardRegistry.register(ironTrinket)

        context("Tezzeret, Cruel Captain") {

            test("an artifact you control entering puts a loyalty counter on Tezzeret") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret, Cruel Captain")
                    .withCardInHand(1, "Iron Trinket")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret, Cruel Captain")!!
                game.state = game.state.updateEntity(tezzeret) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
                }

                game.castSpell(1, "Iron Trinket").error shouldBe null
                game.resolveStack()

                withClue("artifact entered → Tezzeret gains the loyalty trigger") {
                    loyalty(game, tezzeret) shouldBe 5
                }
            }

            test("a non-artifact entering does NOT bump Tezzeret's loyalty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret, Cruel Captain")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret, Cruel Captain")!!
                game.state = game.state.updateEntity(tezzeret) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("non-artifact entering filtered out by GameObjectFilter.Artifact.youControl") {
                    loyalty(game, tezzeret) shouldBe 4
                }
            }

            test("0 ability untaps target artifact creature AND puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret, Cruel Captain")
                    .withCardOnBattlefield(1, "Palladium Myr", tapped = true, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret, Cruel Captain")!!
                val myr = game.findPermanent("Palladium Myr")!!
                game.state = game.state.updateEntity(tezzeret) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
                }

                val zero = cardRegistry.getCard("Tezzeret, Cruel Captain")!!.script.activatedAbilities[0]
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tezzeret,
                        abilityId = zero.id,
                        targets = listOf(ChosenTarget.Permanent(myr))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("artifact creature → untapped and gains a +1/+1 counter") {
                    game.state.getEntity(myr)?.has<TappedComponent>() shouldBe false
                    plusOnePlusOne(game, myr) shouldBe 1
                }
            }

            test("0 ability on a non-artifact creature untaps it but does NOT add a +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret, Cruel Captain")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret, Cruel Captain")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(tezzeret) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
                }

                val zero = cardRegistry.getCard("Tezzeret, Cruel Captain")!!.script.activatedAbilities[0]
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tezzeret,
                        abilityId = zero.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("non-artifact creature → untapped but no +1/+1 counter") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                    plusOnePlusOne(game, bears) shouldBe 0
                }
            }

            test("−7 emblem: at next begin-combat, target non-creature artifact gains three +1/+1 counters and becomes a 0/0 Robot artifact creature (so net 3/3)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tezzeret, Cruel Captain")
                    .withCardOnBattlefield(1, "Iron Trinket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tezzeret = game.findPermanent("Tezzeret, Cruel Captain")!!
                val trinket = game.findPermanent("Iron Trinket")!!
                game.state = game.state.updateEntity(tezzeret) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
                }

                val minus7 = cardRegistry.getCard("Tezzeret, Cruel Captain")!!.script.activatedAbilities[2]
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tezzeret,
                        abilityId = minus7.id
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("emblem persists after Tezzeret dies to SBA from 0 loyalty") {
                    game.findPermanent("Tezzeret, Cruel Captain") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                // Walk into the begin-combat step on the same turn — the emblem's
                // "at the beginning of combat on your turn" trigger fires there and asks for
                // a target. The only legal target is the lone artifact we control.
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(trinket))
                game.resolveStack()

                withClue("Iron Trinket got three +1/+1 counters AND became a 0/0 Robot") {
                    plusOnePlusOne(game, trinket) shouldBe 3
                    val projected = game.state.projectedState
                    projected.isCreature(trinket) shouldBe true
                    val pv = projected.getProjectedValues(trinket)
                    pv?.power shouldBe 3
                    pv?.toughness shouldBe 3
                }
            }
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun plusOnePlusOne(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
}
