package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Abhorrent Overlord
 * {5}{B}{B}
 * Creature — Demon
 * 6/6
 * Flying
 * When this creature enters, create a number of 1/1 black Harpy creature tokens with flying equal to
 * your devotion to black.
 * At the beginning of your upkeep, sacrifice a creature.
 *
 * Devotion is counted as the trigger resolves, so the Overlord's own {B}{B} counts if it's still on
 * the battlefield. The upkeep sacrifice may choose the Overlord itself.
 */
val AbhorrentOverlord = card("Abhorrent Overlord") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\nWhen this creature enters, create a number of 1/1 black Harpy creature tokens " +
        "with flying equal to your devotion to black. (Each {B} in the mana costs of permanents you " +
        "control counts toward your devotion to black.)\nAt the beginning of your upkeep, sacrifice a creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.devotionTo(Color.BLACK),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Harpy"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/2/6/26b24bbe-f5bc-44d3-b716-6612d39b07bc.jpg?1783939700"
        )
        description = "When this creature enters, create a number of 1/1 black Harpy creature tokens with " +
            "flying equal to your devotion to black."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.SacrificeOwn(GameObjectFilter.Creature)
        description = "At the beginning of your upkeep, sacrifice a creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4415d050-7a76-4f8b-bf78-e33dd21fe4f1.jpg?1783939785"
        ruling(
            "2013-09-15",
            "Numeric mana symbols ({0}, {1}, and so on) in mana costs of permanents you control don't " +
                "count toward your devotion to any color."
        )
        ruling(
            "2013-09-15",
            "Mana symbols in the text boxes of permanents you control don't count toward your devotion to " +
                "any color."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count " +
                "toward your devotion to their color(s)."
        )
        ruling(
            "2013-09-15",
            "If an activated ability or triggered ability has an effect that depends on your devotion to a " +
                "color, you count the number of mana symbols of that color among the mana costs of " +
                "permanents you control as the ability resolves. The permanent with that ability will be " +
                "counted if it's still on the battlefield at that time."
        )
    }
}
