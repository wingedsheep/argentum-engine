package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent

/**
 * Circle of the Moon Druid
 * {2}{G}
 * Creature — Human Elf Druid
 * 2/4
 * Bear Form — During your turn, this creature is a Bear with base power and toughness 4/2.
 * (It loses all other creature types.)
 *
 * Futurist Operative's shape, gated on the turn instead of on being tapped: one static ability
 * spanning two layers. [TransformPermanent] with only `setSubtypes` replaces every creature type
 * with Bear (Layer 4) while leaving the card types alone, and [SetBasePowerToughnessStatic] sets
 * base P/T to 4/2 (Layer 7b), so +1/+1 counters and pumps still apply on top. Off your turn the
 * condition fails and it is a 2/4 Human Elf Druid again. "Bear Form" is an ability word with no
 * rules meaning.
 */
val CircleOfTheMoonDruid = card("Circle of the Moon Druid") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Elf Druid"
    power = 2
    toughness = 4
    oracleText = "Bear Form — During your turn, this creature is a Bear with base power and " +
        "toughness 4/2. (It loses all other creature types.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            condition = Conditions.IsYourTurn,
            ability = CompositeStaticAbility(
                listOf(
                    TransformPermanent(setSubtypes = setOf(Subtype.BEAR.value), filter = Filters.Self),
                    SetBasePowerToughnessStatic(4, 2, Filters.Self),
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Nicholas Elias"
        flavorText = "Druids of the Circle of the Moon are fierce guardians of nature who haunt " +
            "the deepest parts of the wilderness."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/700c9ed8-6bfe-4f1e-855a-5f31e0d2fc9b.jpg?1783926466"
    }
}
