package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.WizardsSpellbook
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Wizard's Spellbook (AFR #82): {T}, sorcery speed — exile target instant or sorcery card from a
 * graveyard (linked to the Spellbook), then a d20 picks how the copy is cast: 1—9 paying its cost,
 * 10—19 for {1}, 20 every card in the Spellbook's linked pile for free.
 */
class WizardsSpellbookScenarioTest : FunSpec({

    val blast = card("Spellbook Test Blast") {
        manaCost = "{3}"
        typeLine = "Sorcery"
        spell { effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }
    val surge = card("Spellbook Test Surge") {
        manaCost = "{4}{U}{U}"
        typeLine = "Instant"
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(WizardsSpellbook, blast, surge))
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Tap the Spellbook at [card], give [mana] colorless to the pool, force the d20, and resolve the ability. */
    fun GameTestDriver.activate(book: EntityId, card: EntityId, natural: Int, mana: Int) {
        val player = activePlayer!!
        submit(
            ActivateAbility(
                player,
                book,
                WizardsSpellbook.activatedAbilities.single().id,
                listOf(ChosenTarget.Card(card, player, Zone.GRAVEYARD)),
            )
        ).outcome shouldBe Outcome.Done
        if (mana > 0) giveColorlessMana(player, mana)
        replaceState(state.copy(rng = GameRng.seeded(seedFor(natural))))
        bothPass().events.filterIsInstance<DieRolledEvent>().single().result shouldBe natural
    }

    /** Answer every offer: [accept] the "may cast", pick each offered copy, and drain the stack. */
    fun GameTestDriver.castCopies(accept: Boolean) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 80) {
            val decision = state.pendingDecision
            when {
                decision is YesNoDecision -> submitYesNo(decision.playerId, accept)
                decision is SelectCardsDecision && accept && decision.options.isNotEmpty() ->
                    submitCardSelection(decision.playerId, listOf(decision.options.first()))
                decision is SelectCardsDecision -> submitCardSelection(decision.playerId, emptyList())
                decision is SelectManaSourcesDecision -> submitManaAutoPayOrDecline(decision.playerId, autoPay = true)
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    test("1-9 exiles the card and casts the copy paying its mana cost") {
        val d = driver()
        val me = d.activePlayer!!
        val book = d.putPermanentOnBattlefield(me, "Wizard's Spellbook")
        val card = d.putCardInGraveyard(me, "Spellbook Test Blast")

        d.activate(book, card, natural = 5, mana = 3)
        d.castCopies(accept = true)

        d.getLifeTotal(d.getOpponent(me)) shouldBe 18
        d.getExileCardNames(me) shouldContainExactlyInAnyOrder listOf("Spellbook Test Blast")
        d.getGraveyardCardNames(me).contains("Spellbook Test Blast") shouldBe false
    }

    test("10-19 casts the copy for {1} instead of its mana cost") {
        val d = driver()
        val me = d.activePlayer!!
        val book = d.putPermanentOnBattlefield(me, "Wizard's Spellbook")
        val card = d.putCardInGraveyard(me, "Spellbook Test Surge")

        d.activate(book, card, natural = 14, mana = 1)
        d.castCopies(accept = true)

        d.getLifeTotal(d.getOpponent(me)) shouldBe 17
        d.getExileCardNames(me) shouldContainExactlyInAnyOrder listOf("Spellbook Test Surge")
    }

    test("20 copies every card exiled with this Spellbook and casts them for free") {
        val d = driver()
        val me = d.activePlayer!!
        val book = d.putPermanentOnBattlefield(me, "Wizard's Spellbook")
        val first = d.putCardInGraveyard(me, "Spellbook Test Blast")
        val second = d.putCardInGraveyard(me, "Spellbook Test Surge")

        // First activation: decline to cast; the Blast stays in the Spellbook's pile.
        d.activate(book, first, natural = 3, mana = 0)
        d.castCopies(accept = false)
        d.getLifeTotal(d.getOpponent(me)) shouldBe 20

        d.untapPermanent(book)
        d.activate(book, second, natural = 20, mana = 0)
        d.castCopies(accept = true)

        // Blast (2) + Surge (3), neither paid for.
        d.getLifeTotal(d.getOpponent(me)) shouldBe 15
        d.getExileCardNames(me) shouldContainExactlyInAnyOrder
            listOf("Spellbook Test Blast", "Spellbook Test Surge")
    }
})
