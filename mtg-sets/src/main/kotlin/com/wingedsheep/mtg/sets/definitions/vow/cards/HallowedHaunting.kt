package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hallowed Haunting
 * {2}{W}{W}
 * Enchantment
 *
 * As long as you control seven or more enchantments, creatures you control have flying and vigilance.
 * Whenever you cast an enchantment spell, create a white Spirit Cleric creature token with "This
 * token's power and toughness are each equal to the number of Spirits you control."
 *
 * Implementation:
 *  - The "as long as" clause grants two keywords (flying, vigilance) to creatures you control. Each
 *    keyword is one continuous static ability gated on the same [Conditions.YouControlAtLeast] over
 *    enchantments, wrapped in a [ConditionalStaticAbility] (Hungry Ridgewolf's split-static idiom).
 *  - The token's characteristic-defining P/T is a self-referential [SetBasePowerToughnessDynamicStatic]
 *    CDA on the token itself ([GroupFilter.source]), counting Spirits you control — so it updates
 *    continuously rather than snapshotting at creation (Thousand Moons Smithy's idiom).
 */

/** Number of Spirits you control — the Spirit Cleric token's characteristic-defining P/T. */
private val spiritsYouControl =
    DynamicAmounts.battlefield(
        com.wingedsheep.sdk.scripting.references.Player.You,
        GameObjectFilter.Creature.withSubtype("Spirit"),
    ).count()

val HallowedHaunting = card("Hallowed Haunting") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As long as you control seven or more enchantments, creatures you control have " +
        "flying and vigilance.\n" +
        "Whenever you cast an enchantment spell, create a white Spirit Cleric creature token with " +
        "\"This token's power and toughness are each equal to the number of Spirits you control.\""

    val sevenEnchantments = Conditions.YouControlAtLeast(7, GameObjectFilter.Enchantment)

    // As long as you control seven or more enchantments, creatures you control have flying …
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.AllCreaturesYouControl),
            condition = sevenEnchantments,
        )
    }

    // … and vigilance.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.AllCreaturesYouControl),
            condition = sevenEnchantments,
        )
    }

    // Whenever you cast an enchantment spell, create a white Spirit Cleric creature token …
    triggeredAbility {
        trigger = Triggers.YouCastEnchantment
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit", "Cleric"),
            imageUri = "https://cards.scryfall.io/normal/front/5/2/5212bae5-d768-45ab-aba8-94c4f9fabc79.jpg?1783924699",
            staticAbilities = listOf(
                SetBasePowerToughnessDynamicStatic(
                    power = spiritsYouControl,
                    toughness = spiritsYouControl,
                    filter = GroupFilter.source(),
                ),
            ),
        )
        description = "Whenever you cast an enchantment spell, create a white Spirit Cleric creature " +
            "token with \"This token's power and toughness are each equal to the number of Spirits " +
            "you control.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/674c0a50-9c37-4c29-84d8-eb3e34f34d37.jpg?1783924918"
    }
}
