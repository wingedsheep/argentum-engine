package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Circle of Dreams Druid
 * {G}{G}{G}
 * Creature — Elf Druid
 * 2/1
 * {T}: Add {G} for each creature you control.
 */
val CircleOfDreamsDruid = card("Circle of Dreams Druid") {
    manaCost = "{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "{T}: Add {G} for each creature you control."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.GREEN,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Sam Guay"
        flavorText = "The realms protected by druids of the Circle of Dreams are gleaming, fruitful places, where dream and reality blur together and the weary can find rest."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be6fdec0-a2c4-4da2-ae14-961185eaee66.jpg?1783926466"
    }
}
