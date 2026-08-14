package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smaug the Magnificent — The Hobbit #110
 * {2}{R}{R} · Legendary Creature — Dragon · Mythic
 * 4/3
 *
 * Flying, haste
 * Whenever Smaug attacks, he deals damage equal to the number of Treasures you control to any target.
 * At the beginning of your upkeep, create a Treasure token.
 *
 * Modeling notes:
 *  - The attack trigger targets when it goes on the stack, but the Treasure count is read at
 *    resolution — sacrificing Treasures for mana in response shrinks the damage, and it can be
 *    zero (the ability still resolves and deals no damage).
 *  - Treasures are counted by subtype rather than by name, so a Treasure token from any source
 *    (and any nontoken Treasure artifact) counts.
 */
val SmaugTheMagnificent = card("Smaug the Magnificent") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dragon"
    power = 4
    toughness = 3
    oracleText = "Flying, haste\n" +
        "Whenever Smaug attacks, he deals damage equal to the number of Treasures you control to any target.\n" +
        "At the beginning of your upkeep, create a Treasure token."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        target = AnyTarget()
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE)
            ).count(),
            EffectTarget.ContextTarget(0)
        )
        description = "Whenever Smaug the Magnificent attacks, he deals damage equal to the number " +
            "of Treasures you control to any target."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.CreateTreasure()
        description = "At the beginning of your upkeep, create a Treasure token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Francisco Miyara"
        flavorText = "\"You think you will get a fair share? If you get off alive, you will be lucky.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a5d8fad-2ffd-4645-8c49-907999b6cecf.jpg?1783902784"
    }
}
