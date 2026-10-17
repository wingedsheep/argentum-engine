package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.ForgeBoss
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Forge Boss (SNC #189) — {2}{B}{R} 3/4 Human Warrior.
 * "Whenever you sacrifice one or more other creatures, this creature deals 2 damage to each
 * opponent. This ability triggers only once each turn."
 *
 * The batch + OTHER sacrifice trigger (`Triggers.you.sacrificesAnother(filter, batch = true)`): once
 * per batch however many creatures go, never for Forge Boss alone, but still when Forge Boss is
 * sacrificed in the same batch as other creatures (it looks back at the whole batch).
 */
class ForgeBossScenarioTest : ScenarioTestBase() {

    private val sacrificePermanent = card("Sacrifice Permanent") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice target permanent you control."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.SacrificeTarget(t)
        }
    }

    private val cullTheRats = card("Cull the Rats") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all Rats you control."
        spell { effect = Effects.SacrificeAll(GameObjectFilter.Creature.withSubtype("Rat").youControl()) }
    }

    // Forge Boss is a Warrior, so this sacrifices it together with the Warrior fodder.
    private val disbandTheWarriors = card("Disband the Warriors") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all Warriors you control."
        spell { effect = Effects.SacrificeAll(GameObjectFilter.Creature.withSubtype("Warrior").youControl()) }
    }

    private val ratFodder = card("Sewer Rat") {
        manaCost = "{0}"
        typeLine = "Creature — Rat"
        power = 1
        toughness = 1
    }

    private val warriorFodder = card("Street Tough") {
        manaCost = "{0}"
        typeLine = "Creature — Human Warrior"
        power = 1
        toughness = 1
    }

    private val trinket = card("Shiny Trinket") {
        manaCost = "{0}"
        typeLine = "Artifact"
    }

    init {
        cardRegistry.register(ForgeBoss)
        cardRegistry.register(sacrificePermanent)
        cardRegistry.register(cullTheRats)
        cardRegistry.register(disbandTheWarriors)
        cardRegistry.register(ratFodder)
        cardRegistry.register(warriorFodder)
        cardRegistry.register(trinket)

        fun base() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Forge Boss", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Forge Boss") {

            test("sacrificing another creature deals 2 damage to each opponent") {
                val game = base()
                    .withCardOnBattlefield(1, "Sewer Rat")
                    .withCardInHand(1, "Sacrifice Permanent")
                    .build()

                game.castSpell(1, "Sacrifice Permanent", game.findPermanent("Sewer Rat")!!).error shouldBe null
                game.resolveStack()

                withClue("one other creature sacrificed → 2 damage") { game.getLifeTotal(2) shouldBe 18 }
                game.getLifeTotal(1) shouldBe 20
            }

            test("three creatures sacrificed at once trigger it only once") {
                val game = base()
                    .withCardOnBattlefield(1, "Sewer Rat")
                    .withCardOnBattlefield(1, "Sewer Rat")
                    .withCardOnBattlefield(1, "Sewer Rat")
                    .withCardInHand(1, "Cull the Rats")
                    .build()

                game.castSpell(1, "Cull the Rats").error shouldBe null
                game.resolveStack()

                withClue("one batch of three → one trigger") { game.getLifeTotal(2) shouldBe 18 }
            }

            test("sacrificing a noncreature permanent doesn't trigger it") {
                val game = base()
                    .withCardOnBattlefield(1, "Shiny Trinket")
                    .withCardInHand(1, "Sacrifice Permanent")
                    .build()

                game.castSpell(1, "Sacrifice Permanent", game.findPermanent("Shiny Trinket")!!).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("sacrificing Forge Boss alone doesn't trigger it") {
                val game = base()
                    .withCardInHand(1, "Sacrifice Permanent")
                    .build()

                game.castSpell(1, "Sacrifice Permanent", game.findPermanent("Forge Boss")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Forge Boss") shouldBe false
                withClue("\"other\" excludes Forge Boss itself") { game.getLifeTotal(2) shouldBe 20 }
            }

            test("Forge Boss sacrificed together with other creatures still triggers once") {
                val game = base()
                    .withCardOnBattlefield(1, "Street Tough")
                    .withCardOnBattlefield(1, "Street Tough")
                    .withCardInHand(1, "Disband the Warriors")
                    .build()

                game.castSpell(1, "Disband the Warriors").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Forge Boss") shouldBe false
                withClue("it looks back at the batch it left with → one trigger") { game.getLifeTotal(2) shouldBe 18 }
            }

            test("it triggers only once each turn") {
                val game = base()
                    .withCardOnBattlefield(1, "Sewer Rat")
                    .withCardOnBattlefield(1, "Street Tough")
                    .withCardInHand(1, "Sacrifice Permanent")
                    .withCardInHand(1, "Sacrifice Permanent")
                    .build()

                game.castSpell(1, "Sacrifice Permanent", game.findPermanent("Sewer Rat")!!).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Sacrifice Permanent", game.findPermanent("Street Tough")!!).error shouldBe null
                game.resolveStack()

                withClue("second sacrifice this turn doesn't trigger again") { game.getLifeTotal(2) shouldBe 18 }
            }
        }
    }
}
