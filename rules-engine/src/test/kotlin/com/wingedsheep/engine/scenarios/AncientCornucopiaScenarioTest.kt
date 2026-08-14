package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ancient Cornucopia (BIG #16).
 *
 * Ancient Cornucopia — {2}{G} Artifact.
 *   "Whenever you cast a spell that's one or more colors, you may gain 1 life for each of that
 *    spell's colors. Do this only once each turn.
 *    {T}: Add one mana of any color."
 */
class AncientCornucopiaScenarioTest : ScenarioTestBase() {

    init {
        // A mono-green sorcery (1 color).
        val monoSpell = card("Test Green Spell") {
            manaCost = "{G}"; typeLine = "Sorcery"
            spell { effect = Effects.DrawCards(0) }
        }
        // A two-color (Selesnya) sorcery (2 colors).
        val goldSpell = card("Test Gold Spell") {
            manaCost = "{G}{W}"; typeLine = "Sorcery"
            spell { effect = Effects.DrawCards(0) }
        }
        // A colorless artifact spell (0 colors → never triggers).
        val colorlessSpell = card("Test Colorless Spell") {
            manaCost = "{1}"; typeLine = "Artifact"
        }
        cardRegistry.register(listOf(monoSpell, goldSpell, colorlessSpell))

        test("casting a 1-color spell offers to gain 1 life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ancient Cornucopia")
                .withCardInHand(1, "Test Green Spell")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .build()

            val start = game.getLifeTotal(1)
            game.castSpell(1, "Test Green Spell").error shouldBe null
            game.resolveStack() // cast trigger goes on the stack; resolving it pauses at the "may"

            val decision = game.getPendingDecision()
            withClue("a 'you may gain life' yes/no is offered") {
                (decision is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true)
            game.resolveStack()

            withClue("gained 1 life for 1 color") {
                game.getLifeTotal(1) shouldBe start + 1
            }
        }

        test("a 2-color spell gains 2 life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ancient Cornucopia")
                .withCardInHand(1, "Test Gold Spell")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .build()

            val start = game.getLifeTotal(1)
            game.castSpell(1, "Test Gold Spell").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("gained 2 life for 2 colors") {
                game.getLifeTotal(1) shouldBe start + 2
            }
        }

        test("declining the 'may' gains no life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ancient Cornucopia")
                .withCardInHand(1, "Test Green Spell")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .build()

            val start = game.getLifeTotal(1)
            game.castSpell(1, "Test Green Spell").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            withClue("declined → no life gained") {
                game.getLifeTotal(1) shouldBe start
            }
        }

        test("a colorless spell does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ancient Cornucopia")
                .withCardInHand(1, "Test Colorless Spell")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .build()

            val start = game.getLifeTotal(1)
            game.castSpell(1, "Test Colorless Spell").error shouldBe null

            withClue("no 'one or more colors' → no trigger, no decision") {
                game.hasPendingDecision() shouldBe false
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe start
        }

        test("only triggers once each turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ancient Cornucopia")
                .withCardInHand(1, "Test Green Spell")
                .withCardInHand(1, "Test Gold Spell")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .build()

            val start = game.getLifeTotal(1)
            game.castSpell(1, "Test Green Spell").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()
            withClue("first cast this turn gained 1 life") {
                game.getLifeTotal(1) shouldBe start + 1
            }

            // Second colored spell the same turn: the ability already triggered this turn.
            game.castSpell(1, "Test Gold Spell").error shouldBe null
            withClue("second colored spell the same turn does not trigger again") {
                game.hasPendingDecision() shouldBe false
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe start + 1
        }

        test("declining does not spend the turn — a later spell still offers the life") {
            // "Once you *choose* to gain life using Ancient Cornucopia's triggered ability, that
            // ability won't trigger again that turn" (Scryfall ruling; CR 603.2h). Declining is
            // not choosing, so the bigger two-color spell later in the turn is still worth
            // holding for. The old `oncePerTurn` modelling burned the turn on the decline.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ancient Cornucopia")
                .withCardInHand(1, "Test Green Spell")
                .withCardInHand(1, "Test Gold Spell")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .build()

            val start = game.getLifeTotal(1)
            game.castSpell(1, "Test Green Spell").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()
            withClue("declined the 1-color spell → no life") {
                game.getLifeTotal(1) shouldBe start
            }

            game.castSpell(1, "Test Gold Spell").error shouldBe null
            game.resolveStack()
            withClue("the ability triggers again after a decline") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true)
            game.resolveStack()
            withClue("took the 2-color spell instead") {
                game.getLifeTotal(1) shouldBe start + 2
            }
        }
    }
}
