package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.ArcReactor
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The built-in AI's automatic **improvise** (CR 702.126) payment.
 *
 * Improvise is optional — "you *may* tap an untapped artifact" — and that makes it the one
 * tap-for-generic payment an auto-filler can lose the game with. A tapped artifact stops being a
 * mana source but credits only {1}, so tapping a mana rock for improvise can turn a payable cast
 * into an unpayable one. Arc Reactor is the extreme case: `{T}: Add {C}{C}{C}` traded for {1}.
 * The Whir of Invention rulings call this out — *"if an artifact you control has a mana ability
 * with {T} … you won't be able to tap it again for improvise."*
 *
 * So the AI fills the payment only when the enumerator says the taps are what make the cast
 * affordable (`LegalAction.tapForGenericRequired`). Both directions are pinned here.
 */
class ImprovisePaymentAiTest : FunSpec({

    /** A vanilla artifact — improvise fodder that produces no mana, so tapping it is free. */
    val cog = card("Improvise Cog") {
        manaCost = "{1}"
        colorIdentity = ""
        typeLine = "Artifact"
        oracleText = ""
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ArcReactor, cog))
        initMirrorMatch(Deck.of("Island" to 20))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("does not tap a mana rock for improvise when mana alone pays the cast") {
        val driver = driver()
        val player = driver.activePlayer!!
        // {5} is payable without improvise: 3 Islands + Arc Reactor's {C}{C}{C} = 6.
        repeat(3) { driver.putLandOnBattlefield(player, "Island") }
        val rock = driver.putPermanentOnBattlefield(player, "Arc Reactor")
        driver.untapPermanent(rock)
        val spell = driver.putCardInHand(player, "Arc Reactor")

        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { (it.action as? CastSpell)?.cardId == spell }
        legal.hasTapForGeneric shouldBe true
        legal.tapForGenericRequired shouldBe false

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as CastSpell
        // Tapping the Reactor for improvise would buy {1} and cost {C}{C}{C}, leaving {4} owed to
        // three Islands — the cast would hard-error at the mana step.
        (chosen.alternativePayment?.tapForGenericPermanents ?: emptySet()).isEmpty() shouldBe true

        val result = driver.submit(chosen)
        result.isSuccess shouldBe true
        result.state.getEntity(rock)!!.has<TappedComponent>() shouldBe true // tapped for mana, not improvise
    }

    test("still fills the payment when the taps are what make the cast affordable") {
        val driver = driver()
        val player = driver.activePlayer!!
        // Two Islands can't pay {5} on their own; three artifacts cover the shortfall.
        repeat(2) { driver.putLandOnBattlefield(player, "Island") }
        val cogs = List(3) { driver.putPermanentOnBattlefield(player, "Improvise Cog") }
        val spell = driver.putCardInHand(player, "Arc Reactor")

        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { (it.action as? CastSpell)?.cardId == spell }
        legal.tapForGenericRequired shouldBe true

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as CastSpell
        chosen.alternativePayment?.tapForGenericPermanents?.size shouldBe 3

        val result = driver.submit(chosen)
        result.isSuccess shouldBe true
        cogs.all { result.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
    }
})
