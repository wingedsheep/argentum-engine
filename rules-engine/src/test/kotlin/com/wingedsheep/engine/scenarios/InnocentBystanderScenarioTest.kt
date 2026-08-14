package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.InnocentBystander
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Innocent Bystander (MKM) — "Whenever this creature is dealt 3 or more damage, investigate."
 *
 * The threshold is measured **per damage event** (the printed ruling: "all at once … not if damage
 * that adds up to 3 or more is dealt to it at different times"), so these cover both sides of that
 * line: one instance of 3 fires it, two instances of 2 do not, and — because the Bystander is a 2/1
 * — a lethal instance still fires, since the trigger is detected off the damage event before the
 * state-based action that kills it has any say (CR 603.10).
 */
class InnocentBystanderScenarioTest : ScenarioTestBase() {

    /** Free burn spells so the only variable is the size of a single damage instance. */
    private val ping = card("Ping Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Ping Test deals 2 damage to target creature."
        spell {
            val t = target("target creature", TargetCreature())
            effect = Effects.DealDamage(2, t)
        }
    }

    private val bolt = card("Bolt Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Bolt Test deals 3 damage to target creature."
        spell {
            val t = target("target creature", TargetCreature())
            effect = Effects.DealDamage(3, t)
        }
    }

    /** Toughness so the 2/1 survives to be pinged twice — the "adds up" case needs a live target. */
    private val fortify = card("Fortify Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Target creature gets +0/+8 until end of turn."
        spell {
            val t = target("target creature", TargetCreature())
            effect = Effects.ModifyStats(0, 8, t)
        }
    }

    private fun game(vararg spells: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Innocent Bystander", summoningSickness = false)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        spells.forEach { builder.withCardInHand(1, it) }
        return builder.build()
    }

    init {
        cardRegistry.register(InnocentBystander)
        cardRegistry.register(ping)
        cardRegistry.register(bolt)
        cardRegistry.register(fortify)

        context("Innocent Bystander") {

            test("a single 3-damage instance investigates") {
                val game = game("Bolt Test")
                val bystander = game.findPermanent("Innocent Bystander")!!
                game.findPermanents("Clue").size shouldBe 0

                game.castSpell(1, "Bolt Test", targetId = bystander).error shouldBe null
                game.resolveStack()

                withClue("3 >= 3 — the trigger fires even though the 2/1 dies to the same damage") {
                    game.findPermanents("Clue").size shouldBe 1
                }
                withClue("lethal damage still killed it; the Clue survives it") {
                    game.isOnBattlefield("Innocent Bystander") shouldBe false
                }
            }

            test("two separate 2-damage instances never investigate, even though they total 4") {
                // The Bystander is propped up to a 2/9 first so it survives both pings; without
                // that the second instance would never land on a live creature.
                val game = game("Fortify Test", "Ping Test", "Ping Test")
                val bystander = game.findPermanent("Innocent Bystander")!!
                game.castSpell(1, "Fortify Test", targetId = bystander).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Ping Test", targetId = bystander).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Ping Test", targetId = bystander).error shouldBe null
                game.resolveStack()

                withClue("4 total damage, but no single instance reached 3 — the ruling's case") {
                    game.isOnBattlefield("Innocent Bystander") shouldBe true
                    game.findPermanents("Clue").size shouldBe 0
                }
            }
        }
    }
}
