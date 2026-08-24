package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orc General
 * {2}{R}
 * Creature — Orc Warrior
 * 2/2
 * {T}, Sacrifice another Orc or Goblin: Other Orc creatures get +1/+1 until end of turn.
 *
 * Two "other"s that mean different things, and both are the General itself. The sacrifice cost
 * excludes him (`Costs.SacrificeAnother`), and the pump excludes him too
 * (`GroupFilter(..., excludeSelf = true)`) — so the General never pumps himself no matter how many
 * Orcs he eats.
 *
 * The pump is every *other Orc creature*, not just yours: the card names no controller. The sacrifice
 * fodder, by contrast, has to be a permanent you control, which is what a sacrifice cost means.
 */
val OrcGeneral = card("Orc General") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Warrior"
    power = 2
    toughness = 2
    oracleText = "{T}, Sacrifice another Orc or Goblin: Other Orc creatures get +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(
                GameObjectFilter.Any.withSubtype(Subtype.ORC) or
                    GameObjectFilter.Any.withSubtype(Subtype.GOBLIN)
            ),
        )
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.ORC), excludeSelf = true),
            Effects.ModifyStats(1, 1, EffectTarget.Self),
        )
        description = "{T}, Sacrifice another Orc or Goblin: Other Orc creatures get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Jesper Myrfors"
        flavorText = "\"Your army must fear you more than the enemy. Only then will you triumph.\" " +
            "—Malga Phlegmtooth"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65a10fd5-506e-46bf-87e6-fde134c0dc04.jpg?1783947933"
    }
}
