package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fires of Invention
 * {3}{R}
 * Enchantment
 *
 * You can cast spells only during your turn and you can cast no more than two spells each turn.
 * You may cast spells with mana value less than or equal to the number of lands you control
 * without paying their mana costs.
 *
 * The first ability is two controller-scoped prohibitions: [PlayersCantCastSpells] while it isn't
 * your turn, and a [RestrictSpellsCastPerTurn] cap of two (which counts the whole turn, so spells
 * cast before Fires entered count). The second is a controller-only
 * [MayCastWithoutPayingManaCost] whose filter caps mana value at the lands you control, resolved
 * when the cast is checked.
 */
val FiresOfInvention = card("Fires of Invention") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "You can cast spells only during your turn and you can cast no more than two spells each turn.\n" +
        "You may cast spells with mana value less than or equal to the number of lands you control without paying their mana costs."

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.You, condition = IsNotYourTurn)
    }

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 2)
    }

    staticAbility {
        ability = MayCastWithoutPayingManaCost(
            controllerOnly = true,
            spellFilter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.ManaValueAtMostDynamic(DynamicAmounts.landsYouControl())
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Stanton Feng"
        flavorText = "\"Just tell us how many you want and get out of the way.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a12b16b0-f75f-42d8-9b24-947c1908e0f7.jpg?1783932623"
        ruling(
            "2019-10-04",
            "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost."
        )
        ruling(
            "2019-10-04",
            "If an effect allows or instructs you to cast a spell during another player's turn, you can't do so. The restriction of Fires of Invention takes precedence over that permission. Similarly, if an effect allows or instructs you to cast a third spell during your turn, you can't do so."
        )
        ruling(
            "2019-10-04",
            "Fires of Invention will look at the entire turn to see if you've cast two spells yet that turn, even if Fires of Invention wasn't on the battlefield when those spells were cast. Notably, you can't cast Fires of Invention and then cast two more spells that turn; if Fires of Invention was your second (or third, or fourth) spell that turn, you can't cast another spell at all."
        )
        ruling(
            "2019-10-04",
            "You may choose to pay the mana cost or an alternative cost for spells you cast, even if their mana value is less than or equal to the number of lands you control."
        )
    }
}
