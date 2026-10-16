package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.predicates.hasDealtDamage
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for `StatePredicate.HasDealtDamage` — the active-voice damage-history
 * predicate — in both of its windows, and for the `HasDealtDamageComponent` turn stamp behind them.
 *
 * Two halves:
 *
 *  1. **The window logic**, unit-tested against the shared `hasDealtDamage` helper that every
 *     dispatch site (PredicateEvaluator, AffectsFilterResolver, TriggerMatcher) calls.
 *  2. **The recording paths**, played out in real games. The per-turn window is only as trustworthy
 *     as the set of engine paths that stamp the marker, so each damage path gets a test asserting the
 *     stamp carries the current turn: combat damage to a player, to a creature, to a planeswalker's
 *     loyalty, a Harsh Justice reflection, and a permanent's noncombat activated-ability damage.
 *     Damage from a spell deliberately stamps nobody (the source isn't a permanent), and the marker is
 *     stripped when the permanent changes zones (CR 400.7).
 */
class HasDealtDamagePredicateTest : ScenarioTestBase() {

    private val sorcererAbilityId by lazy {
        cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id
    }

    private fun ScenarioTestBase.TestGame.marker(id: EntityId): HasDealtDamageComponent? =
        state.getEntity(id)?.get<HasDealtDamageComponent>()

    init {
        context("window logic") {

            val lifetime = StatePredicate.HasDealtDamage()
            val thisTurn = StatePredicate.HasDealtDamage(thisTurnOnly = true)

            test("no marker fails both windows") {
                val bare = ComponentContainer()
                hasDealtDamage(bare, currentTurn = 5, predicate = lifetime) shouldBe false
                hasDealtDamage(bare, currentTurn = 5, predicate = thisTurn) shouldBe false
            }

            test("a marker stamped this turn satisfies both windows") {
                val marked = ComponentContainer().with(HasDealtDamageComponent(5))
                hasDealtDamage(marked, currentTurn = 5, predicate = lifetime) shouldBe true
                hasDealtDamage(marked, currentTurn = 5, predicate = thisTurn) shouldBe true
            }

            test("a marker stamped on an earlier turn satisfies only the lifetime window") {
                val marked = ComponentContainer().with(HasDealtDamageComponent(3))
                hasDealtDamage(marked, currentTurn = 5, predicate = lifetime) shouldBe true
                withClue("The per-turn window expires on its own — nothing clears the marker") {
                    hasDealtDamage(marked, currentTurn = 5, predicate = thisTurn) shouldBe false
                }
            }

            val combatLifetime = StatePredicate.HasDealtDamage(combatOnly = true)
            val combatThisTurn = StatePredicate.HasDealtDamage(thisTurnOnly = true, combatOnly = true)

            test("combatOnly ignores a marker that only ever recorded noncombat damage") {
                val pinged = ComponentContainer().with(HasDealtDamageComponent(5))
                hasDealtDamage(pinged, currentTurn = 5, predicate = lifetime) shouldBe true
                hasDealtDamage(pinged, currentTurn = 5, predicate = combatLifetime) shouldBe false
                hasDealtDamage(pinged, currentTurn = 5, predicate = combatThisTurn) shouldBe false
            }

            test("combatOnly reads the combat stamp for both windows") {
                val marked = ComponentContainer().with(
                    HasDealtDamageComponent(lastDealtDamageTurn = 5, lastDealtCombatDamageTurn = 3)
                )
                hasDealtDamage(marked, currentTurn = 5, predicate = combatLifetime) shouldBe true
                withClue("A noncombat stamp this turn doesn't make the combat damage this turn's") {
                    hasDealtDamage(marked, currentTurn = 5, predicate = combatThisTurn) shouldBe false
                }
                hasDealtDamage(marked, currentTurn = 3, predicate = combatThisTurn) shouldBe true
            }

            test("the passive marker never satisfies the active predicate") {
                val damaged = ComponentContainer().with(WasDealtDamageThisTurnComponent(noncombat = true))
                hasDealtDamage(damaged, currentTurn = 5, predicate = lifetime) shouldBe false
                hasDealtDamage(damaged, currentTurn = 5, predicate = thisTurn) shouldBe false
            }
        }

        context("recording paths") {

            test("combat damage to a player stamps the attacker with the current turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Nothing has dealt damage before combat") {
                    game.marker(bears) shouldBe null
                }

                // Captured before the action, so the assertion doesn't read its expected value out
                // of the same state it is checking.
                val damageTurn = game.state.turnNumber
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 18
                game.marker(bears) shouldBe HasDealtDamageComponent(damageTurn, lastDealtCombatDamageTurn = damageTurn)
            }

            test("combat damage to a creature stamps the blocker that dealt it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                val damageTurn = game.state.turnNumber
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("The blocker dealt combat damage to the attacker") {
                    game.marker(giant) shouldBe HasDealtDamageComponent(damageTurn, lastDealtCombatDamageTurn = damageTurn)
                }
                withClue("The attacker died, so its own stamp went with it (CR 400.7)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("combat damage to a planeswalker stamps the attacker") {
                // The loyalty/defense recipient goes through `removeCountersForDamage`, a different
                // stamp site from the player and creature branches (CR 120.3c).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Sorin, Solemn Visitor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Seed loyalty so a state-based action doesn't bin the planeswalker before combat.
                val sorin = game.findPermanent("Sorin, Solemn Visitor")!!
                game.state = game.state.updateEntity(sorin) { container ->
                    container.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
                }
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val damageTurn = game.state.turnNumber
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Grizzly Bears" to "Sorin, Solemn Visitor")
                ).error shouldBe null
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("2 damage removed 2 loyalty counters") {
                    game.state.getEntity(sorin)?.get<CountersComponent>()
                        ?.getCount(CounterType.LOYALTY) shouldBe 2
                }
                withClue("The attacker dealt that damage, so it carries this turn's stamp") {
                    game.marker(bears) shouldBe HasDealtDamageComponent(damageTurn, lastDealtCombatDamageTurn = damageTurn)
                }
            }

            test("a reflected hit (Harsh Justice) leaves the attacker stamped") {
                // Harsh Justice makes the attacking creature deal its damage to its own controller
                // as well (CR 120.1 — the creature is the source of both). The reflection runs at the
                // tail of the damage-to-player path, so this pins the whole path end to end.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(2, "Harsh Justice")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val damageTurn = game.state.turnNumber

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                // Harsh Justice is castable only during the declare attackers step by a player who
                // was attacked, so the active player has to pass first.
                game.passPriority().error shouldBe null
                withClue("The defending player has priority to cast Harsh Justice") {
                    game.state.priorityPlayerId shouldBe game.player2Id
                }
                game.castSpell(2, "Harsh Justice").error shouldBe null
                game.resolveStack()

                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("The defending player took the Bears' 2") { game.getLifeTotal(2) shouldBe 18 }
                withClue("...and the reflection sent 2 back at the attacking player") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("The attacker dealt damage this turn — to two players, in fact") {
                    game.marker(bears) shouldBe HasDealtDamageComponent(damageTurn, lastDealtCombatDamageTurn = damageTurn)
                }
            }

            test("a permanent's activated-ability damage stamps it, and a spell's damage stamps nobody") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val giant = game.findPermanent("Hill Giant")!!
                val damageTurn = game.state.turnNumber

                // A spell's damage first: the source is on the stack, never the battlefield, so no
                // permanent is recorded as having dealt it.
                game.castSpell(1, "Shock", giant).error shouldBe null
                game.resolveStack()
                withClue("Shock's recipient is not a damage *dealer*") {
                    game.marker(giant) shouldBe null
                }
                withClue("The Sorcerer sat this one out") {
                    game.marker(sorcerer) shouldBe null
                }

                val ping = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = sorcererAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                )
                withClue("Prodigal Sorcerer pings the Giant: ${ping.error}") { ping.error shouldBe null }
                game.resolveStack()

                withClue("Noncombat damage from a permanent stamps that permanent") {
                    game.marker(sorcerer) shouldBe HasDealtDamageComponent(damageTurn)
                }
            }

            test("later noncombat damage keeps the earlier combat stamp") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val giant = game.findPermanent("Hill Giant")!!
                val damageTurn = game.state.turnNumber
                game.declareAttackers(mapOf("Prodigal Sorcerer" to 2)).error shouldBe null
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(2) shouldBe 19

                // Prodigal Sorcerer is tapped from attacking; untap it to ping.
                game.state = game.state.updateEntity(sorcerer) {
                    it.without<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()
                }
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = sorcererAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("The ping restamps the any-damage turn and carries the combat stamp forward") {
                    game.marker(sorcerer) shouldBe
                        HasDealtDamageComponent(damageTurn, lastDealtCombatDamageTurn = damageTurn)
                }
            }

            test("the stamp keeps the lifetime window open across a turn boundary but closes the per-turn one") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val damageTurn = game.state.turnNumber
                game.marker(bears) shouldBe HasDealtDamageComponent(damageTurn, lastDealtCombatDamageTurn = damageTurn)

                // Roll into the opponent's turn.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val laterTurn = game.state.turnNumber
                withClue("A turn boundary was actually crossed") { (laterTurn > damageTurn) shouldBe true }

                val container = game.state.getEntity(bears)!!
                withClue("Nothing cleared the marker") {
                    hasDealtDamage(container, laterTurn, StatePredicate.HasDealtDamage()) shouldBe true
                }
                withClue("...but the per-turn window closed with the turn") {
                    hasDealtDamage(
                        container, laterTurn, StatePredicate.HasDealtDamage(thisTurnOnly = true)
                    ) shouldBe false
                }
            }

            test("leaving the battlefield clears the marker (CR 400.7)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Unsummon")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val giant = game.findPermanent("Hill Giant")!!
                val damageTurn = game.state.turnNumber
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = sorcererAbilityId,
                        targets = listOf(ChosenTarget.Permanent(giant))
                    )
                ).error shouldBe null
                game.resolveStack()
                game.marker(sorcerer) shouldBe HasDealtDamageComponent(damageTurn)

                game.castSpell(1, "Unsummon", sorcerer).error shouldBe null
                game.resolveStack()

                withClue("The Sorcerer is back in hand, and its damage history did not follow it") {
                    game.isOnBattlefield("Prodigal Sorcerer") shouldBe false
                }
                withClue("The old battlefield entity carries no marker") {
                    game.marker(sorcerer) shouldBe null
                }
            }
        }
    }
}
