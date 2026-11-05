package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nissa's Defeat
 * {2}{G}
 * Sorcery
 * Destroy target Forest, green enchantment, or green planeswalker. If that permanent was a Nissa
 * planeswalker, draw a card.
 *
 * "Was" is read before the destroy: the gate checks the target while it is still on the
 * battlefield, and both branches destroy it, so the draw doesn't depend on the destruction
 * succeeding.
 */
val NissasDefeat = card("Nissa's Defeat") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target Forest, green enchantment, or green planeswalker. If that permanent was a " +
        "Nissa planeswalker, draw a card."

    spell {
        val victim = target(
            TargetFilter(
                GameObjectFilter.Land.withSubtype(Subtype.FOREST) or
                    GameObjectFilter.Enchantment.withColor(Color.GREEN) or
                    GameObjectFilter.Planeswalker.withColor(Color.GREEN)
            )
        )
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                victim,
                GameObjectFilter.Planeswalker.withSubtype(Subtype.NISSA)
            ),
            then = Effects.Destroy(victim) then Effects.DrawCards(1),
            otherwise = Effects.Destroy(victim)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Kieran Yanner"
        flavorText = "\"The soul of this world is gone, Nissa, and I would gladly kill it again.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86326456-d188-4083-9141-a639b821b291.jpg?1783936018"
        ruling(
            "2017-07-14",
            "Most nonbasic lands that produce green mana aren't Forests. For example, Desert of the " +
                "Indomitable isn't a Forest. Some nonbasic lands (such as Sheltered Thicket from the Amonkhet " +
                "set) do have basic land types printed on the type line and may be Forests."
        )
    }
}
