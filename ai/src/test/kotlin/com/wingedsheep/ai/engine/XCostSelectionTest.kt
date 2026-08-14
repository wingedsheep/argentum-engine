package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [XCostSelection] — which X values the AI considers, and the target narrowing that
 * keeps the resulting action legal.
 *
 * Kept off the simulation path on purpose. The end-to-end proof that the Strategist *uses* this
 * lives in [XCostSpellAiTest]; what matters here is that the candidate set and the narrowing are
 * exactly right, which a test whose answer depends on the evaluator could never pin down.
 */
class XCostSelectionTest : ScenarioTestBase() {

    /** A board with a known mana-value / power spread to derive X from. */
    private class Board(val state: GameState, val thopter: EntityId, val bears: EntityId, val giant: EntityId)

    private fun board(faceDownGiant: Boolean = false): Board {
        val game = scenario()
            .withPlayers()
            // Ornithopter: mana value 0, power 0. Grizzly Bears: 2 and 2. Hill Giant: 4 and 3.
            .withCardOnBattlefield(2, "Ornithopter")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val giant = game.findPermanent("Hill Giant")!!
        val state = if (faceDownGiant) {
            game.state.updateEntity(giant) { it.with(FaceDownComponent) }
        } else {
            game.state
        }
        return Board(state, game.findPermanent("Ornithopter")!!, game.findPermanent("Grizzly Bears")!!, giant)
    }

    private fun castAction(
        maxAffordableX: Int?,
        minX: Int = 0,
        validTargets: List<EntityId>? = null,
        requiresTargets: Boolean = false,
        targetCount: Int = 1,
        minTargets: Int = 1,
        xConstrainsTargetManaValue: Boolean = false,
        xConstrainsTargetManaValueExactly: Boolean = false,
        xConstrainsTargetPower: Boolean = false,
        xConstrainsTargetCount: Boolean = false,
        targetRequirements: List<TargetInfo>? = null,
    ) = LegalAction(
        action = CastSpell(playerId = EntityId.generate(), cardId = EntityId.generate()),
        actionType = "CastSpell",
        description = "Cast an X spell",
        hasXCost = true,
        maxAffordableX = maxAffordableX,
        minX = minX,
        validTargets = validTargets,
        requiresTargets = requiresTargets,
        targetCount = targetCount,
        minTargets = minTargets,
        xConstrainsTargetManaValue = xConstrainsTargetManaValue,
        xConstrainsTargetManaValueExactly = xConstrainsTargetManaValueExactly,
        xConstrainsTargetPower = xConstrainsTargetPower,
        xConstrainsTargetCount = xConstrainsTargetCount,
        targetRequirements = targetRequirements,
    )

    private fun LegalAction.chosenX(): Int? = (action as CastSpell).xValue

