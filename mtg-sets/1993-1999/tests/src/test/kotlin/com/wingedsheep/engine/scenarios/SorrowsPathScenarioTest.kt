package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sorrow's Path (DRK #119).
 *
 * Land
 * "{T}: Choose two target blocking creatures controlled by the same opponent. If each of those
 *  creatures could block all creatures that the other is blocking, remove both of them from combat.
 *  Each one then blocks all creatures the other was blocking.
 *  Whenever this land becomes tapped, it deals 2 damage to you and each creature you control."
 *
 * Both halves are tested, including the two gates: the targets must be an opponent's blockers, and
 * a swap that would hand a ground creature a flier is refused outright rather than half-applied.
 */
class SorrowsPathScenarioTest : ScenarioTestBase() {

    init {
        fun pathAbilityId() =
            cardRegistry.getCard("Sorrow's Path")!!.script.activatedAbilities[0].id

        context("Sorrow's Path") {

            test("refuses the swap when one blocker couldn't block the other's attacker") {
                // Scryb Sprites blocks a flier; Wall of Wood has no flying or reach, so handing it
                // the Serra Angel would be an illegal block. The printed gate refuses the whole
                // swap rather than half-applying it — and this is why the card is unusable.
                val game = scenario()
                    .withPlayers("Pathfinder", "Defender")
                    .withCardOnBattlefield(1, "Sorrow's Path")
                    .withCardOnBattlefield(1, "Serra Angel")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Wood")
                    .withCardOnBattlefield(2, "Scryb Sprites")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(
                    mapOf("Serra Angel" to 2, "Craw Wurm" to 2)
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val angel = game.findPermanent("Serra Angel")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val wall = game.findPermanent("Wall of Wood")!!
                val sprites = game.findPermanent("Scryb Sprites")!!

                game.declareBlockers(
                    mapOf(
                        "Scryb Sprites" to listOf("Serra Angel"),
                        "Wall of Wood" to listOf("Craw Wurm"),
                    )
                ).error shouldBe null
                game.passPriority()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Sorrow's Path")!!,
                        abilityId = pathAbilityId(),
                        targets = listOf(
                            entityIdToChosenTarget(game.state, wall),
                            entityIdToChosenTarget(game.state, sprites),
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the swap is refused outright — assignments are unchanged") {
                    game.state.getEntity(wall)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(wurm)
                    game.state.getEntity(sprites)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(angel)
                }
            }

            test("refuses to swap the activating player's own blockers") {
                // "…controlled by the same opponent". On the opponent's turn the Sorrow's Path
                // player is the one blocking, so their own two blockers are the only blocking
                // creatures on the board — and they are not legal targets. Both creatures could
                // legally block either attacker, so nothing but the controller restriction is
                // stopping this swap.
                val game = scenario()
                    .withPlayers("Pathfinder", "Attacker")
                    .withCardOnBattlefield(1, "Sorrow's Path")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withCardOnBattlefield(1, "Scryb Sprites")
                    .withCardOnBattlefield(2, "Hurloon Minotaur")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(
                    mapOf("Hurloon Minotaur" to 1, "Craw Wurm" to 1)
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val minotaur = game.findPermanent("Hurloon Minotaur")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val wall = game.findPermanent("Wall of Wood")!!
                val sprites = game.findPermanent("Scryb Sprites")!!

                game.declareBlockers(
                    mapOf(
                        "Wall of Wood" to listOf("Hurloon Minotaur"),
                        "Scryb Sprites" to listOf("Craw Wurm"),
                    )
                ).error shouldBe null

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Sorrow's Path")!!,
                        abilityId = pathAbilityId(),
                        targets = listOf(
                            entityIdToChosenTarget(game.state, wall),
                            entityIdToChosenTarget(game.state, sprites),
                        )
                    )
                )

                withClue("your own blockers are not legal targets") {
                    result.error.shouldNotBeNull()
                }
                withClue("nothing moved, and the land never tapped") {
                    game.state.getEntity(wall)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(minotaur)
                    game.state.getEntity(sprites)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(wurm)
                }
            }

            test("swaps two legal blockers, and damages its controller for tapping") {
                // Two ground attackers, both tough enough to survive the land's own 2 damage —
                // otherwise the becomes-tapped trigger resolves *first* (it goes on the stack above
                // the ability), kills an attacker, and the swap fizzles because its target is no
                // longer blocking anything. That ordering is the card's actual joke.
                // Both blockers can legally block either attacker (a flier may block a ground
                // creature), so the swap is legal in both directions.
                val game = scenario()
                    .withPlayers("Pathfinder", "Defender")
                    .withCardOnBattlefield(1, "Sorrow's Path")
                    .withCardOnBattlefield(1, "Hurloon Minotaur")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Wood")
                    .withCardOnBattlefield(2, "Scryb Sprites")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(
                    mapOf("Hurloon Minotaur" to 2, "Craw Wurm" to 2)
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val minotaur = game.findPermanent("Hurloon Minotaur")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val wall = game.findPermanent("Wall of Wood")!!
                val sprites = game.findPermanent("Scryb Sprites")!!

                game.declareBlockers(
                    mapOf(
                        "Wall of Wood" to listOf("Hurloon Minotaur"),
                        "Scryb Sprites" to listOf("Craw Wurm"),
                    )
                ).error shouldBe null

                withClue("both creatures are actually blocking before the swap") {
                    game.state.getEntity(wall)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(minotaur)
                    game.state.getEntity(sprites)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(wurm)
                }

                // After blockers are declared the defender holds priority, so pass it to the
                // active player — the one holding Sorrow's Path.
                game.passPriority()
                game.state.priorityPlayerId shouldBe game.player1Id

                val lifeBefore = game.getLifeTotal(1)
                val path = game.findPermanent("Sorrow's Path")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = path,
                        abilityId = pathAbilityId(),
                        targets = listOf(
                            entityIdToChosenTarget(game.state, wall),
                            entityIdToChosenTarget(game.state, sprites),
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the blockers traded assignments") {
                    game.state.getEntity(wall)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(wurm)
                    game.state.getEntity(sprites)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(minotaur)
                }
                withClue("tapping the land cost its controller 2 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 2
                }
                withClue("both attackers are tough enough to survive their own land's 2 damage") {
                    game.findPermanent("Hurloon Minotaur").shouldNotBeNull()
                    game.findPermanent("Craw Wurm").shouldNotBeNull()
                }
            }

            test("refuses the same blocker named twice, before the land ever taps") {
                // "Two target blocking creatures" is one instance of "target", so CR 601.2c makes
                // the pair distinct — naming the same blocker twice is an illegal *announcement*,
                // not a spell that fizzles. The difference is the whole point of this test: the tap
                // is a cost, so an ability that fizzled at resolution would already have dealt the
                // becomes-tapped trigger's 2 damage across its controller's board and left the land
                // tapped for nothing.
                val game = scenario()
                    .withPlayers("Pathfinder", "Defender")
                    .withCardOnBattlefield(1, "Sorrow's Path")
                    .withCardOnBattlefield(1, "Hurloon Minotaur")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Wood")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(
                    mapOf("Hurloon Minotaur" to 2, "Craw Wurm" to 2)
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val minotaur = game.findPermanent("Hurloon Minotaur")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val wall = game.findPermanent("Wall of Wood")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.declareBlockers(
                    mapOf(
                        "Wall of Wood" to listOf("Hurloon Minotaur"),
                        "Grizzly Bears" to listOf("Craw Wurm"),
                    )
                ).error shouldBe null
                game.passPriority()

                val lifeBefore = game.getLifeTotal(1)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Sorrow's Path")!!,
                        abilityId = pathAbilityId(),
                        targets = listOf(
                            entityIdToChosenTarget(game.state, wall),
                            entityIdToChosenTarget(game.state, wall),
                        )
                    )
                )

                withClue("the same creature can't fill both target slots") {
                    result.error.shouldNotBeNull()
                }
                withClue("so the land never tapped and its controller took no damage") {
                    game.getLifeTotal(1) shouldBe lifeBefore
                }
                withClue("and the blocks are untouched") {
                    game.state.getEntity(wall)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(minotaur)
                    game.state.getEntity(bears)?.get<BlockingComponent>()
                        ?.blockedAttackerIds?.toList() shouldContainExactly listOf(wurm)
                }
            }
        }
    }
}
