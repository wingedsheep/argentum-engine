package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Drown in Filth
 * {B}{G}
 * Sorcery
 * Choose target creature. Mill four cards, then that creature gets -1/-1 until end of turn for
 * each land card in your graveyard.
 *
 * The target is chosen on cast; the land count is read when the -X/-X applies, after the mill,
 * so lands milled by this spell count.
 */
val DrownInFilth = card("Drown in Filth") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Choose target creature. Mill four cards, then that creature gets -1/-1 until end of turn for each land card in your graveyard."

    spell {
        val creature = target(TargetFilter.Creature)
        val lands = -DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Land)
        effect = Patterns.Library.mill(4) then Effects.ModifyStats(lands, lands, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Seb McKinnon"
        flavorText = "The rot farmer's first rite of passage is to craft a pair of work stilts."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22feacda-01e0-4f0d-a3c7-a22e3d40bf4e.jpg?1783940030"
        ruling(
            "2013-04-15",
            "You choose the target of Drown in Filth as you cast the spell. You won't necessarily know what will happen to that creature's power and toughness at that time."
        )
    }
}
