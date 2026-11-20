package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Labyrinth Champion — Theros #126 (canonical printing)
 * {3}{R}
 * Creature — Human Warrior
 * 2/2
 * Heroic — Whenever you cast a spell that targets this creature, this creature deals 2 damage to any target.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Crusader, Favored Hoplite).
 */
val LabyrinthChampion = card("Labyrinth Champion") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, this creature deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
        description = "Heroic — Whenever you cast a spell that targets this creature, this creature deals 2 damage " +
            "to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Chase Stone"
        flavorText = "\"It used to be a lair. Now it's just a tunnel.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f86d035-a86d-4c2a-a19d-3b90e0f9ff0f.jpg?1783939761"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
