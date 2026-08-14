package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Molten Man, Inferno Incarnate
 * {2}{R}
 * Legendary Creature — Elemental Villain
 * 0/0
 * When Molten Man enters, search your library for a basic Mountain card, put it onto
 * the battlefield tapped, then shuffle.
 * Molten Man gets +1/+1 for each Mountain you control.
 * When Molten Man leaves the battlefield, sacrifice a land.
 */
val MoltenManInfernoIncarnate = card("Molten Man, Inferno Incarnate") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Elemental Villain"
    power = 0
    toughness = 0
    oracleText = "When Molten Man enters, search your library for a basic Mountain card, put it onto " +
        "the battlefield tapped, then shuffle.\n" +
        "Molten Man gets +1/+1 for each Mountain you control.\n" +
        "When Molten Man leaves the battlefield, sacrifice a land."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.MOUNTAIN),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "When Molten Man enters, search your library for a basic Mountain card, " +
            "put it onto the battlefield tapped, then shuffle."
    }

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)
            ).count(),
            toughnessBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)
            ).count()
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.Sacrifice(GameObjectFilter.Land, count = 1, target = EffectTarget.Controller)
        description = "When Molten Man leaves the battlefield, sacrifice a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Lie Setiawan"
        flavorText = "Is this it? Is this the end of Spider-Man?"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f469d621-25d0-4d8e-909f-47dac0b9c5b0.jpg?1783905334"
    }
}
