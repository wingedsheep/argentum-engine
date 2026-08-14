package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.OdricLunarchMarshal
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Odric, Lunarch Marshal (SOI #31, {3}{W}, 3/3).
 *
 *   At the beginning of each combat, creatures you control gain first strike until end of turn if a
 *   creature you control has first strike. The same is true for flying, deathtouch, double strike,
 *   haste, hexproof, indestructible, lifelink, menace, reach, skulk, trample, and vigilance.
 *
 * The card is twelve independent `if you control a creature with K, grant K to creatures you
 * control` clauses, so the things that can silently go wrong are all about the *gate*, not the
 * grant:
 *
 *  - a keyword nobody has must not be handed out (the condition has to actually gate);
 *  - a keyword somebody has must reach every creature you control, including Odric himself;
 *  - and the grant must be a resolution-time snapshot, not a continuously re-evaluated condition —
 *    the printed ruling says the granted abilities "won't change even if every creature that
 *    normally had the abilities leaves the battlefield". Wiring this through `GrantKeyword`'s
 *    `condition` parameter instead of a `ConditionalEffect` would pass the first two tests and fail
 *    the third, which is why the third one exists.
 *
 * Also pinned: the trigger is "each combat", so it fires on the opponent's turn too.
 */
class OdricLunarchMarshalScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(OdricLunarchMarshal))

        context("sharing a keyword someone has") {

            test("first strike spreads to every creature you control, Odric included") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Odric, Lunarch Marshal")
                    .withCardOnBattlefield(1, "First Strike Knight")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val odric = game.findPermanent("Odric, Lunarch Marshal")!!
                val lions = game.findPermanent("Savannah Lions")!!

                withClue("the Lions start without first strike") {
                    game.state.projectedState.hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the vanilla creature picked up first strike") {
                    projected.hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("Odric shares with himself too — he is a creature you control") {
                    projected.hasKeyword(odric, Keyword.FIRST_STRIKE) shouldBe true
                }
            }

            test("an opponent's creature is not affected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Odric, Lunarch Marshal")
                    .withCardOnBattlefield(1, "First Strike Knight")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirLions = game.findPermanent("Savannah Lions")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("\"creatures you control\" excludes the opponent's board") {
                    game.state.projectedState
                        .hasKeyword(theirLions, Keyword.FIRST_STRIKE) shouldBe false
                }
            }
        }

        context("the gate") {

            test("a keyword no creature you control has is not granted") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Odric, Lunarch Marshal")
                    .withCardOnBattlefield(1, "First Strike Knight")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("nobody has trample, so nobody gains trample") {
                    projected.hasKeyword(lions, Keyword.TRAMPLE) shouldBe false
                }
                withClue("nobody has deathtouch, so nobody gains deathtouch") {
                    projected.hasKeyword(lions, Keyword.DEATHTOUCH) shouldBe false
                }
                withClue("nobody has flying, so nobody gains flying") {
                    projected.hasKeyword(lions, Keyword.FLYING) shouldBe false
                }
            }

            test("with no first striker on board, first strike is not granted") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Odric, Lunarch Marshal")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("the intervening condition really gates the grant") {
                    game.state.projectedState
                        .hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe false
                }
            }
        }

        context("the grant is a snapshot, not a live condition") {

            test("keeps first strike after the only first striker leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Odric, Lunarch Marshal")
                    .withCardOnBattlefield(1, "First Strike Knight")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knight = game.findPermanent("First Strike Knight")!!
                val lions = game.findPermanent("Savannah Lions")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("granted while the knight was around") {
                    game.state.projectedState.hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe true
                }

                // The creature that supplied first strike dies.
                game.state = ZoneTransitionService
                    .moveToZone(game.state, knight, Zone.GRAVEYARD).state

                withClue("the granted keyword survives — it is not re-checked each projection") {
                    game.state.projectedState.hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe true
                }
            }
        }

        context("\"each combat\", not \"your combat\"") {

            test("fires on the opponent's turn as well") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Odric, Lunarch Marshal")
                    .withCardOnBattlefield(1, "First Strike Knight")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("blocking matters too, so the trigger must fire on their combat") {
                    game.state.projectedState.hasKeyword(lions, Keyword.FIRST_STRIKE) shouldBe true
                }
            }
        }
    }
}
