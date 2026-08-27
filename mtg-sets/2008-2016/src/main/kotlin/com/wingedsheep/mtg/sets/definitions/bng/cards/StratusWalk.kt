package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Stratus Walk
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature has flying.
 * Enchanted creature can block only creatures with flying.
 */
val StratusWalk = card("Stratus Walk") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, draw a card.\nEnchanted creature has flying. (It can't be blocked except by creatures with flying or reach.)\nEnchanted creature can block only creatures with flying."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        // `CanOnlyBlockCreaturesWith` defaults its `filter` to `GroupFilter.source()`, which would
        // restrict the *Aura*; the printed line is about the enchanted creature.
        ability = CanOnlyBlockCreaturesWith(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44744725-6ba7-40bd-b25b-788a1032ecf4.jpg?1783939565"
    }
}
