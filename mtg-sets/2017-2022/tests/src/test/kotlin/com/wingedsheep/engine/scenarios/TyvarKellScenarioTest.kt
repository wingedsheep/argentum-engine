package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tyvar Kell (KHM #198, {2}{G}{G}, Loyalty 3).
 *
 * The −6 emblem is the interesting part: "Whenever you cast an Elf spell, it gains haste until end
 * of turn and you draw two cards." Per the ruling the haste carries through to the permanent the
 * spell becomes, and only until end of turn.
 */
class TyvarKellScenarioTest : ScenarioTestBase() {

    init {
        context("the +1") {
            test("puts a +1/+1 counter on the target Elf, untaps it and gives it deathtouch") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar Kell")
                    .withCardOnBattlefield(1, "Llanowar Elves", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tyvar = game.findPermanent("Tyvar Kell")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                activate(game, tyvar, index = 0, targets = listOf(ChosenTarget.Permanent(elves)))
                game.resolveStack()

                game.state.getEntity(elves)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.getEntity(elves)?.has<TappedComponent>() shouldBe false
                game.state.projectedState.hasKeyword(elves, Keyword.DEATHTOUCH) shouldBe true
                loyalty(game, tyvar) shouldBe 4
            }
        }

        context("the 0") {
            test("creates a 1/1 green Elf Warrior") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar Kell")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activate(game, game.findPermanent("Tyvar Kell")!!, index = 1)
                game.resolveStack()

                val token = game.findPermanent("Elf Warrior Token")
                withClue("an Elf Warrior token entered") { (token != null) shouldBe true }
                game.state.projectedState.hasSubtype(token!!, "Elf") shouldBe true
            }
        }

        context("the −6 emblem") {
            test("an Elf spell gains haste that carries onto the creature, and you draw two") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar Kell")
                    .withCardInHand(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tyvar = game.findPermanent("Tyvar Kell")!!
                setLoyalty(game, tyvar, 6)
                activate(game, tyvar, index = 2)
                game.resolveStack()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()

                val elves = game.findPermanent("Llanowar Elves")!!
                withClue("cast trigger drew two (minus the Elf that left the hand)") {
                    game.handSize(1) shouldBe handBefore - 1 + 2
                }
                withClue("the haste granted to the spell carried onto the permanent") {
                    game.state.projectedState.hasKeyword(elves, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the haste lasted only until end of turn") {
                    game.state.projectedState.hasKeyword(elves, Keyword.HASTE) shouldBe false
                }
            }

            test("a non-Elf spell doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar Kell")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tyvar = game.findPermanent("Tyvar Kell")!!
                setLoyalty(game, tyvar, 6)
                activate(game, tyvar, index = 2)
                game.resolveStack()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore - 1
                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.HASTE) shouldBe false
            }
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        val ability = cardRegistry.getCard("Tyvar Kell")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.LOYALTY to amount)))
        }
    }
}
