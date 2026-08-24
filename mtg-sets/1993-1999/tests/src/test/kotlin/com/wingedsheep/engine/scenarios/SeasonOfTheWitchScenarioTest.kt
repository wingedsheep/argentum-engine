package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisTurnComponent
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Season of the Witch (DRK #52).
 *
 * {B}{B}{B} Enchantment
 * "At the beginning of your upkeep, sacrifice this enchantment unless you pay 2 life.
 *  At the beginning of the end step, destroy all untapped creatures that didn't attack this turn,
 *  except for creatures that couldn't attack."
 *
 * The end-step sweep is the interesting half: it has to spare the ones that attacked, the ones left
 * tapped, the ones that never had the option (defender, "can't attack", summoning sickness), and —
 * the broadest exemptions, and the easiest to miss — every creature the *non-active* player
 * controls, since only the active player ever declares attackers (CR 508.1a), plus the entire board
 * on a turn whose combat phase was skipped, where no attacker could be declared at all. The last
 * two each come with a control test asserting the sweep still bites without them, because a sweep
 * that silently failed to run would pass every "spared" assertion on its own.
 */
class SeasonOfTheWitchScenarioTest : ScenarioTestBase() {

    init {
        context("Season of the Witch") {

            test("the end step destroys a creature that stayed home but spares the attacker") {
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hurloon Minotaur")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the Minotaur was untapped, could have attacked, and didn't") {
                    game.findPermanent("Hurloon Minotaur").shouldBeNull()
                }
                withClue("the Bears attacked, so they are spared (and are tapped besides)") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
            }

            test("the opponent's creatures are spared — they were never able to attack") {
                // The bug this guards: only the active player declares attackers (CR 508.1a), so a
                // creature the opponent controls could not have attacked this turn no matter how
                // untapped and healthy it is. The sweep is one-sided; it punishes the controller of
                // the turn for staying home.
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hurloon Minotaur")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the active player's own Bears could have attacked and didn't") {
                    game.findPermanent("Grizzly Bears").shouldBeNull()
                }
                withClue("it wasn't the opponent's turn — their creatures couldn't attack") {
                    game.findPermanent("Hurloon Minotaur").shouldNotBeNull()
                    game.findPermanent("Serra Angel").shouldNotBeNull()
                }
            }

            test("on the opponent's turn the sweep judges the opponent's creatures instead") {
                // "The end step", not "your end step": the enchantment keeps working while its
                // controller is the one who couldn't have attacked.
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hurloon Minotaur")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the Minotaur's controller was attacking this turn and kept it home") {
                    game.findPermanent("Hurloon Minotaur").shouldBeNull()
                }
                withClue("the Witch player couldn't have attacked on someone else's turn") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
            }

            test("a turn whose combat phase was skipped destroys nothing") {
                // False Peace / Fatespinner leave SkipCombatPhasesComponent on their target;
                // TurnManager consumes it at BEGIN_COMBAT and jumps straight to the postcombat
                // main phase, so no Declare Attackers Step ever happens. Nobody was offered the
                // choice to attack, so "creatures that couldn't attack" covers the whole board —
                // its controller's included. Paired with the control test below, which is the same
                // turn with the combat phase left intact.
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) {
                    it.with(SkipCombatPhasesComponent)
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("the combat phase really was skipped") {
                    game.state.getEntity(game.player1Id)
                        ?.has<AttackersDeclaredThisTurnComponent>() shouldBe false
                }
                game.resolveStack()

                withClue("no Declare Attackers Step happened, so staying home was nobody's choice") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
            }

            test("the same turn with its combat phase intact destroys the creature that stayed home") {
                // The control for the test above: identical board, minus the skip. The Bears reach
                // a Declare Attackers Step, are not declared, and pay for it.
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("the step happened — an empty declaration still marks it") {
                    game.state.getEntity(game.player1Id)
                        ?.has<AttackersDeclaredThisTurnComponent>() shouldBe true
                }
                game.resolveStack()

                withClue("the Bears reached a Declare Attackers Step and sat it out") {
                    game.findPermanent("Grizzly Bears").shouldBeNull()
                }
            }

            test("a Pacifism'd creature is spared because it can't attack") {
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hurloon Minotaur")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Pacifism means the Bears couldn't attack — spared") {
                    game.findPermanent("Grizzly Bears").shouldNotBeNull()
                }
                withClue("the Minotaur had no such excuse") {
                    game.findPermanent("Hurloon Minotaur").shouldBeNull()
                }
            }

            test("a Wall is spared because it couldn't attack") {
                // Wall of Wood has defender: staying home was never its choice. The Bears next to
                // it have no such excuse.
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("defender means it couldn't attack — spared") {
                    game.findPermanent("Wall of Wood").shouldNotBeNull()
                }
                withClue("the Bears could have attacked and didn't") {
                    game.findPermanent("Grizzly Bears").shouldBeNull()
                }
            }

            test("a creature that entered this turn is spared for summoning sickness") {
                // Cast on the same turn the sweep happens, so it never had the option to attack.
                // The Bears that were already there did.
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Hurloon Minotaur")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hurloon Minotaur").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("summoning sick, so it couldn't attack — spared") {
                    game.findPermanent("Hurloon Minotaur").shouldNotBeNull()
                }
                withClue("the Bears had been there since before the turn and stayed home") {
                    game.findPermanent("Grizzly Bears").shouldBeNull()
                }
            }

            test("paying the upkeep ransom keeps the enchantment") {
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                // Pass into the Witch player's own upkeep.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("paying 2 life keeps the enchantment") {
                    game.findPermanent("Season of the Witch").shouldNotBeNull()
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("declining the upkeep ransom sacrifices the enchantment") {
                val game = scenario()
                    .withPlayers("Witch", "Opponent")
                    .withCardOnBattlefield(1, "Season of the Witch")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("refusing the 2 life sacrifices it, and costs nothing") {
                    game.findPermanent("Season of the Witch").shouldBeNull()
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
