package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wasteland Viper — Gatecrash #139 (canonical printing)
 * {G} · Creature — Snake · 1/2
 *
 * Deathtouch
 * Bloodrush — {G}, Discard this card: Target attacking creature gets +1/+2 and gains
 * deathtouch until end of turn.
 */
val WastelandViper = card("Wasteland Viper") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 1
    toughness = 2
    oracleText = "Deathtouch\nBloodrush — {G}, Discard this card: Target attacking creature gets +1/+2 and gains deathtouch until end of turn."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(1, 2, creature) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
        description = "Bloodrush — {G}, Discard this card: Target attacking creature gets +1/+2 and gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Lucas Graciano"
        flavorText = "\"The Gruul embrace all things poisonous and living in the cracks of society.\"\n—Teysa Karlov, Grand Envoy of Orzhov"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4a5b2b8-3890-485f-8731-8f178a2da3d7.jpg?1783940113"
    }
}
