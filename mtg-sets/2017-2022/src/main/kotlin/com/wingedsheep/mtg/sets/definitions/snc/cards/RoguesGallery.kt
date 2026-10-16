package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rogues' Gallery — Streets of New Capenna #92.
 *
 * "For each color, up to one target creature card of that color" is one instance of "target" with
 * the [TargetObject.onePerColor] cross-target rule: each chosen card claims a different colour, so a
 * multicoloured card fills one colour slot and a colourless creature card can't be chosen.
 */
val RoguesGallery = card("Rogues' Gallery") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "For each color, return up to one target creature card of that color from your graveyard to your hand."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD),
            unlimited = true,
            onePerColor = true,
        )
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b9b1fce-585f-442c-99a6-3b9767d68f13.jpg?1783923124"
    }
}
