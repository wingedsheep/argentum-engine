package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

private const val EXTORT_TEXT = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, " +
    "each opponent loses 1 life and you gain that much life.)"

/**
 * Crypt Ghast
 * {3}{B}
 * Creature — Spirit
 * 2/2
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 * you gain that much life.)
 * Whenever you tap a Swamp for mana, add an additional {B}.
 *
 * Extort is composed as on Sorin of House Markov / The Kingpin of Crime: a cast trigger with a
 * resolution-time [Effects.MayPay] of `{W/B}` over [Effects.DrainLife] (the gain is the total lost).
 * The mana half is the Nissa, Who Shakes the World shape: [AdditionalManaOnSourceTap] over Swamps you
 * control (projected subtype, so nonbasic Swamps count), a triggered mana ability off the stack.
 */
val CryptGhast = card("Crypt Ghast") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "$EXTORT_TEXT\nWhenever you tap a Swamp for mana, add an additional {B}."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.MayPay(cost = ManaCost.parse("{W/B}"), then = Effects.DrainLife(1))
        description = EXTORT_TEXT
    }

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.withSubtype(Subtype.SWAMP).youControl(),
            color = Color.BLACK,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3795a4e7-646f-4bb7-b154-2610eb740e8d.jpg?1783940132"
        ruling("2024-01-12", "You may pay {W/B} a maximum of one time for each extort triggered ability. You decide whether to pay when the ability resolves.")
        ruling("2024-01-12", "The amount of life you gain from extort is based on the total amount of life lost, not necessarily the number of opponents you have.")
        ruling("2024-01-12", "The extort ability doesn't target any player.")
        ruling("2024-01-12", "The extort ability resolves before the spell that caused it to trigger. The ability resolves even if that spell is countered.")
    }
}
