package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skarrg Goliath — Gatecrash #133 (canonical printing)
 * {6}{G}{G} · Creature — Beast · 9/9
 *
 * Trample
 * Bloodrush — {5}{G}{G}, Discard this card: Target attacking creature gets +9/+9 and gains trample
 * until end of turn.
 */
val SkarrgGoliath = card("Skarrg Goliath") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 9
    toughness = 9
    oracleText = "Trample\nBloodrush — {5}{G}{G}, Discard this card: Target attacking creature gets +9/+9 and gains trample until end of turn."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(9, 9, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "Bloodrush — {5}{G}{G}, Discard this card: Target attacking creature gets +9/+9 and gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Scott Chou"
        flavorText = "\"They bind us with their laws. We free ourselves with nature's fist.\"\n—Nikya of the Old Ways"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b2dcafd-eb72-4f3a-9c1c-ba17fe30bf0f.jpg?1783940116"
    }
}
