package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spawn of Thraxes
 * {5}{R}{R}
 * Creature — Dragon
 * 5/5
 *
 * Flying
 * When this creature enters, it deals damage to any target equal to the number of Mountains you control.
 */
val SpawnOfThraxes = card("Spawn of Thraxes") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, it deals damage to any target equal to the number of Mountains you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)).count(),
            anyTarget,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Svetlin Velinov"
        flavorText = "Sparks from Purphoros's forge fill the belly of every dragon."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e13aa40e-c626-486d-8abc-10aa68542c5b.jpg?1783939418"
    }
}
