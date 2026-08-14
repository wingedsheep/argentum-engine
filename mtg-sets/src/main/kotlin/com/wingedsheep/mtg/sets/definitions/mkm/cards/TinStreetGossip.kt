package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Tin Street Gossip — Murders at Karlov Manor #235
 * {2}{R}{G} · Creature — Lizard Advisor · 4/4
 *
 * Vigilance
 * {T}: Add {R}{G}. Spend this mana only to cast face-down spells or to turn creatures face up.
 *
 * The disguise deck's mana engine: vigilance means it can attack and still leave two mana up to
 * flip something mid-combat.
 *
 * The spend clause is a two-atom [ManaRestriction.AnyOf] —
 * [ManaRestriction.FaceDownSpellsOnly] for the cast half (added for this card) and the existing
 * [ManaRestriction.TurnPermanentsFaceUpOnly] for the flip half. The printed wording says "turn
 * *creatures* face up" where the atom says "permanents", which is not a gap: a face-down permanent
 * is always a 2/2 creature (CR 708.2), so the two sets coincide.
 *
 * Per the printed ruling this mana can't pay for a spell that instructs you to cloak or manifest —
 * those put cards onto the battlefield face down without anything being *cast* face down, which is
 * why [ManaRestriction.FaceDownSpellsOnly] keys off the cast itself and not off face-down-ness in
 * general.
 *
 * `{T}: Add {R}{G}` is one activation producing two mana of different colors, so the effect is a
 * composite of two [Effects.AddMana] calls carrying the same restriction (Gruul Signet's shape).
 * Both halves are a mana ability (CR 605.1a): no target, adds mana, doesn't use the stack.
 */
val TinStreetGossip = card("Tin Street Gossip") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Lizard Advisor"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "{T}: Add {R}{G}. Spend this mana only to cast face-down spells or to turn creatures face up."

    keywords(Keyword.VIGILANCE)

    val faceDownMana = ManaRestriction.AnyOf(
        listOf(
            ManaRestriction.FaceDownSpellsOnly,
            ManaRestriction.TurnPermanentsFaceUpOnly
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            Effects.AddMana(Color.RED, 1, restriction = faceDownMana),
            Effects.AddMana(Color.GREEN, 1, restriction = faceDownMana)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {R}{G}. Spend this mana only to cast face-down spells or to turn " +
            "creatures face up."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Tony Foti"
        flavorText = "\"The lawmages are trying to hush it up, but I'm telling you—the guard didn't " +
            "fall off that balcony. He was pushed!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/4094b13f-28d4-48b6-8cce-3c44656745b7.jpg?1783912834"

        ruling(
            "2024-02-02",
            "The mana produced by Tin Street Gossip can't be used to cast a spell that instructs " +
                "you to cloak or manifest cards."
        )
    }
}
