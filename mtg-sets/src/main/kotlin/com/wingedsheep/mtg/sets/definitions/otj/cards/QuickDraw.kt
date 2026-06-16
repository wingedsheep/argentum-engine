package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Quick Draw
 * {R}
 * Instant
 *
 * Target creature you control gets +1/+1 and gains first strike until end of turn.
 * Creatures target opponent controls lose first strike and double strike until end of turn.
 */
val QuickDraw = card("Quick Draw") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+1 and gains first strike until end of turn. " +
        "Creatures target opponent controls lose first strike and double strike until end of turn."

    spell {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            Effects.ModifyStats(power = 1, toughness = 1, target = creature),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, target = creature),
            Patterns.Group.removeKeywordFromAll(
                keyword = Keyword.FIRST_STRIKE,
                filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent))
            ),
            Patterns.Group.removeKeywordFromAll(
                keyword = Keyword.DOUBLE_STRIKE,
                filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Lie Setiawan"
        flavorText = "The opening of the Omenpaths awoke the wild magic of Thunder Junction. " +
            "Settlers dubbed it \"thunder\" and put it to use in everything from farming tools to deadly weapons."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56399cd0-1214-42b6-be38-f2cbd770915f.jpg?1712355816"
    }
}
