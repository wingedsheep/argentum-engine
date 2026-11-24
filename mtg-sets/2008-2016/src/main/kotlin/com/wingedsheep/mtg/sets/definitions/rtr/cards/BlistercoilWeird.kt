package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blistercoil Weird
 * {U/R}
 * Creature — Weird
 * 1/1
 * Whenever you cast an instant or sorcery spell, this creature gets +1/+1 until end of turn. Untap it.
 */
val BlistercoilWeird = card("Blistercoil Weird") {
    manaCost = "{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Weird"
    oracleText = "Whenever you cast an instant or sorcery spell, this creature gets +1/+1 until end of turn. Untap it."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Dan Murayama Scott"
        flavorText = "Azorius lawmages would like to outlaw the creation of weirds, but first they'd have to settle their long-standing debate on how to classify them."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2a8e716-ea33-4ae2-9ff8-5e78b0e50459.jpg?1783940328"
    }
}
