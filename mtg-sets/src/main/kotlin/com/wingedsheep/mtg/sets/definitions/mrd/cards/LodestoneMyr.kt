package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lodestone Myr — Mirrodin #200
 * {4} · Artifact Creature — Myr
 * 2/2
 *
 * Trample
 * Tap an untapped artifact you control: This creature gets +1/+1 until end of turn.
 *
 * The activation cost is [Costs.TapPermanents] over [GameObjectFilter.Artifact] with
 * `excludeSelf = false`, which is load-bearing twice over:
 *  - Lodestone Myr is itself an artifact, so it is a legal choice for its own cost (the
 *    Birchlore Rangers idiom) — tapping itself for +1/+1 is a real line of play.
 *  - The cost is *not* the {T} symbol of this ability, so summoning sickness never gates it:
 *    CR 302.6 only restricts {T}/{Q} in a creature's *own* activation cost, and this ability's
 *    cost is "tap an untapped artifact you control". A Myr that entered this turn can be tapped
 *    to pump itself, and so can any other summoning-sick artifact creature you control.
 *
 * The ability has no {T} symbol and no mana, so it can be activated arbitrarily many times as
 * long as there are untapped artifacts left to tap.
 */
val LodestoneMyr = card("Lodestone Myr") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Myr"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Tap an untapped artifact you control: This creature gets +1/+1 until end of turn."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Artifact)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Tap an untapped artifact you control: This creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Greg Staples"
        flavorText = "When necessary, myr can override and control any artificial object, as can their creator."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/faab1d25-dee5-4315-ba63-f8e14087a9c0.jpg?1783944514"
    }
}
