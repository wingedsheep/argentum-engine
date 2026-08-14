package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Beorn the Fierce (HOB #119) — {3}{G}{G} Legendary Creature — Bear Shapeshifter Warrior 6/6
 *
 * Trample
 * Other Bears you control get +2/+2.
 * At the beginning of combat on your turn, put a trample counter on up to one target creature you
 * control. It becomes a Bear in addition to its other types. Then if you control three or more
 * Bears, draw two cards.
 *
 * Three things here can silently go wrong, so each gets a test: the subtype must be *added* (not
 * set) and must outlive the turn; the newly-made Bear must count toward the draw check, and pick up
 * the anthem; and declining the optional target must not eat the draw clause.
 */
class BeornTheFierceScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    /** Pass until the begin-combat trigger asks for its "up to one target creature you control". */
    private fun advanceToTargetPrompt(game: TestGame) {
        var guard = 0
        while (guard++ < 30 && !game.hasPendingDecision()) {
            game.passPriority()
        }
        withClue("Beorn's begin-combat trigger should have prompted for a target") {
            game.hasPendingDecision() shouldBe true
        }
    }

    init {
        context("Beorn the Fierce") {

            test("the anthem pumps other Bears you control but not Beorn himself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Beorn the Fierce")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withCardOnBattlefield(2, "Large Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = projector.project(game.state)
                val beorn = game.findPermanent("Beorn the Fierce")!!
                val myBear = game.findPermanent("Ordinary Bear")!!
                val theirBear = game.findPermanent("Large Bear")!!

                withClue("Beorn stays 6/6 — 'Other'") {
                    projected.getPower(beorn) shouldBe 6
                    projected.getToughness(beorn) shouldBe 6
                }
                withClue("Ordinary Bear 4/5 -> 6/7") {
                    projected.getPower(myBear) shouldBe 6
                    projected.getToughness(myBear) shouldBe 7
                }
                withClue("The opponent's Bear is untouched — 'you control'") {
                    projected.getPower(theirBear) shouldBe 5
                    projected.getToughness(theirBear) shouldBe 5
                }
            }

            test("the trigger adds a trample counter and the Bear type, and draws on three Bears") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Beorn the Fierce")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withCardOnBattlefield(1, "Goblin-town Flunkies")
                    .withCardInLibrary(1, "Little Bear")
                    .withCardInLibrary(1, "Large Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val flunkies = game.findPermanent("Goblin-town Flunkies")!!
                val handBefore = game.handSize(1)

                advanceToTargetPrompt(game)
                game.selectTargets(listOf(flunkies))
                game.resolveStack()

                val projected = projector.project(game.state)

                withClue("A trample counter landed, and it grants trample through the layers") {
                    game.state.getEntity(flunkies)?.get<CountersComponent>()
                        ?.getCount(CounterType.TRAMPLE) shouldBe 1
                    projected.hasKeyword(flunkies, Keyword.TRAMPLE) shouldBe true
                }
                withClue("Bear is added, not substituted — Goblin and Soldier survive") {
                    projected.hasSubtype(flunkies, "Bear") shouldBe true
                    projected.hasSubtype(flunkies, "Goblin") shouldBe true
                    projected.hasSubtype(flunkies, "Soldier") shouldBe true
                }
                withClue("The new Bear picks up the anthem: base 1/1 -> 3/3") {
                    projected.getPower(flunkies) shouldBe 3
                    projected.getToughness(flunkies) shouldBe 3
                }
                withClue("Beorn + Ordinary Bear + the new Bear = three, so draw two") {
                    game.handSize(1) shouldBe handBefore + 2
                }
            }

            test("declining the optional target still runs the draw check") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Beorn the Fierce")
                    .withCardOnBattlefield(1, "Ordinary Bear")
                    .withCardInLibrary(1, "Little Bear")
                    .withCardInLibrary(1, "Large Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                advanceToTargetPrompt(game)
                game.skipTargets()
                game.resolveStack()

                withClue("Only two Bears, so no draw — but the trigger resolved without fizzling") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("Beorn is still around; the trigger didn't blow up on zero targets") {
                    game.isOnBattlefield("Beorn the Fierce") shouldBe true
                }
            }
        }
    }
}
