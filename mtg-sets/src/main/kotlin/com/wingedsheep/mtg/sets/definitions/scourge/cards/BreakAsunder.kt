package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Break Asunder
 * {2}{G}{G}
 * Sorcery
 * Destroy target artifact or enchantment.
 * Cycling {2}
 */
val BreakAsunder = card("Break Asunder") {
    manaCost = "{2}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact or enchantment.\nCycling {2}"

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment)))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Jim Nelson"
        flavorText = "\"No good will come of this.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb989895-a5c7-4151-8620-ab277d826303.jpg?1562537374"
    }
}
