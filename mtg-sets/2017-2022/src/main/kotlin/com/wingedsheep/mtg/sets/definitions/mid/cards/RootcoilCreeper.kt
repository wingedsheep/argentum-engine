package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rootcoil Creeper (Innistrad: Midnight Hunt #238)
 * {G}{U} · Creature — Plant Horror 2/2
 * {T}: Add one mana of any color.
 * {T}: Add two mana of any one color. Spend this mana only to cast spells from your graveyard.
 * {G}{U}, {T}, Exile this creature: Return target card with flashback you own from exile to your hand.
 *
 * The restricted mana is [ManaRestriction.CastFromGraveyardOnly] — any cast from your own
 * graveyard (flashback, disturb, …). "Card with flashback" is the FLASHBACK keyword, which a card
 * keeps in exile (the Sorceress's Schemes filter).
 */
val RootcoilCreeper = card("Rootcoil Creeper") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Plant Horror"
    power = 2
    toughness = 2
    oracleText = "{T}: Add one mana of any color.\n" +
        "{T}: Add two mana of any one color. Spend this mana only to cast spells from your graveyard.\n" +
        "{G}{U}, {T}, Exile this creature: Return target card with flashback you own from exile to your hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(2, restriction = ManaRestriction.CastFromGraveyardOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{U}"), Costs.Tap, Costs.ExileSelf)
        val card = target(
            TargetFilter(GameObjectFilter.Any.withKeyword(Keyword.FLASHBACK).ownedByYou(), zone = Zone.EXILE)
        )
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3743cf9c-226f-43a3-b385-375a25414792.jpg?1783925554"
    }
}
