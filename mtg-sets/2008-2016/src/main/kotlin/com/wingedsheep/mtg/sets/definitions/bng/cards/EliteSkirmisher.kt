package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elite Skirmisher — Born of the Gods #8 (canonical printing)
 * {2}{W}
 * Creature — Human Soldier
 * 3/1
 * Heroic — Whenever you cast a spell that targets this creature, you may tap target creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Skyguard); "you may tap target creature" is the
 * optional targeted tap of Seasoned Marshal.
 */
val EliteSkirmisher = card("Elite Skirmisher") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, you may tap target creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        optional = true
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
        description = "Heroic — Whenever you cast a spell that targets this creature, you may tap target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Mark Winters"
        flavorText = "Some adopted the tactics of the leonin to combat the ferocity of the minotaurs."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/982eed00-e9fe-4d0f-a2b7-9606aa7926a1.jpg?1783939583"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
