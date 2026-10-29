package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Keranos, God of Storms
 * {3}{U}{R}
 * Legendary Enchantment Creature — God
 * 6/5
 *
 * Indestructible
 * As long as your devotion to blue and red is less than seven, Keranos isn't a creature.
 * Reveal the first card you draw on each of your turns. Whenever you reveal a land card this way,
 * draw a card. Whenever you reveal a nonland card this way, Keranos deals 3 damage to any target.
 *
 * The reveal is Primitive Etchings' [RevealFirstDrawEachTurn] scoped to your own turns; the two
 * "this way" triggers are linked to Keranos's own reveal, so another reveal source (Etchings on an
 * opponent's turn) doesn't fire them.
 */
val KeranosGodOfStorms = card("Keranos, God of Storms") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to blue and red is less than seven, Keranos isn't a creature.\n" +
        "Reveal the first card you draw on each of your turns. Whenever you reveal a land card this way, " +
        "draw a card. Whenever you reveal a nonland card this way, Keranos deals 3 damage to any target."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.BLUE, Color.RED),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility { ability = RevealFirstDrawEachTurn(yourTurnsOnly = true) }

    triggeredAbility {
        trigger = Triggers.you.revealsFirstDraw(GameObjectFilter.Land)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.revealsFirstDraw(GameObjectFilter.Nonland)
        val damageTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, damageTarget)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab70c262-37a9-4dcd-80bb-d4422368eade.jpg?1783939404"
        ruling(
            "2014-04-26",
            "If you reveal a land card this way, Keranos's triggered ability will cause you to draw another card."
        )
        ruling(
            "2020-01-24",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The abilities of Gods function as long as they're on the battlefield, regardless of whether they're creatures."
        )
    }
}
