package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fangorn, Tree Shepherd
 * {4}{G}{G}{G}
 * Legendary Creature — Treefolk
 * 4/10
 *
 * Treefolk you control have vigilance.
 * Whenever one or more Treefolk you control attack, add twice that much {G}.
 * You don't lose unspent green mana as steps and phases end.
 */
val FangornTreeShepherd = card("Fangorn, Tree Shepherd") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Treefolk"
    power = 4
    toughness = 10
    oracleText = "Treefolk you control have vigilance.\n" +
        "Whenever one or more Treefolk you control attack, add twice that much {G}.\n" +
        "You don't lose unspent green mana as steps and phases end."

    // Treefolk you control have vigilance.
    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
            GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Treefolk"))
        )
    }

    // Whenever one or more Treefolk you control attack, add twice that much {G}.
    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(
            GameObjectFilter.Creature.youControl().withSubtype("Treefolk")
        )
        effect = Effects.AddDynamicMana(
            amount = DynamicAmount.Multiply(
                DynamicAmounts.battlefield(
                    com.wingedsheep.sdk.scripting.references.Player.You,
                    GameObjectFilter.Creature.attacking().withSubtype("Treefolk")
                ).count(),
                2
            ),
            allowedColors = setOf(Color.GREEN)
        )
    }

    // "You don't lose unspent green mana as steps and phases end." — in this engine mana pools
    // persist between steps and phases (they are emptied only at end of turn), so this clause is
    // already the engine's default behavior. See Grand Warlord Radha for the same modeling.

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Jesper Ejsing"
        flavorText = "\"I am an Ent. Fangorn is my name according to some, Treebeard others make it.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fb0f946-2127-4aa3-88ac-9bd2e94d8983.jpg?1686969365"
    }
}
