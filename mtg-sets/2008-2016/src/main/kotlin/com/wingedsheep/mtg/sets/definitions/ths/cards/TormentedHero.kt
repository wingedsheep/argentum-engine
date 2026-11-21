package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Tormented Hero — Theros #108 (canonical printing)
 * {B}
 * Creature — Human Warrior
 * 2/1
 * This creature enters tapped.
 * Heroic — Whenever you cast a spell that targets this creature, each opponent loses 1 life.
 * You gain life equal to the life lost this way.
 *
 * [Effects.DrainLife] gains the life actually lost, not a fixed amount.
 */
val TormentedHero = card("Tormented Hero") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "This creature enters tapped.\n" +
        "Heroic — Whenever you cast a spell that targets this creature, each opponent loses 1 life. " +
        "You gain life equal to the life lost this way."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.DrainLife(1)
        description = "Heroic — Whenever you cast a spell that targets this creature, each opponent loses 1 life. " +
            "You gain life equal to the life lost this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c101cf1b-cedf-4b74-9e3c-35f9a8695a8f.jpg?1783939772"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
