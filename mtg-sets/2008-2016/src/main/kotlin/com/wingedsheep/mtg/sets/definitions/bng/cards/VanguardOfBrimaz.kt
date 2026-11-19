package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Vanguard of Brimaz — Born of the Gods #29
 * {W}{W}
 * Creature — Cat Soldier
 * 2/2
 * Vigilance
 * Heroic — Whenever you cast a spell that targets this creature, create a 1/1 white Cat Soldier
 * creature token with vigilance.
 */
val VanguardOfBrimaz = card("Vanguard of Brimaz") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\nHeroic — Whenever you cast a spell that targets this creature, create a 1/1 white " +
        "Cat Soldier creature token with vigilance."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat", "Soldier"),
            keywords = setOf(Keyword.VIGILANCE),
        )
        description = "Heroic — Whenever you cast a spell that targets this creature, create a 1/1 white " +
            "Cat Soldier creature token with vigilance."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Mark Zug"
        flavorText = "\"The humans and their gods never blessed me. Only the pride deserves my allegiance.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adb38717-97d5-4763-9e6c-46251df704ed.jpg?1783939574"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
    }
}
