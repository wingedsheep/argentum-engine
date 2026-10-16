package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GraveyardCreaturesHaveSneak
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ninja Teen
 * {2}{B}
 * Enchantment — Class
 *
 * Whenever a creature you control leaves the battlefield, each opponent loses 1 life.
 * {1}{B}: Level 2 — Creatures you control get +1/+0 and have menace.
 * {B}: Level 3 — Creature cards in your graveyard have sneak {3}{B}. You may cast
 *   creature spells from your graveyard using their sneak abilities.
 */
val NinjaTeen = card("Ninja Teen") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "Whenever a creature you control leaves the battlefield, each opponent loses 1 life.\n" +
        "{1}{B}: Level 2\n" +
        "Creatures you control get +1/+0 and have menace.\n" +
        "{B}: Level 3\n" +
        "Creature cards in your graveyard have sneak {3}{B}.\n" +
        "You may cast creature spells from your graveyard using their sneak abilities."

    // Level 1: each opponent loses 1 life whenever a creature you control leaves.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).leaves()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            listOf(Effects.LoseLife(1, EffectTarget.Controller))
        )
        description = "Whenever a creature you control leaves the battlefield, each opponent loses 1 life."
    }

    classLevel(2, "{1}{B}") {
        staticAbility { ability = ModifyStats(1, 0, GroupFilter.AllCreaturesYouControl) }
        staticAbility { ability = GrantKeyword(Keyword.MENACE, GroupFilter.AllCreaturesYouControl) }
    }

    classLevel(3, "{B}") {
        staticAbility { ability = GraveyardCreaturesHaveSneak(ManaCost.parse("{3}{B}")) }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0825a28f-f60b-4f80-83e3-cad6f9b266ce.jpg?1777939770"
    }
}
