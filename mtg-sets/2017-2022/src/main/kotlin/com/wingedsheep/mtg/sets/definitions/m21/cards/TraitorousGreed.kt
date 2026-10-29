package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Traitorous Greed — Core Set 2021 #166 (only printing)
 * {3}{R} · Sorcery
 *
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until
 * end of turn. Add two mana of any one color.
 *
 * Act of Treason plus Metamorphosis's spell-side mana: "two mana of any one color" is
 * [Effects.AddAnyColorMana] with amount 2 — one colour choice, added twice. An illegal target
 * fizzles the whole spell, so no mana is added then (ruling).
 */
val TraitorousGreed = card("Traitorous Greed") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn. " +
        "Add two mana of any one color. (The creature can attack and {T} this turn.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.AddAnyColorMana(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Svetlin Velinov"
        flavorText = "\"Remind me again, what's the going rate for betrayal these days?\"\n—Captain Rhenmir"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab7052f1-9736-47b6-9da3-8c5ca925ab54.jpg?1783930683"
        ruling("2020-06-23", "Traitorous Greed can target any creature, even one that's untapped or one you already control.")
        ruling("2020-06-23", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
        ruling("2020-06-23", "If the target creature is an illegal target by the time Traitorous Greed tries to resolve, the spell won't resolve. You won't add two mana.")
    }
}
