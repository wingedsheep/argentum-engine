package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Melira, the Living Cure (ONE #209) — {G}{W} 3/3 Legendary Creature — Human Scout.
 *
 * "If you would get one or more poison counters, instead you get one poison counter and you can't
 *  get additional poison counters this turn. / Exile Melira: Choose another target creature or
 *  artifact. When it's put into a graveyard this turn, return that card to the battlefield under
 *  its owner's control."
 *
 * The full cap/lock matrix (toxic, proliferate, lock outliving the source, end of turn) lives in the
 * engine's `CapCounterPlacementThisTurnScenarioTest`; this pins the card.
 */
class MeliraTheLivingCureScenarioTest : ScenarioTestBase() {

    private val poison = card("Test Poison Three") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val p = target(Targets.Player)
            effect = Effects.AddCounters(CounterType.POISON, 3, p)
        }
    }

    private fun TestGame.poisonOf(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    private fun TestGame.exileMeliraTargeting(targetId: EntityId) {
        val melira = findPermanent("Melira, the Living Cure")!!
        val ability = cardRegistry.getCard("Melira, the Living Cure")!!.script.activatedAbilities.single()
        execute(ActivateAbility(player1Id, melira, ability.id, targets = listOf(ChosenTarget.Permanent(targetId))))
            .error shouldBe null
        resolveStack()
    }

    init {
        cardRegistry.register(poison)

        test("its controller gets one poison counter, then no more this turn") {
            val game = scenario()
                .withPlayers("Melira", "Opponent")
                .withCardOnBattlefield(1, "Melira, the Living Cure")
                .withCardInHand(2, "Test Poison Three")
                .withCardInHand(2, "Test Poison Three")
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Test Poison Three", 1).error shouldBe null
            game.resolveStack()
            game.poisonOf(game.player1Id) shouldBe 1

            game.castSpellTargetingPlayer(2, "Test Poison Three", 1).error shouldBe null
            game.resolveStack()
            game.poisonOf(game.player1Id) shouldBe 1
        }

        test("an opponent's poison counters aren't capped") {
            val game = scenario()
                .withPlayers("Melira", "Opponent")
                .withCardOnBattlefield(1, "Melira, the Living Cure")
                .withCardInHand(1, "Test Poison Three")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Test Poison Three", 2).error shouldBe null
            game.resolveStack()
            game.poisonOf(game.player2Id) shouldBe 3
        }

        test("exiling Melira returns the chosen creature when it dies this turn, under its owner's control") {
            val game = scenario()
                .withPlayers("Melira", "Opponent")
                .withCardOnBattlefield(1, "Melira, the Living Cure")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.exileMeliraTargeting(bears)
            game.isOnBattlefield("Melira, the Living Cure") shouldBe false

            game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("the Bears came back to the battlefield under their owner's control") {
                val returned = game.findPermanent("Grizzly Bears")
                (returned != null) shouldBe true
                game.state.getEntity(returned!!)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            }
        }

        test("the return only watches this turn") {
            val game = scenario()
                .withPlayers("Melira", "Opponent")
                .withCardOnBattlefield(1, "Melira, the Living Cure")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.exileMeliraTargeting(game.findPermanent("Grizzly Bears")!!)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id

            game.castSpell(1, "Lightning Bolt", targetId = game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
    }
}
