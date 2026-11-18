package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Quarry Colossus
 * {5}{W}{W}
 * Creature — Giant
 * 5/6
 * When this creature enters, put target creature into its owner's library just beneath the top X
 * cards of that library, where X is the number of Plains you control.
 *
 * X is counted as the trigger resolves; "Plains" is the land type, so any land with that subtype
 * counts. With X at or past the library's size the creature goes to the bottom.
 */
val QuarryColossus = card("Quarry Colossus") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant"
    power = 5
    toughness = 6
    oracleText = "When this creature enters, put target creature into its owner's library just beneath the top X cards of that library, where X is the number of Plains you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.PutIntoLibraryBeneathTop(
            creature,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Plains")).count(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d921128a-ebfd-4e3c-8728-481a8c3c03f3.jpg?1783939455"
    }
}
