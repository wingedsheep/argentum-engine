package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ureni's Rebuff — Tarkir: Dragonstorm #63
 * {1}{U} · Sorcery · Uncommon
 *
 * Return target creature to its owner's hand.
 * Harmonize {5}{U} (You may cast this card from your graveyard for its harmonize cost. You may
 * tap a creature you control to reduce that cost by {X}, where X is its power. Then exile this
 * spell.)
 */
val UrenisRebuff = card("Ureni's Rebuff") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target creature to its owner's hand.\n" +
        "Harmonize {5}{U} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by {X}, where X is its power. " +
        "Then exile this spell.)"

    spell {
        target = Targets.Creature
        effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
    }

    keywordAbility(KeywordAbility.harmonize("{5}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Sergio Cosmai"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/722716df-9cea-40a7-924b-c28497e227e6.jpg?1743204214"
    }
}
