package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.player.EnteredPermanentRecord
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bespoke Battlegarb (Wilds of Eldraine) — the Celebration *free attach* trigger.
 *
 * "Celebration — At the beginning of combat on your turn, if two or more nonland permanents entered
 * the battlefield under your control this turn, attach this Equipment to **up to one** target
 * creature you control."
 *
 * The intervening-'if' half is already pinned generically by [CelebrationScenarioTest]; what is
 * card-specific here is the *optional* target on an attach effect. "Up to one target" means the
 * ability must resolve cleanly when the controller declines — the Equipment simply stays where it
 * is — and that path is the one an attach effect could plausibly get wrong, since attaching with no
 * target has nothing to attach to.
 */
class BespokeBattlegarbScenarioTest : ScenarioTestBase() {

    private fun attachedHost(game: TestGame, equipmentId: EntityId): EntityId? =
        game.state.getEntity(equipmentId)?.get<AttachedToComponent>()?.targetId

    /** Overwrite the entry log wholesale — the cheapest way to switch Celebration on. */
    private fun celebrate(game: TestGame, playerId: EntityId) {
        game.state = game.state.updateEntity(playerId) { container ->
            container.with(
                PermanentsEnteredUnderControlThisTurnComponent(
                    (0..1).map { EnteredPermanentRecord(EntityId.of("entry-$it"), setOf(CardType.CREATURE)) }
                )
            )
        }
    }

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Beast",
                manaCost = ManaCost.parse("{G}"),
                subtypes = setOf(Subtype.BEAST),
                power = 1,
                toughness = 1
            )
        )

        context("Bespoke Battlegarb — Celebration attach") {

            test("attaches to the chosen creature and grants +2/+0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bespoke Battlegarb")
                    .withCardOnBattlefield(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                celebrate(game, game.player1Id)

                val battlegarb = game.findPermanent("Bespoke Battlegarb")!!
                val beast = game.findPermanent("Test Beast")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(beast))
                game.resolveStack()

                attachedHost(game, battlegarb) shouldBe beast
                withClue("the equipped creature gets +2/+0") {
                    game.state.projectedState.getPower(beast) shouldBe 3
                    game.state.projectedState.getToughness(beast) shouldBe 1
                }
            }

            test("declining the optional target resolves cleanly and attaches nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bespoke Battlegarb")
                    .withCardOnBattlefield(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                celebrate(game, game.player1Id)

                val battlegarb = game.findPermanent("Bespoke Battlegarb")!!
                val beast = game.findPermanent("Test Beast")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.skipTargets()
                game.resolveStack()

                withClue("no target chosen — the Equipment stays unattached") {
                    attachedHost(game, battlegarb) shouldBe null
                    game.state.projectedState.getPower(beast) shouldBe 1
                }
            }

            test("does not trigger when fewer than two nonland permanents entered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bespoke Battlegarb")
                    .withCardOnBattlefield(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val battlegarb = game.findPermanent("Bespoke Battlegarb")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("the intervening-'if' fails at trigger time, so nothing asks for a target") {
                    game.state.pendingDecision shouldBe null
                }
                game.resolveStack()
                attachedHost(game, battlegarb) shouldBe null
            }
        }
    }
}
