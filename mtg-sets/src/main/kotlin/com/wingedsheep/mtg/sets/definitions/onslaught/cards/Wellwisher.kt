package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GainLifeEffect

/**
 * Wellwisher
 * {1}{G}
 * Creature — Elf
 * 1/1
 * {T}: You gain 1 life for each Elf on the battlefield.
 */
val Wellwisher = card("Wellwisher") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        effect = GainLifeEffect(DynamicAmount.CreaturesWithSubtypeOnBattlefield(Subtype("Elf")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "300"
        artist = "Christopher Rush"
        flavorText = "\"Close your ears to the voice of greed, and you can turn a gift for one into a gift for many.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be95ab7c-0e77-4293-aa48-ee54902a363f.jpg?1562939969"
    }
}
