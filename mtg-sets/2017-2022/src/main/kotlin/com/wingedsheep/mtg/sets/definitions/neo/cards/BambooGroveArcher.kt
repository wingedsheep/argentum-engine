package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bamboo Grove Archer — Kamigawa: Neon Dynasty #173 (canonical printing)
 * {1}{G} · Enchantment Creature — Snake Archer · 3/3
 *
 * Defender, reach
 * Channel — {4}{G}, Discard this card: Destroy target creature with flying.
 */
val BambooGroveArcher = card("Bamboo Grove Archer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Snake Archer"
    power = 3
    toughness = 3
    oracleText = "Defender, reach\n" +
        "Channel — {4}{G}, Discard this card: Destroy target creature with flying."

    keywords(Keyword.DEFENDER, Keyword.REACH)

    // Channel — {4}{G}, Discard this card (from hand): destroy target creature with flying.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.Destroy(creature)
        description = "Channel — {4}{G}, Discard this card: Destroy target creature with flying."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Steve Prescott"
        flavorText = "\"Do not mistake tranquility for domestication. This land is wild, and will always remain so.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b43bb9f-1b84-41cc-a4f9-5875ae6d207c.jpg?1783923854"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
