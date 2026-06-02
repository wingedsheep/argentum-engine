package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Andúril, Flame of the West
 * {3}
 * Legendary Artifact — Equipment
 *
 * Equipped creature gets +3/+1.
 * Whenever equipped creature attacks, create two tapped 1/1 white Spirit creature tokens with
 * flying. If that creature is legendary, instead create two of those tokens that are tapped and
 * attacking.
 * Equip {2}
 */
val AndurilFlameOfTheWest = card("Andúril, Flame of the West") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+1.\n" +
        "Whenever equipped creature attacks, create two tapped 1/1 white Spirit creature tokens with flying. " +
        "If that creature is legendary, instead create two of those tokens that are tapped and attacking.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(+3, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attacks(binding = TriggerBinding.ATTACHED)
        effect = ConditionalEffect(
            condition = Conditions.EnchantedCreatureIsLegendary(),
            effect = CreateTokenEffect(
                count = DynamicAmount.Fixed(2),
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING),
                tapped = true,
                attacking = true,
                imageUri = "https://cards.scryfall.io/normal/front/b/5/b53ee2f0-afbd-491f-b3b8-c9997f175199.jpg?1699974536"
            ),
            elseEffect = CreateTokenEffect(
                count = DynamicAmount.Fixed(2),
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING),
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/b/5/b53ee2f0-afbd-491f-b3b8-c9997f175199.jpg?1699974536"
            )
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "236"
        artist = "Irvin Rodriguez"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e7b9eea-a224-4db1-a089-cb385f7af20c.jpg?1686970125"
    }
}
