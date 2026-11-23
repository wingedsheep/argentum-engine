package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scab-Clan Charger — Gatecrash #131 (canonical printing)
 * {3}{G} · Creature — Centaur Warrior · 2/4
 *
 * Bloodrush — {1}{G}, Discard this card: Target attacking creature gets +2/+4 until end of turn.
 */
val ScabClanCharger = card("Scab-Clan Charger") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 2
    toughness = 4
    oracleText = "Bloodrush — {1}{G}, Discard this card: Target attacking creature gets +2/+4 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(2, 4, creature)
        description = "Bloodrush — {1}{G}, Discard this card: Target attacking creature gets +2/+4 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Nils Hamm"
        flavorText = "\"The only roads I wish to see are those beaten flat by wild hooves.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/964c88d3-3141-44ab-8856-44a3f08331ea.jpg?1783940115"
    }
}
