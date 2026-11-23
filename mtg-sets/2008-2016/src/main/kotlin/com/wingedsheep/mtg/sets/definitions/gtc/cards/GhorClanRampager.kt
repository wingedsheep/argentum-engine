package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ghor-Clan Rampager — Gatecrash #167 (canonical printing)
 * {2}{R}{G} · Creature — Beast · 4/4
 *
 * Trample
 * Bloodrush — {R}{G}, Discard this card: Target attacking creature gets +4/+4 and gains trample
 * until end of turn.
 */
val GhorClanRampager = card("Ghor-Clan Rampager") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Trample\nBloodrush — {R}{G}, Discard this card: Target attacking creature gets +4/+4 and gains trample until end of turn."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(4, 4, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "Bloodrush — {R}{G}, Discard this card: Target attacking creature gets +4/+4 and gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Charles Urbach"
        flavorText = "\"The Simic come from the cold slow depths. How could they understand the fire in a wild heart?\"\n—Kroshkar, Gruul shaman"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/382048ec-0bf5-49a5-90d5-f80fbda08962.jpg?1783940107"
    }
}
