package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mothdust Changeling — Morningtide #42 (canonical printing)
 * {U} · Creature — Shapeshifter · 1/1
 *
 * Changeling (This card is every creature type.)
 * Tap an untapped creature you control: This creature gains flying until end of turn.
 *
 * The cost has no {T}, so the changeling itself (even summoning sick) is a legal creature to tap —
 * `TapPermanents` without `excludeSelf` (2008-04-01 ruling).
 */
val MothdustChangeling = card("Mothdust Changeling") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 1
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Tap an untapped creature you control: This creature gains flying until end of turn."

    keywords(Keyword.CHANGELING)

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        description = "Tap an untapped creature you control: This creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Shelly Wan"
        flavorText = "\"Ever seen a changeling fly into a lantern?\"\n—Calydd, kithkin farmer"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95da9bc9-6ac5-432d-ac12-04088cc6c74d.jpg?1783942798"
        ruling(
            "2008-04-01",
            "Since the activated ability doesn't have a tap symbol in its cost, you can tap a creature " +
                "(including Mothdust Changeling itself) that hasn't been under your control since your most " +
                "recent turn began to pay the cost."
        )
    }
}
