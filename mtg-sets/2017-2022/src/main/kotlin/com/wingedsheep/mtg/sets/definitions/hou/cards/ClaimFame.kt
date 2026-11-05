package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Claim // Fame — Hour of Devastation #150
 *
 * Claim {B} — Sorcery: Return target creature card with mana value 2 or less from your graveyard to
 *   the battlefield.
 * Fame {1}{R} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Target creature gets +2/+0 and gains haste until end of turn.
 */
val ClaimFame = card("Claim // Fame") {
    layout = CardLayout.SPLIT
    colorIdentity = "BR"

    face("Claim") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Return target creature card with mana value 2 or less from your graveyard to the battlefield."

        spell {
            val card = target(TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(2), zone = Zone.GRAVEYARD))
            effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        }
    }

    face("Fame") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Target creature gets +2/+0 and gains haste until end of turn."
        keywords(Keyword.AFTERMATH)

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 0, creature) then
                Effects.GrantKeyword(Keyword.HASTE, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15b0f214-8668-4921-88ba-7ccf38c9f770.jpg?1783936007"
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
