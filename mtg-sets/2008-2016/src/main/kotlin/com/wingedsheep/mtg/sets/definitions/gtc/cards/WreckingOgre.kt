package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wrecking Ogre — Gatecrash #112 (canonical printing)
 * {4}{R} · Creature — Ogre Warrior · 3/3
 *
 * Double strike
 * Bloodrush — {3}{R}{R}, Discard this card: Target attacking creature gets +3/+3 and gains double
 * strike until end of turn.
 */
val WreckingOgre = card("Wrecking Ogre") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "Double strike\nBloodrush — {3}{R}{R}, Discard this card: Target attacking creature gets +3/+3 and gains double strike until end of turn."

    keywords(Keyword.DOUBLE_STRIKE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(3, 3, creature) then
            Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature)
        description = "Bloodrush — {3}{R}{R}, Discard this card: Target attacking creature gets +3/+3 and gains double strike until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Nils Hamm"
        flavorText = "If no opponent can be found, ogres of the Bolrac Clan will often pick fights with buildings."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87f1a27c-c576-4c34-873f-6faf020c2773.jpg?1783940120"
    }
}
