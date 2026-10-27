package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sneaking Guide — Zendikar Rising #164
 * {R} · Creature — Goblin Rogue · 1/1
 *
 * {2}, {T}: Target creature with power 2 or less can't be blocked this turn.
 */
val SneakingGuide = card("Sneaking Guide") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    oracleText = "{2}, {T}: Target creature with power 2 or less can't be blocked this turn."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val creature = target(TargetFilter(GameObjectFilter.Creature.powerAtMost(2)))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        description = "{2}, {T}: Target creature with power 2 or less can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Dan Murayama Scott"
        flavorText = "\"I swear, it's a shortcut.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/6/569c9e8c-7808-49d0-82c1-72d5b835f51c.jpg"
    }
}
