package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Virtuous Charge
 * {2}{W}
 * Sorcery
 * Creatures you control get +1/+1 until end of turn.
 *
 * The board-wide pump is [Effects.ForEachInGroup] over `creaturesYouControl` with the modifier aimed
 * at [EffectTarget.Self] — the current iteration entity.
 */
val VirtuousCharge = card("Virtuous Charge") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +1/+1 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            Filters.Group.creaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Qu Xin"
        flavorText = "\"The empire belongs to no one man but to all in the empire. He who has virtue shall possess it.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72ad4c79-a85d-4fc6-95ab-5a6d6d667579.jpg"
    }
}
