package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Akroan Crusader — Theros #111 (canonical printing)
 * {R}
 * Creature — Human Soldier
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, create a 1/1 red Soldier creature
 * token with haste.
 *
 * Heroic is the `TargetsSource` cast trigger (Favored Hoplite, Akroan Skyguard).
 */
val AkroanCrusader = card("Akroan Crusader") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, create a 1/1 red Soldier creature " +
        "token with haste. (It can attack and {T} as soon as it comes under your control.)"

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/5/0/509e44d4-72c1-4d68-b942-c3e8de6ed7a8.jpg?1783939699",
        )
        description = "Heroic — Whenever you cast a spell that targets this creature, create a 1/1 red Soldier " +
            "creature token with haste."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Johann Bodin"
        flavorText = "An Akroan soldier's worth is measured by the number of swords raised by his battle cry."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a47c50d9-11ec-486b-a5ce-5a6b70858f30.jpg?1783939767"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
