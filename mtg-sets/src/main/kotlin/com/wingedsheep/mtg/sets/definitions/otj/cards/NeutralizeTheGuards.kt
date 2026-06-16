package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Neutralize the Guards
 * {2}{B}
 * Instant
 *
 * Creatures target opponent controls get -1/-1 until end of turn. Surveil 2.
 *
 * The group debuff is scoped to the chosen opponent via
 * [GameObjectFilter.Creature.targetPlayerControls] + [Patterns.Group.modifyStatsForAll]
 * (see Wail of War). Surveil 2 follows the debuff in the instruction order via
 * [Patterns.Library.surveil].
 */
val NeutralizeTheGuards = card("Neutralize the Guards") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Creatures target opponent controls get -1/-1 until end of turn. Surveil 2. " +
        "(Look at the top two cards of your library, then put any number of them into your " +
        "graveyard and the rest on top of your library in any order.)"

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            Patterns.Group.modifyStatsForAll(
                power = -1,
                toughness = -1,
                filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent))
            ),
            Patterns.Library.surveil(2)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Nereida"
        flavorText = "\"Don't bother getting up. I'll see myself in.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60f1a481-598e-4e05-8471-eedb12a39022.jpg?1712355617"
    }
}
