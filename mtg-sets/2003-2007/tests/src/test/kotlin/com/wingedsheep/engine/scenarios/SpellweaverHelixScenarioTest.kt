package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SpellweaverHelix
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spellweaver Helix (MRD #247) — "Imprint — When this artifact enters, you may exile two target
 * sorcery cards from a single graveyard. Whenever a player casts a card, if it has the same name
 * as one of the cards exiled with this artifact, you may copy the other. If you do, you may cast
 * the copy without paying its mana cost."
 *
 * What these tests pin is "the other": the pipeline selects the imprint whose name matches the
 * spell just cast and copies the *remainder*, and that one choice is what makes all three printed
 * behaviours fall out —
 *  - a matching name copies the sibling, leaving both imprints in exile (the Helix copies, it
 *    never spends);
 *  - two imprints sharing a name produce **one** copy, not two (the 2004-12-01 ruling);
 *  - a name that matches neither imprint doesn't trigger at all.
 */
class SpellweaverHelixScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SpellweaverHelix)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Cast the Helix from hand and work its imprint trigger through to resolution, imprinting
     * [imprints] (two sorcery cards in one graveyard). Returns the Helix on the battlefield.
     */
    fun GameTestDriver.castHelix(imprints: List<EntityId>): EntityId {
        val inHand = putCardInHand(player1, "Spellweaver Helix")
        giveColorlessMana(player1, 3)
        castSpell(player1, inHand).error shouldBe null
        // Targets are chosen as the imprint trigger goes on the stack; the "you may" is answered
        // when it resolves. Stop as soon as both have happened — passing on would hand priority
        // away and end the main phase.
        var guard = 0
        while (guard++ < 12) {
            when (val decision = state.pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(player1, imprints).error shouldBe null
                is YesNoDecision -> submitYesNo(player1, true).error shouldBe null
                null -> {
                    val settled = findPermanent(player1, "Spellweaver Helix") != null &&
                        getExileCardNames(player2).isNotEmpty()
                    if (settled) break else bothPass()
                }
                else -> error("unexpected decision $decision")
            }
        }
        return findPermanent(player1, "Spellweaver Helix")!!
    }

    /**
     * Pass until the stack is empty, declining every further Helix offer. Casting a copy is itself
     * "a player casts a card", so an imprint pair whose names both appear keeps re-offering; the
     * loop is what stops these tests from having to count passes.
     */
    fun GameTestDriver.settleDecliningFurtherCopies() {
        var guard = 0
        while (guard++ < 12 && (stackSize > 0 || state.pendingDecision != null)) {
            when (val decision = state.pendingDecision) {
                null -> bothPass()
                is YesNoDecision -> submitYesNo(player1, false).error shouldBe null
                // Careful Study's own "then discard a card" — not what these tests are about.
                is SelectCardsDecision ->
                    submitCardSelection(player1, listOf(decision.options.first())).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    test("casting a card named like one imprint copies the other and casts it for free") {
        val d = driver()
        // Both imprints come out of player 2's graveyard — "from a single graveyard".
        val study = d.putCardInGraveyard(d.player2, "Careful Study")
        val doomBlade = d.putCardInGraveyard(d.player2, "Doom Blade")
        d.castHelix(listOf(study, doomBlade))

        withClue("both sorceries are exiled with the Helix") {
            d.getExileCardNames(d.player2).sorted() shouldBe listOf("Careful Study", "Doom Blade")
        }

        // A creature for the copied Doom Blade to kill — the observable proof it resolved.
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        // Player 1 casts their own Careful Study: same name as one imprint, so the Helix triggers
        // and offers the *other* one, Doom Blade.
        val ownStudy = d.putCardInHand(d.player1, "Careful Study")
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpell(d.player1, ownStudy).error shouldBe null
        d.bothPass()

        withClue("the Helix's trigger asks whether to copy") {
            d.submitYesNo(d.player1, true).error shouldBe null
        }
        withClue("only one imprint is named 'Careful Study', so no pick is needed — the copy is " +
            "Doom Blade, cast during the trigger's own resolution") {
            d.submitTargetSelection(d.player1, listOf(bears)).error shouldBe null
        }

        // Casting the copy is itself "a player casts a card", and its name matches the *other*
        // imprint — so the Helix offers again. Declining stops the loop.
        d.bothPass()
        withClue("the copy's own cast re-triggers the Helix; the second offer is declinable") {
            d.submitYesNo(d.player1, false).error shouldBe null
        }
        d.settleDecliningFurtherCopies()

        withClue("the copied Doom Blade resolved and destroyed the Bears") {
            d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        }
        withClue("the Helix copies rather than spends — both imprints are still exiled") {
            d.getExileCardNames(d.player2).sorted() shouldBe listOf("Careful Study", "Doom Blade")
        }
    }

    test("two imprints with the same name make one copy, not two") {
        val d = driver()
        val first = d.putCardInGraveyard(d.player2, "Doom Blade")
        val second = d.putCardInGraveyard(d.player2, "Doom Blade")
        d.castHelix(listOf(first, second))

        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spare = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")

        val ownBlade = d.putCardInHand(d.player1, "Doom Blade")
        d.giveMana(d.player1, Color.BLACK, 1)
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, ownBlade, listOf(bears)).error shouldBe null
        d.bothPass()

        d.submitYesNo(d.player1, true).error shouldBe null
        withClue("both imprints match the cast name, so the controller picks which one is 'it'") {
            (d.state.pendingDecision is SelectCardsDecision) shouldBe true
            d.submitCardSelection(d.player1, listOf(first)).error shouldBe null
        }
        withClue("the remainder is a single card, so a single copy is cast — target it at the Lions") {
            d.submitTargetSelection(d.player1, listOf(spare)).error shouldBe null
        }

        // The copy re-triggers the Helix in turn; decline so exactly one extra Blade resolves.
        d.settleDecliningFurtherCopies()

        withClue("exactly one extra Doom Blade resolved: the Lions died to the copy, the Bears " +
            "to the real spell") {
            d.findPermanent(d.player2, "Savannah Lions") shouldBe null
            d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        }
        withClue("both imprints stay exiled") {
            d.getExileCardNames(d.player2) shouldBe listOf("Doom Blade", "Doom Blade")
        }
    }

    test("a spell whose name matches neither imprint doesn't trigger the Helix") {
        val d = driver()
        val first = d.putCardInGraveyard(d.player2, "Doom Blade")
        val second = d.putCardInGraveyard(d.player2, "Doom Blade")
        d.castHelix(listOf(first, second))

        val study = d.putCardInHand(d.player1, "Careful Study")
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpell(d.player1, study).error shouldBe null

        withClue("the intervening-if fails, so only the spell itself is on the stack") {
            d.getStackSpellNames() shouldBe listOf("Careful Study")
            d.state.pendingDecision shouldBe null
        }
        withClue("and the imprints are untouched") {
            d.getExileCardNames(d.player2) shouldBe listOf("Doom Blade", "Doom Blade")
        }
    }
})
