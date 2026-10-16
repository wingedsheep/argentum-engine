package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mayhem Patrol
 * {1}{R}
 * Creature — Devil Warrior
 * 1/2
 * Menace
 * Whenever this creature attacks, target creature gets +1/+0 until end of turn.
 * Blitz {1}{R} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val MayhemPatrol = card("Mayhem Patrol") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil Warrior"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks, target creature gets +1/+0 until end of turn.\n" +
        "Blitz {1}{R} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 1
    toughness = 2

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, t)
        description = "Whenever this creature attacks, target creature gets +1/+0 until end of turn."
    }

    keywordAbility(KeywordAbility.blitz("{1}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50162cdd-ba30-48df-93ff-197c7f4a2913.jpg?1783923117"
    }
}
