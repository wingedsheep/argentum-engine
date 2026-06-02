package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Restock
 * {3}{G}{G}
 * Sorcery
 * Return two target cards from your graveyard to your hand. Exile Restock.
 */
val Restock = card("Restock") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return two target cards from your graveyard to your hand. Exile Restock."

    spell {
        target = TargetObject(
            count = 2,
            filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11a013ff-7c99-445a-b9e0-0fc45036f068.jpg?1562898535"
    }
}
