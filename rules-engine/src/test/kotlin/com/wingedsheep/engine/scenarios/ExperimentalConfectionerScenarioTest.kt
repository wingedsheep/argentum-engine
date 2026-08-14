package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Experimental Confectioner (WOE #314) — {2}{B} Creature — Human Peasant, 2/3.
 *
 * When this creature enters, create a Food token.
 * Whenever you sacrifice a Food, create a 1/1 black Rat creature token with "This token can't block."
 *
 * These cover the new [Triggers.YouSacrificeA] vocabulary — the per-permanent "you sacrifice **a**
 * <filter>" template with an ANY binding. Two things distinguish it from the pre-existing pair:
 *  - vs [Triggers.YouSacrificeOneOrMore] (batch): three simultaneously-sacrificed Foods must make
 *    three Rats, not one (CR 603.2c).
 *  - vs [Triggers.YouSacrificeAnother] (OTHER binding): a source that is itself a Food counts its
 *    own sacrifice. The Confectioner is a Human Peasant so it can never hit that path; a Food
 *    artifact creature carrying the same trigger is registered below to prove it.
 */
class ExperimentalConfectionerScenarioTest : ScenarioTestBase() {

    // {0} sorcery sacrificing every Food you control at once — one batch, N sacrifice occurrences.
    private val cleanOutThePantry = card("Clean Out the Pantry") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all Foods you control."
        spell {
            effect = Effects.SacrificeAll(GameObjectFilter.Artifact.withSubtype("Food").youControl())
        }
    }

    // {0} sorcery sacrificing every non-Food artifact you control — the negative control.
    private val cleanOutTheWorkshop = card("Clean Out the Workshop") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Sacrifice all Clues you control."
        spell {
            effect = Effects.SacrificeAll(GameObjectFilter.Artifact.withSubtype("Clue").youControl())
        }
    }

    // A Food artifact *creature* carrying the same trigger, so the source is inside the batch.
    // "Whenever you sacrifice a Food" (ANY) counts itself; "another" (OTHER) would not.
    private val toothsomeSentry = card("Toothsome Sentry") {
        manaCost = "{0}"
        typeLine = "Artifact Creature — Food Golem"
        power = 1
        toughness = 1
        oracleText = "Whenever you sacrifice a Food, you gain 2 life."
        triggeredAbility {
            trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Food"))
            effect = Effects.GainLife(2)
        }
    }

    init {
        cardRegistry.register(cleanOutThePantry)
        cardRegistry.register(cleanOutTheWorkshop)
        cardRegistry.register(toothsomeSentry)

        context("Experimental Confectioner") {

            test("entering the battlefield creates a Food token") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Experimental Confectioner")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Experimental Confectioner").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Experimental Confectioner") shouldBe true
                withClue("the enters trigger makes exactly one Food") {
                    game.findPermanents("Food").size shouldBe 1
                }
                withClue("no Food has been sacrificed yet, so no Rats") {
                    game.findPermanents("Rat Token").size shouldBe 0
                }
            }

            test("sacrificing one Food creates one Rat") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Experimental Confectioner")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInHand(1, "Clean Out the Pantry")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clean Out the Pantry").error shouldBe null
                game.resolveStack()

                withClue("the Food is gone") {
                    game.isOnBattlefield("Food") shouldBe false
                }
                game.findPermanents("Rat Token").size shouldBe 1
            }

            // CR 603.2c: "a Food" is the per-permanent template, so one batch of three sacrifices
            // fires the ability three times. A batch trigger would produce a single Rat here.
            test("sacrificing three Foods at once creates three Rats, not one") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Experimental Confectioner")
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withCardInHand(1, "Clean Out the Pantry")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("three Foods on the battlefield to start") {
                    game.findPermanents("Food").size shouldBe 3
                }

                game.castSpell(1, "Clean Out the Pantry").error shouldBe null
                game.resolveStack()

                withClue("all three Foods went at once") {
                    game.isOnBattlefield("Food") shouldBe false
                }
                withClue("three sacrifice occurrences = three separate triggers = three Rats") {
                    game.findPermanents("Rat Token").size shouldBe 3
                }
            }

            test("sacrificing a non-Food artifact creates no Rat") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Experimental Confectioner")
                    .withCardOnBattlefield(1, "Clue", isToken = true)
                    .withCardInHand(1, "Clean Out the Workshop")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Clean Out the Workshop").error shouldBe null
                game.resolveStack()

                withClue("the Clue is gone") {
                    game.isOnBattlefield("Clue") shouldBe false
                }
                withClue("a Clue is not a Food, so the trigger never fired") {
                    game.findPermanents("Rat Token").size shouldBe 0
                }
            }
        }

        context("Triggers.YouSacrificeA binding") {

            // The ANY binding is what separates YouSacrificeA from YouSacrificeAnother: a source
            // that is itself a Food reacts to its own sacrifice.
            test("a Food source counts its own sacrifice") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Toothsome Sentry")
                    .withCardInHand(1, "Clean Out the Pantry")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpell(1, "Clean Out the Pantry").error shouldBe null
                game.resolveStack()

                withClue("the Sentry sacrificed itself (it is a Food)") {
                    game.isOnBattlefield("Toothsome Sentry") shouldBe false
                }
                withClue("'a Food' includes the source, so the trigger fired once") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 2
                }
            }
        }
    }
}
