package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fireblade Charger — Zendikar Rising #139
 * {R} · Creature — Goblin Warrior · 1/1
 *
 * As long as this creature is equipped, it has haste.
 * When this creature dies, it deals damage equal to its power to any target.
 *
 * The haste static is Armory Veteran's shape (a source-matches `equipped()` condition re-read during
 * projection). The dies trigger uses [DynamicAmounts.sourcePower], which reads the creature's
 * last-known power — including any Equipment bonus it had as it died — like Juri and Balduvian
 * Berserker.
 */
val FirebladeCharger = card("Fireblade Charger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "As long as this creature is equipped, it has haste.\n" +
        "When this creature dies, it deals damage equal to its power to any target."

    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Lie Setiawan"
        flavorText = "\"Down in flames\" isn't a destination, but a journey."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca657846-f352-4ac4-b2b3-d0ced6607163.jpg?1783929360"
    }
}
