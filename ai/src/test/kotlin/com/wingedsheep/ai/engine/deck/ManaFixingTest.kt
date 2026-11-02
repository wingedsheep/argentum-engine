package com.wingedsheep.ai.engine.deck

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * [ManaFixing] reads what a card does for a limited deck's colours off its structure — the reading the
 * deckbuilder falls back to on every set whose archetype table carries no curated fixing list.
 */
class ManaFixingTest : FunSpec({

    val byName: Map<String, CardDefinition> by lazy {
        val out = HashMap<String, CardDefinition>()
        for (set in MtgSetCatalog.all) for (card in set.cards) out.putIfAbsent(card.name, card)
        out
    }
    fun card(name: String) = byName.getValue(name)
    val all = Color.entries.toSet()

    test("a land that fetches any basic fixes every colour, and is a fetcher") {
        ManaFixing.colors(card("Evolving Wilds")) shouldBe all
        ManaFixing.fetchesBasic(card("Evolving Wilds")) shouldBe true
    }

    test("a sacrifice land that finds one of three named basics fixes those three, and is a fetcher") {
        ManaFixing.colors(card("Riveteers Overlook")) shouldBe setOf(Color.BLACK, Color.RED, Color.GREEN)
        ManaFixing.fetchesBasic(card("Riveteers Overlook")) shouldBe true
    }

    test("a gain land fixes its two colours and is no fetcher") {
        ManaFixing.colors(card("Jungle Hollow")) shouldBe setOf(Color.BLACK, Color.GREEN)
        ManaFixing.fetchesBasic(card("Jungle Hollow")) shouldBe false
    }

    test("a land of one chosen colour is a basic of your choice, not a five-colour land") {
        ManaFixing.colors(card("Uncharted Haven")).shouldBeEmpty()
    }

    test("a Thriving land counts only the colour it always makes") {
        ManaFixing.colors(card("Thriving Bluff")) shouldBe setOf(Color.RED)
    }

    test("nonland fixers: any-colour rock, basic-land search, our own Treasure") {
        ManaFixing.colors(card("Prophetic Prism")) shouldBe all
        ManaFixing.colors(card("Rampant Growth")) shouldBe all
        ManaFixing.colors(card("Prosperous Innkeeper")) shouldBe all
    }

    test("a mono-coloured dork is ramp, not fixing") {
        ManaFixing.colors(card("Llanowar Elves")).shouldBeEmpty()
    }

    test("Treasure an opponent gets, or one from combat damage, is not fixing to build on") {
        ManaFixing.colors(card("Buy Your Silence")).shouldBeEmpty()
        ManaFixing.colors(card("Professional Face-Breaker")).shouldBeEmpty()
    }

    test("Vivid, domain and converge reward colours; a vanilla card doesn't") {
        ManaFixing.rewardsColors(card("Rime Chill")) shouldBe true
        ManaFixing.rewardsColors(card("Wayfaring Giant")) shouldBe true
        ManaFixing.rewardsColors(card("Together as One")) shouldBe true
        ManaFixing.rewardsColors(card("Llanowar Elves")) shouldBe false
    }
})
