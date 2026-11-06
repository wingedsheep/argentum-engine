package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceGameLossWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Exquisite Archangel
 * {5}{W}{W}
 * Creature — Angel
 * 5/5
 * Flying
 * If you would lose the game, instead exile this creature and your life total becomes equal to
 * your starting life total.
 *
 * Dealt lethal damage in the same state-based-action pass that would lose you the game, you choose
 * whether it is exiled or goes to the graveyard (its first ruling) — the engine asks
 * (GameLossSourceChoiceCheck); either way your life total resets.
 */
val ExquisiteArchangel = card("Exquisite Archangel") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying\nIf you would lose the game, instead exile this creature and your life total " +
        "becomes equal to your starting life total."

    keywords(Keyword.FLYING)

    replacementEffect(
        ReplaceGameLossWith(
            replacementEffect = Effects.Exile(EffectTarget.Self, fromZone = Zone.BATTLEFIELD) then
                Effects.SetLifeTotal(DynamicAmounts.startingLifeTotal())
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Brad Rigney"
        flavorText = "A living invention born of a grand design."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d04018c9-510c-4610-9daa-677434628805.jpg?1783936779"
    }
}
