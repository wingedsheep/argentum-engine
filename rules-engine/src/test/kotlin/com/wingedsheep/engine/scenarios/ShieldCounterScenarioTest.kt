package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feature test for **shield counters** (CR 122.1c).
 *
 * > One or more shield counters on a permanent create a single replacement effect and a single
 * > prevention effect that protect the permanent. These effects are "If this permanent would be
 * > destroyed as the result of an effect, instead remove a shield counter from it" and "If damage
 * > would be dealt to this permanent, prevent that damage and remove a shield counter from it."
 *
 * Each test pins one clause of that rule or one of the official rulings:
 *
 * - one counter consumed per damage event, whatever the damage amount and however many counters;
 * - destruction *by an effect* replaced, single-target and board-wipe alike;
 * - combat damage covered, and covered **once** for the whole simultaneous batch (CR 510.2);
 * - *not* regeneration — no tap, and the counter is spent instead of a regeneration shield;
 * - sacrifice unaffected;
 * - indestructible leaves the counter unspent (nothing "would be destroyed", CR 702.12b);
 * - unpreventable damage is dealt **and** still removes a counter.
 *
 * Backs Captain America, Super-Soldier [MSH 9].
 */
class ShieldCounterScenarioTest : ScenarioTestBase() {

    // Vanilla bodies defined here rather than pulled from the shared catalog so the damage maths
    // below can't drift with someone else's stub P/T.
    private val bruiser = card("Shield Test Bruiser") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 3
        toughness = 3
    }

    private val bulwark = card("Shield Test Bulwark") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 3
        toughness = 3
        keywords(Keyword.INDESTRUCTIBLE)
    }

    private val biter = card("Shield Test Biter") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin Warrior"
        power = 2
        toughness = 2
    }

    private val snapper = card("Shield Test Snapper") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin Rogue"
        power = 2
        toughness = 2
    }

    // Deals damage in both combat damage steps (CR 702.4b), which is the cheapest way to put two
    // separate damage events on one blocked attacker without a damage-assignment decision.
    private val duelist = card("Shield Test Duelist") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Human Warrior"
        power = 1
        toughness = 1
        keywords(Keyword.DOUBLE_STRIKE)
    }

    // {0} utility spells so the tests never fight the mana system.
    private val shieldUp = card("Shield Up") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Put a shield counter on target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.AddCounters(Counters.SHIELD, 1, t)
        }
    }

    private val zap = card("Shield Test Zap") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Shield Test Zap deals 3 damage to target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.DealDamage(3, t)
        }
    }

    private val slay = card("Shield Test Slay") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.Destroy(t)
        }
    }

    private val wipe = card("Shield Test Wipe") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures."
        spell {
            effect = Effects.DestroyAll(Filters.Creature)
        }
    }

    private val feed = card("Shield Test Feed") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Target creature's controller sacrifices it."
        spell {
            val t = target("target creature", Targets.Creature)
            effect = Effects.SacrificeTarget(t)
        }
    }

    private val noPrevention = card("Shield Test No Prevention") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Damage can't be prevented this turn."
        spell {
            effect = Effects.DamageCantBePreventedThisTurn()
        }
    }

    private fun shieldCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) ?: 0

    private fun markedDamage(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    /** Put [count] shield counters on [id] by resolving that many copies of Shield Up. */
    private fun shield(game: TestGame, id: EntityId, count: Int = 1) {
        repeat(count) {
            game.castSpell(1, "Shield Up", id).error shouldBe null
            game.resolveStack()
        }
    }

    init {
        cardRegistry.register(bruiser)
        cardRegistry.register(bulwark)
        cardRegistry.register(biter)
        cardRegistry.register(snapper)
        cardRegistry.register(duelist)
        cardRegistry.register(shieldUp)
        cardRegistry.register(zap)
        cardRegistry.register(slay)
        cardRegistry.register(wipe)
        cardRegistry.register(feed)
        cardRegistry.register(noPrevention)

        context("shield counters — damage prevention (CR 122.1c)") {

            test("damage to a shielded creature is prevented and consumes exactly one counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test Zap")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Shield Test Bruiser")!!
                shield(game, creature)
                shieldCounters(game, creature) shouldBe 1

                game.castSpell(1, "Shield Test Zap", creature).error shouldBe null
                game.resolveStack()

                withClue("3 damage to a 3/3 is fully prevented — the creature survives") {
                    game.findPermanent("Shield Test Bruiser") shouldBe creature
                }
                withClue("no damage is marked; the damage was prevented, not absorbed") {
                    markedDamage(game, creature) shouldBe 0
                }
                withClue("exactly one shield counter is consumed") {
                    shieldCounters(game, creature) shouldBe 0
                }
            }

            test("with two shield counters only one is removed per damage event") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test Zap")
                    .withCardInHand(1, "Shield Test Zap")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Shield Test Bruiser")!!
                shield(game, creature, count = 2)
                shieldCounters(game, creature) shouldBe 2

                game.castSpell(1, "Shield Test Zap", creature).error shouldBe null
                game.resolveStack()
                withClue("one damage event removes one counter, not all of them") {
                    shieldCounters(game, creature) shouldBe 1
                }

                game.castSpell(1, "Shield Test Zap", creature).error shouldBe null
                game.resolveStack()
                withClue("the second damage event spends the second counter") {
                    shieldCounters(game, creature) shouldBe 0
                }
                withClue("the creature survived both") {
                    game.findPermanent("Shield Test Bruiser") shouldBe creature
                }
            }

            test("unpreventable damage is still dealt and still removes a shield counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test No Prevention")
                    .withCardInHand(1, "Shield Test Zap")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Shield Test Bruiser")!!
                shield(game, creature)
                game.castSpell(1, "Shield Test No Prevention").error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Shield Test Zap", creature).error shouldBe null
                game.resolveStack()

                withClue("3 unpreventable damage kills the 3/3 through the lethal-damage SBA") {
                    game.findPermanent("Shield Test Bruiser") shouldBe null
                    game.isInGraveyard(1, "Shield Test Bruiser") shouldBe true
                }
            }
        }

        context("shield counters — destruction replacement (CR 122.1c)") {

            test("a destroy effect is replaced by removing a shield counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test Slay")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Shield Test Bruiser")!!
                shield(game, creature)

                game.castSpell(1, "Shield Test Slay", creature).error shouldBe null
                game.resolveStack()

                withClue("the creature survives the destroy effect") {
                    game.findPermanent("Shield Test Bruiser") shouldBe creature
                }
                withClue("one shield counter paid for it") {
                    shieldCounters(game, creature) shouldBe 0
                }
                withClue("this is not regeneration — the creature is not tapped") {
                    game.state.getEntity(creature)?.has<TappedComponent>() shouldBe false
                }
            }

            test("a board wipe spends one counter per shielded permanent and kills the rest") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Shield Test Biter", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test Wipe")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shielded = game.findPermanent("Shield Test Bruiser")!!
                shield(game, shielded)

                game.castSpell(1, "Shield Test Wipe").error shouldBe null
                game.resolveStack()

                withClue("the shielded creature survives the wipe with its counter spent") {
                    game.findPermanent("Shield Test Bruiser") shouldBe shielded
                    shieldCounters(game, shielded) shouldBe 0
                }
                withClue("the unshielded creature dies") {
                    game.findPermanent("Shield Test Biter") shouldBe null
                }
            }

            test("an indestructible permanent keeps its shield counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bulwark", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test Slay")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Shield Test Bulwark")!!
                shield(game, creature)

                game.castSpell(1, "Shield Test Slay", creature).error shouldBe null
                game.resolveStack()

                withClue(
                    "an indestructible permanent is never going to be destroyed (CR 702.12b), so the " +
                        "shield counter has nothing to replace and stays put"
                ) {
                    game.findPermanent("Shield Test Bulwark") shouldBe creature
                    shieldCounters(game, creature) shouldBe 1
                }
            }

            test("shield counters do not stop a sacrifice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Test Feed")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Shield Test Bruiser")!!
                shield(game, creature)

                game.castSpell(1, "Shield Test Feed", creature).error shouldBe null
                game.resolveStack()

                withClue("sacrifice is not destruction — the shield counter does not save it") {
                    game.findPermanent("Shield Test Bruiser") shouldBe null
                    game.isInGraveyard(1, "Shield Test Bruiser") shouldBe true
                }
            }
        }

        context("shield counters — combat damage (CR 510.2)") {

            test("combat damage to a shielded attacker is prevented for one counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Shield Test Biter", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attacker = game.findPermanent("Shield Test Bruiser")!!
                shield(game, attacker)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shield Test Bruiser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Shield Test Biter" to listOf("Shield Test Bruiser")))
                    .error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the shielded attacker takes no combat damage") {
                    markedDamage(game, attacker) shouldBe 0
                }
                withClue("one shield counter paid for the combat damage event") {
                    shieldCounters(game, attacker) shouldBe 0
                }
                withClue("its own damage still killed the 2/2 blocker") {
                    game.findPermanent("Shield Test Biter") shouldBe null
                }
            }

            test("a shielded attacker blocked by two creatures still spends only one counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Shield Test Biter", summoningSickness = false)
                    .withCardOnBattlefield(2, "Shield Test Snapper", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Up")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attacker = game.findPermanent("Shield Test Bruiser")!!
                shield(game, attacker, count = 2)
                shieldCounters(game, attacker) shouldBe 2

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shield Test Bruiser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Shield Test Biter" to listOf("Shield Test Bruiser"),
                        "Shield Test Snapper" to listOf("Shield Test Bruiser"),
                    )
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                // Two blockers ⇒ the attacker's controller orders/assigns its damage.
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                }
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue(
                    "all combat damage in a step is dealt as one event (CR 510.2), so being blocked " +
                        "by two creatures still costs exactly one shield counter"
                ) {
                    shieldCounters(game, attacker) shouldBe 1
                }
                withClue("and none of the 4 damage was marked") {
                    markedDamage(game, attacker) shouldBe 0
                }
            }

            test("the two combat damage steps are separate events and cost a counter each") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shield Test Bruiser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Shield Test Duelist", summoningSickness = false)
                    .withCardInHand(1, "Shield Up")
                    .withCardInHand(1, "Shield Up")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attacker = game.findPermanent("Shield Test Bruiser")!!
                shield(game, attacker, count = 2)
                shieldCounters(game, attacker) shouldBe 2

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Shield Test Bruiser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Shield Test Duelist" to listOf("Shield Test Bruiser")))
                    .error shouldBe null

                // Combat damage is dealt as the turn-based action on entering each damage step, so
                // by the time we are *in* a step its damage has already been applied.
                game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)
                withClue(
                    "the double striker's first-strike hit is its own damage event (CR 510.4), so " +
                        "exactly one counter is gone after the first step — not two, not none"
                ) {
                    shieldCounters(game, attacker) shouldBe 1
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("the regular combat damage step is a second event and spends the second") {
                    shieldCounters(game, attacker) shouldBe 0
                }
                withClue("both hits were prevented, so nothing is marked and the attacker lives") {
                    markedDamage(game, attacker) shouldBe 0
                    game.findPermanent("Shield Test Bruiser") shouldBe attacker
                }
                withClue("the 1/1 duelist still died to the attacker's 3 damage") {
                    game.findPermanent("Shield Test Duelist") shouldBe null
                }
            }
        }
    }
}
