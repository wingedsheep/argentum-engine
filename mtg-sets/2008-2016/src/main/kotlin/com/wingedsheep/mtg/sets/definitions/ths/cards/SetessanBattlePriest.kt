package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Setessan Battle Priest — Theros #29 (canonical printing)
 * {1}{W}
 * Creature — Human Cleric
 * 1/3
 * Heroic — Whenever you cast a spell that targets this creature, you gain 2 life.
 */
val SetessanBattlePriest = card("Setessan Battle Priest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.GainLife(2)
        description = "Heroic — Whenever you cast a spell that targets this creature, you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Wesley Burt"
        flavorText = "\"Your god teaches you only how to kill. Karametra teaches me to defend what I hold dear. That is why I will prevail.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c9cfa33-dcec-4b34-9218-c08ad932d27b.jpg?1783939808"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
