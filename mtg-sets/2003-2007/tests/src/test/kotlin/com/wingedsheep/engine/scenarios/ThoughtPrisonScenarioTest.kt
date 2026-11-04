package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.ThoughtPrison
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thought Prison (MRD #261) — "Imprint — When this artifact enters, you may have target player
 * reveal their hand. If you do, choose a nonland card from it and exile that card. Whenever a
 * player casts a spell that shares a color or mana value with the exiled card, this artifact deals
 * 2 damage to that player."
 *
 * The imprint here is always **Counterspell** ({U}{U} — blue, mana value 2), which makes the three
 * branches of "shares a color **or** mana value" separable:
 *  - Phantom Warrior ({1}{U}{U}) shares the colour but not the mana value,
 *  - Artifact Creature ({2}) shares the mana value but not the colour (it has none),
 *  - Savannah Lions ({W}) shares neither.
 * The fourth test covers the declined imprint, where the reference resolves to nothing and the
 * trigger must stay silent rather than firing on everything.
 */
class ThoughtPrisonScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ThoughtPrison)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Work through whatever decisions are pending, passing priority only while there is still
     * something on the stack — so the helper never walks the game out of the main phase and leaves
     * a later `castSpell` illegal for want of a timing window.
     */
    fun GameTestDriver.settle(accept: Boolean, pick: List<com.wingedsheep.sdk.model.EntityId> = emptyList()) {
        repeat(20) {
            when (val decision = state.pendingDecision) {
                null -> if (state.stack.isNotEmpty()) bothPass() else return
                is ChooseTargetsDecision -> submitTargetSelection(player1, listOf(player2)).error shouldBe null
                is YesNoDecision -> submitYesNo(player1, accept).error shouldBe null
                is SelectCardsDecision -> submitCardSelection(player1, pick).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    /**
     * Cast Thought Prison, aim the imprint at player 2, and (when [accept]) exile the Counterspell
     * planted in their hand. Player 2's opening hand is all Swamps, so Counterspell is the only
     * nonland card the choice can land on.
     */
    fun GameTestDriver.castPrison(accept: Boolean) {
        val counterspell = putCardInHand(player2, "Counterspell")
        val inHand = putCardInHand(player1, "Thought Prison")
        giveColorlessMana(player1, 5)
        castSpell(player1, inHand).error shouldBe null
        settle(accept = accept, pick = listOf(counterspell))
    }

    /** Player 1 casts [name], then the stack settles (including any Thought Prison trigger). */
    fun GameTestDriver.castCreature(name: String, blue: Int, colorless: Int) {
        val inHand = putCardInHand(player1, name)
        if (blue > 0) giveMana(player1, Color.BLUE, blue)
        if (colorless > 0) giveColorlessMana(player1, colorless)
        if (name == "Savannah Lions") giveMana(player1, Color.WHITE, 1)
        castSpell(player1, inHand).error shouldBe null
        settle(accept = true)
    }

    test("a spell sharing only a color with the exiled card is punished") {
        val d = driver()
        d.castPrison(accept = true)
        withClue("the Counterspell was exiled with Thought Prison, to its owner's exile") {
            d.getExileCardNames(d.player2) shouldBe listOf("Counterspell")
        }

        // Phantom Warrior is blue like Counterspell, but mana value 3 rather than 2.
        d.castCreature("Phantom Warrior", blue = 2, colorless = 1)

        withClue("2 damage to the caster — player 1, who cast it") {
            d.getLifeTotal(d.player1) shouldBe 18
        }
        withClue("the opponent is untouched; the trigger targets the caster, not the controller") {
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }

    test("a spell sharing only a mana value with the exiled card is punished") {
        val d = driver()
        d.castPrison(accept = true)

        // Artifact Creature costs {2} — mana value 2 like Counterspell, and colorless, so the
        // colour half of the predicate cannot be what fired.
        d.castCreature("Artifact Creature", blue = 0, colorless = 2)

        d.getLifeTotal(d.player1) shouldBe 18
    }

    test("a spell sharing neither a color nor a mana value is not punished") {
        val d = driver()
        d.castPrison(accept = true)

        // Savannah Lions is {W}: white, mana value 1. Neither half matches.
        d.castCreature("Savannah Lions", blue = 0, colorless = 0)

        withClue("no shared characteristic, no damage") {
            d.getLifeTotal(d.player1) shouldBe 20
        }
    }

    test("declining the imprint leaves the trigger permanently silent") {
        val d = driver()
        d.castPrison(accept = false)
        d.getExileCardNames(d.player2) shouldBe emptyList()

        // A spell that *would* have matched had the Counterspell been exiled.
        d.castCreature("Phantom Warrior", blue = 2, colorless = 1)

        withClue("no exiled card means no reference, and a missing reference matches nothing") {
            d.getLifeTotal(d.player1) shouldBe 20
        }
    }
})
