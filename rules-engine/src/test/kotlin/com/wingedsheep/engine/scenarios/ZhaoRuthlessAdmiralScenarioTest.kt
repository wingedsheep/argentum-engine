package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.tla.cards.ZhaoRuthlessAdmiral
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Zhao, Ruthless Admiral (TLA #252) — {2}{B/R}{B/R} Legendary Creature —
 * Human Soldier, 3/4.
 *
 * - Firebending 2 (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)
 * - Whenever you sacrifice another permanent, creatures you control get +1/+0 until end of turn.
 *
 * Firebending has no engine handler — the printed keyword is a display tag plus an attack-triggered
 * combat-duration "add {R}{R}" effect — so the firebending test asserts the *behavior* (attacking
 * produces two red combat-duration mana). The sacrifice payoff is the per-permanent
 * "you sacrifice another permanent" trigger (OTHER binding, `Triggers.YouSacrificeAnother`): the
 * Mazirek/Savra template that fires once for EACH permanent sacrificed even when several are
 * sacrificed simultaneously (CR 603.2c), so three simultaneous sacrifices pump the team +3/+0. It
 * is driven here with a {0} "Sacrifice target permanent" sorcery (single) and a {0} "Sacrifice all
 * <subtype>" sorcery (`Effects.SacrificeAll`, a genuine simultaneous batch).
 */
class ZhaoRuthlessAdmiralScenarioTest : ScenarioTestBase() {

    // {0} sorcery that sacrifices a target permanent you control — a clean single-sacrifice outlet.
    private val sacrificePermanent = card("Sacrifice Permanent") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice target permanent you control."
        spell {
            val t = target("target permanent you control", Targets.Permanent)
            effect = Effects.SacrificeTarget(t)
        }
    }

    // {0} sorcery that sacrifices all Rats you control simultaneously (one batch) — the outlet for
    // the per-permanent multiplicity test (Zhao is a Human Soldier, so it survives and is pumped).
    private val cullTheRats = card("Cull the Rats") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all Rats you control."
        spell { effect = Effects.SacrificeAll(GameObjectFilter.Creature.withSubtype("Rat").youControl()) }
    }

    // {0} sorcery that sacrifices all Soldiers you control simultaneously — Zhao is a Soldier, so
    // it is sacrificed *alongside* its fodder, exercising the "another" (OTHER) self-in-batch path.
    private val draftTheSoldiers = card("Draft the Soldiers") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all Soldiers you control."
        spell { effect = Effects.SacrificeAll(GameObjectFilter.Creature.withSubtype("Soldier").youControl()) }
    }

    // 1/1 vanilla fodder with a shared subtype so a single SacrificeAll removes several at once.
    private val ratFodder = card("Sewer Rat") {
        manaCost = "{0}"
        typeLine = "Creature — Rat"
        power = 1
        toughness = 1
    }
    private val soldierFodder = card("Raw Recruit") {
        manaCost = "{0}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(ZhaoRuthlessAdmiral)
        cardRegistry.register(sacrificePermanent)
        cardRegistry.register(cullTheRats)
        cardRegistry.register(draftTheSoldiers)
        cardRegistry.register(ratFodder)
        cardRegistry.register(soldierFodder)

        context("Zhao, Ruthless Admiral") {

            test("firebending 2: attacking with Zhao adds {R}{R} combat-duration mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zhao, Ruthless Admiral", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Zhao, Ruthless Admiral" to 2)).error shouldBe null
                game.resolveStack()

                val combatMana = game.state.getEntity(game.player1Id)
                    ?.get<ManaPoolComponent>()
                    ?.restrictedMana
                    ?.filter { it.expiry == ManaExpiry.END_OF_COMBAT }
                    ?: emptyList()

                withClue("firebending 2 adds two red combat-duration mana on attack") {
                    combatMana.size shouldBe 2
                    combatMana.all { it.color == Color.RED } shouldBe true
                }
            }

            test("sacrificing another permanent gives creatures you control +1/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zhao, Ruthless Admiral", summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false) // 3/3 pump target
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)    // 1/1 fodder
                    .withCardInHand(1, "Sacrifice Permanent")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zhao = game.findPermanent("Zhao, Ruthless Admiral")!!
                val ally = game.findPermanent("Centaur Courser")!!
                val fodder = game.findPermanent("Savannah Lions")!!

                withClue("baseline power before any sacrifice") {
                    game.state.projectedState.getPower(zhao) shouldBe 3
                    game.state.projectedState.getPower(ally) shouldBe 3
                }

                // Sacrifice the Savannah Lions (another permanent), firing Zhao's trigger.
                game.castSpell(1, "Sacrifice Permanent", fodder).error shouldBe null
                game.resolveStack()

                withClue("the fodder is gone") {
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
                withClue("every creature you control gets +1/+0 until end of turn (toughness unchanged)") {
                    game.state.projectedState.getPower(zhao) shouldBe 4
                    game.state.projectedState.getToughness(zhao) shouldBe 4
                    game.state.projectedState.getPower(ally) shouldBe 4
                    game.state.projectedState.getToughness(ally) shouldBe 3
                }
            }

            // CR 603.2c: "another permanent" fires once per permanent sacrificed, even simultaneously.
            test("sacrificing three permanents at once pumps the team +3/+0, not +1/+0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zhao, Ruthless Admiral", summoningSickness = false)
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false) // 3/3 survivor
                    .withCardOnBattlefield(1, "Sewer Rat", summoningSickness = false)
                    .withCardOnBattlefield(1, "Sewer Rat", summoningSickness = false)
                    .withCardOnBattlefield(1, "Sewer Rat", summoningSickness = false)
                    .withCardInHand(1, "Cull the Rats")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zhao = game.findPermanent("Zhao, Ruthless Admiral")!!
                val ally = game.findPermanent("Centaur Courser")!!

                // Sacrifice all three Rats simultaneously — one batch, three sacrifice occurrences.
                game.castSpell(1, "Cull the Rats").error shouldBe null
                game.resolveStack()

                withClue("all three Rats are gone") {
                    game.isOnBattlefield("Sewer Rat") shouldBe false
                }
                withClue("three simultaneous sacrifices = three +1/+0 triggers = +3/+0 on survivors") {
                    game.state.projectedState.getPower(zhao) shouldBe 6
                    game.state.projectedState.getToughness(zhao) shouldBe 4
                    game.state.projectedState.getPower(ally) shouldBe 6
                    game.state.projectedState.getToughness(ally) shouldBe 3
                }
            }

            // "Another" excludes the source: Zhao sacrificed alongside two other Soldiers reacts to
            // the two others (fires twice), not to itself.
            test("Zhao sacrificed with two others fires for the others only (+2/+0), excluding itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zhao, Ruthless Admiral", summoningSickness = false) // Soldier
                    .withCardOnBattlefield(1, "Raw Recruit", summoningSickness = false) // Soldier fodder
                    .withCardOnBattlefield(1, "Raw Recruit", summoningSickness = false) // Soldier fodder
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2 non-Soldier survivor
                    .withCardInHand(1, "Draft the Soldiers")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val survivor = game.findPermanent("Grizzly Bears")!!

                // Sacrifice Zhao + both Raw Recruits simultaneously (all Soldiers).
                game.castSpell(1, "Draft the Soldiers").error shouldBe null
                game.resolveStack()

                withClue("Zhao sacrificed itself along with the fodder") {
                    game.isOnBattlefield("Zhao, Ruthless Admiral") shouldBe false
                }
                withClue("Zhao's 'another' trigger fired for the two OTHER Soldiers only: +2/+0") {
                    game.state.projectedState.getPower(survivor) shouldBe 4
                    game.state.projectedState.getToughness(survivor) shouldBe 2
                }
            }
        }
    }
}