    init {
        context("a free X — nothing about the targets depends on it") {

            test("proposes every affordable value, biggest first") {
                val b = board()
                XCostSelection.candidateXValues(b.state, castAction(maxAffordableX = 9)) shouldBe
                    listOf(9, 8, 7, 6, 5, 4, 3, 2, 1)
            }

            test("X=0 is not a candidate — it is the enumerator's default and buys nothing") {
                val b = board()
                XCostSelection.candidateXValues(b.state, castAction(maxAffordableX = 1)) shouldBe listOf(1)
                XCostSelection.candidateXValues(b.state, castAction(maxAffordableX = 0)) shouldBe emptyList()
            }

            test("an 'X can't be 0' floor is respected") {
                val b = board()
                XCostSelection.candidateXValues(b.state, castAction(maxAffordableX = 4, minX = 3)) shouldBe
                    listOf(4, 3)
                // Can't afford even the minimum — no legal X at all.
                XCostSelection.candidateXValues(b.state, castAction(maxAffordableX = 2, minX = 3)) shouldBe
                    emptyList()
            }

            test("narrowing leaves an unconstrained target list untouched") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 5,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                )
                val narrowed = XCostSelection.narrowToX(b.state, action, 2).shouldNotBeNull()
                narrowed.validTargets shouldBe listOf(b.thopter, b.bears, b.giant)
            }
        }

        context("expanding to candidates") {

            test("keeps the top MAX_X_CANDIDATES, biggest first") {
                val b = board()
                XCostSelection.expandToX(b.state, castAction(maxAffordableX = 9))
                    .map { it.chosenX() } shouldBe listOf(9, 8, 7, 6, 5)
            }

            test("a range shorter than the cap is offered whole") {
                val b = board()
                XCostSelection.expandToX(b.state, castAction(maxAffordableX = 3))
                    .map { it.chosenX() } shouldBe listOf(3, 2, 1)
            }

            test("a proposal that cannot be cast is dropped rather than filling a candidate slot") {
                val b = board()
                // Requirement 0 proposes 4, 2 and 0 — the permanents' own mana values. Requirement 1
                // only ever accepts a creature with power 0, so X=0 is the single castable value,
                // and it is the *last* proposal: the cap has to be applied to what survives
                // narrowing, or on a wider board it would be cut off by proposals that cannot be
                // cast at all.
                val action = castAction(
                    maxAffordableX = 9,
                    targetRequirements = listOf(
                        TargetInfo(
                            index = 0,
                            description = "target permanent with mana value X",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.thopter, b.bears, b.giant),
                            xConstrainsManaValueExactly = true,
                        ),
                        TargetInfo(
                            index = 1,
                            description = "target creature with power X",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.thopter),
                            xConstrainsPower = true,
                        ),
                    ),
                )
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(4, 2, 0)
                XCostSelection.expandToX(b.state, action).map { it.chosenX() } shouldBe listOf(0)
            }

            test("an X-driven target count below the requirement's minimum is not castable") {
                val b = board()
                // "X target creatures", at least three of them: X of 1 and 2 cannot be cast.
                val action = castAction(
                    maxAffordableX = 6,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    targetCount = 1,
                    minTargets = 3,
                    xConstrainsTargetCount = true,
                )
                XCostSelection.expandToX(b.state, action).map { it.chosenX() } shouldBe
                    listOf(6, 5, 4, 3)
            }

            test("an uncastable spell expands to nothing at all") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.bears),
                    requiresTargets = true,
                    xConstrainsTargetManaValueExactly = true,
                    minX = 5, // Grizzly Bears' mana value of 2 is below the floor.
                )
                XCostSelection.expandToX(b.state, action) shouldBe emptyList()
            }
        }

        context("X gates which targets are legal") {

            test("'mana value X or less' derives candidates from the targets' own mana values") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValue = true,
                )
                // Exactly the values that reach a permanent — never 1, 3 or 5-9, which cost more
                // mana to hit the same thing.
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(4, 2, 0)
            }

            test("candidates above what the player can pay are dropped") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 3,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValue = true,
                )
                // Hill Giant's 4 is unaffordable, so it is not offered.
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(2, 0)
            }

            test("'power X' derives candidates from projected power") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetPower = true,
                )
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(3, 2, 0)
            }

            test("'mana value X or less' narrows the target list to what X reaches") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValue = true,
                )
                XCostSelection.narrowToX(b.state, action, 2)!!.validTargets shouldBe
                    listOf(b.thopter, b.bears)
                XCostSelection.narrowToX(b.state, action, 0)!!.validTargets shouldBe listOf(b.thopter)
            }

            test("'mana value X' exactly is an equality filter, not a ceiling") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValueExactly = true,
                )
                XCostSelection.narrowToX(b.state, action, 2)!!.validTargets shouldBe listOf(b.bears)
            }

            test("an X that reaches nothing is not a castable action") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValueExactly = true,
                )
                // No permanent has mana value 1, so this X cannot be cast.
                XCostSelection.narrowToX(b.state, action, 1).shouldBeNull()
            }
        }

        context("a face-down permanent has no mana cost (CR 708.2a)") {

            test("X is derived from 0, not the printed cost the card would show face up") {
                val b = board(faceDownGiant = true)
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValueExactly = true,
                )
                // Face up the Hill Giant would propose 4; face down it joins Ornithopter at 0.
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(2, 0)
            }

            test("narrowing agrees, so the AI cannot pick a target the engine rejects") {
                val b = board(faceDownGiant = true)
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValueExactly = true,
                )
                XCostSelection.narrowToX(b.state, action, 4).shouldBeNull()
                XCostSelection.narrowToX(b.state, action, 0)!!.validTargets shouldContainExactly
                    listOf(b.thopter, b.giant)
            }

            test("projected power follows the face-down 2/2, not the printed stats") {
                val b = board(faceDownGiant = true)
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetPower = true,
                )
                // The Giant's printed 3 is gone; it is a 2/2 alongside Grizzly Bears.
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(2, 0)
            }
        }

        context("X caps how many targets may be chosen") {

            test("the chosen X replaces the enumerator's placeholder count") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 5,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    targetCount = 1,
                    minTargets = 0,
                    xConstrainsTargetCount = true,
                )
                XCostSelection.narrowToX(b.state, action, 3)!!.targetCount shouldBe 3
            }

            test("a count cap does not gate legality, so X is swept rather than target-derived") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 4,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    targetCount = 1,
                    minTargets = 0,
                    xConstrainsTargetCount = true,
                )
                XCostSelection.candidateXValues(b.state, action) shouldBe listOf(4, 3, 2, 1)
            }
        }

        context("multi-requirement actions") {

            test("each requirement is narrowed on its own flags") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    targetRequirements = listOf(
                        TargetInfo(
                            index = 0,
                            description = "target permanent with mana value X or less",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.thopter, b.bears, b.giant),
                            xConstrainsManaValue = true,
                        ),
                        TargetInfo(
                            index = 1,
                            description = "target creature",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.thopter, b.bears, b.giant),
                        ),
                    ),
                )
                val narrowed = XCostSelection.narrowToX(b.state, action, 2).shouldNotBeNull()
                narrowed.targetRequirements!![0].validTargets shouldBe listOf(b.thopter, b.bears)
                narrowed.targetRequirements!![1].validTargets shouldBe
                    listOf(b.thopter, b.bears, b.giant)
            }

            test("emptying a mandatory requirement makes the whole X illegal") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    targetRequirements = listOf(
                        TargetInfo(
                            index = 0,
                            description = "target permanent with mana value X",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.bears),
                            xConstrainsManaValueExactly = true,
                        ),
                    ),
                )
                XCostSelection.narrowToX(b.state, action, 2).shouldNotBeNull()
                XCostSelection.narrowToX(b.state, action, 3).shouldBeNull()
            }

            test("an optional requirement emptied by X is not fatal") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    targetRequirements = listOf(
                        TargetInfo(
                            index = 0,
                            description = "up to one target permanent with mana value X or less",
                            minTargets = 0,
                            maxTargets = 1,
                            validTargets = listOf(b.giant),
                            xConstrainsManaValue = true,
                        ),
                    ),
                )
                val narrowed = XCostSelection.narrowToX(b.state, action, 1).shouldNotBeNull()
                narrowed.targetRequirements!![0].validTargets shouldBe emptyList()
            }

            test("the flat view is re-derived from the narrowed requirement, not narrowed twice") {
                val b = board()
                // The enumerator mirrors requirement 0 into the flat fields; both views must end up
                // saying the same thing about what X=2 reaches.
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.thopter, b.bears, b.giant),
                    requiresTargets = true,
                    xConstrainsTargetManaValue = true,
                    targetRequirements = listOf(
                        TargetInfo(
                            index = 0,
                            description = "target permanent with mana value X or less",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.thopter, b.bears, b.giant),
                            xConstrainsManaValue = true,
                        ),
                        TargetInfo(
                            index = 1,
                            description = "target creature",
                            minTargets = 1,
                            maxTargets = 1,
                            validTargets = listOf(b.thopter, b.bears, b.giant),
                        ),
                    ),
                )
                val narrowed = XCostSelection.narrowToX(b.state, action, 2).shouldNotBeNull()
                narrowed.validTargets shouldBe narrowed.targetRequirements!![0].validTargets
                narrowed.validTargets shouldBe listOf(b.thopter, b.bears)
            }
        }

        context("an optional single target is not made mandatory by requiresTargets") {

            // The enumerator flags "up to one target ..." as requiresTargets while leaving
            // minTargets at 0. Treating that as mandatory would drop a legal cast.
            test("an X that empties an optional flat target list is still castable") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.giant),
                    requiresTargets = true,
                    targetCount = 1,
                    minTargets = 0,
                    xConstrainsTargetManaValue = true,
                )
                val narrowed = XCostSelection.narrowToX(b.state, action, 1).shouldNotBeNull()
                narrowed.validTargets shouldBe emptyList()
                narrowed.minTargets shouldBe 0
            }

            test("a mandatory flat target list emptied by X is still fatal") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.giant),
                    requiresTargets = true,
                    targetCount = 1,
                    minTargets = 1,
                    xConstrainsTargetManaValue = true,
                )
                XCostSelection.narrowToX(b.state, action, 1).shouldBeNull()
            }
        }

        context("picking one X without simulating") {

            test("bindBestX takes the head of the candidate list") {
                val b = board()
                XCostSelection.bindBestX(b.state, castAction(maxAffordableX = 7)).chosenX() shouldBe 7
            }

            test("bindBestX skips an X that cannot be cast") {
                val b = board()
                val action = castAction(
                    maxAffordableX = 9,
                    validTargets = listOf(b.bears),
                    requiresTargets = true,
                    xConstrainsTargetManaValueExactly = true,
                )
                // 9 down to 3 all reach nothing; only Grizzly Bears' 2 is castable.
                val bound = XCostSelection.bindBestX(b.state, action)
                bound.chosenX() shouldBe 2
                bound.validTargets shouldBe listOf(b.bears)
            }

            test("an action with no X to bind comes back untouched") {
                val b = board()
                val noX = castAction(maxAffordableX = 4).copy(hasXCost = false)
                XCostSelection.bindBestX(b.state, noX) shouldBe noX
            }

            test("sampleX stays inside the candidate set and advances the generator") {
                val b = board()
                val action = castAction(maxAffordableX = 9)
                val offered = XCostSelection.expandToX(b.state, action).map { it.chosenX() }
                for (seed in 1L..40L) {
                    val rng = GameRng.seeded(seed)
                    val (chosen, next) = XCostSelection.sampleX(b.state, action, rng)
                    offered.contains(chosen.chosenX()) shouldBe true
                    (next == rng) shouldBe false
                }
            }

            test("sampleX does not always return the same X — a playout must stay stochastic") {
                val b = board()
                val action = castAction(maxAffordableX = 9)
                val distinct = (1L..40L)
                    .map { XCostSelection.sampleX(b.state, action, GameRng.seeded(it)).first.chosenX() }
                    .toSet()
                (distinct.size > 1) shouldBe true
            }

            test("sampleX is deterministic for one generator state") {
                val b = board()
                val action = castAction(maxAffordableX = 9)
                val first = XCostSelection.sampleX(b.state, action, GameRng.seeded(7L))
                val second = XCostSelection.sampleX(b.state, action, GameRng.seeded(7L))
                first.first.chosenX() shouldBe second.first.chosenX()
            }

            test("sampleX leaves an uncastable action and the generator alone") {
                val b = board()
                val rng = GameRng.seeded(3L)
                val action = castAction(maxAffordableX = 0)
                val (chosen, next) = XCostSelection.sampleX(b.state, action, rng)
                chosen shouldBe action
                next shouldBe rng
            }
        }

        test("an action with no affordable X is not expanded at all") {
            val b = board()
            XCostSelection.candidateXValues(b.state, castAction(maxAffordableX = null)) shouldBe emptyList()
        }

        test("every expanded candidate carries its own X and nothing else moved") {
            val b = board()
            val action = castAction(maxAffordableX = 3)
            val expanded = XCostSelection.expandToX(b.state, action)
            expanded shouldHaveSize 3
            expanded.forEach { it.description shouldBe action.description }
        }
    }
}
