package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nature's Cloak
 * {2}{G}
 * Sorcery
 * Green creatures you control gain forestwalk until end of turn.
 */
val NaturesCloak = card("Nature's Cloak") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN).youControl()),
            GrantKeywordUntilEndOfTurnEffect(Keyword.FORESTWALK, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Rebecca Guay"
        flavorText = "The forest cloaks its children in leaves and shadow."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dfaba58-d0ab-4d1d-91dd-48543c862165.jpg"
    }
}
