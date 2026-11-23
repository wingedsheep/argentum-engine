package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Viashino Shanktail — Gatecrash #110 (canonical printing)
 * {3}{R} · Creature — Lizard Warrior · 3/1
 *
 * First strike
 * Bloodrush — {2}{R}, Discard this card: Target attacking creature gets +3/+1 and gains
 * first strike until end of turn.
 */
val ViashinoShanktail = card("Viashino Shanktail") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    power = 3
    toughness = 1
    oracleText = "First strike\nBloodrush — {2}{R}, Discard this card: Target attacking creature gets +3/+1 and gains first strike until end of turn."

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(3, 1, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
        description = "Bloodrush — {2}{R}, Discard this card: Target attacking creature gets +3/+1 and gains first strike until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Kev Walker"
        flavorText = "The Slizt Clan's battle cry is an impressive display of fury, defiance, and spit."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5dd72d5-548a-4b0e-95bb-e6b8d2de0fbe.jpg?1783940120"
    }
}
