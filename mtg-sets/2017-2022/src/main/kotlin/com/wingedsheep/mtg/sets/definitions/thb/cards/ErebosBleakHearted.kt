package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Erebos, Bleak-Hearted
 * {3}{B}
 * Legendary Enchantment Creature — God
 * 5/6
 * Indestructible
 * As long as your devotion to black is less than five, Erebos isn't a creature.
 * Whenever another creature you control dies, you may pay 2 life. If you do, draw a card.
 * {1}{B}, Sacrifice another creature: Target creature gets -2/-1 until end of turn.
 *
 * The devotion clause is Heliod, Sun-Crowned's God template. The death trigger is Savra's
 * "you may pay 2 life. If you do, draw a card" ([Effects.MayPay] over [Effects.PayLife]); it fires
 * for creatures dying alongside Erebos too (ruling — leaves-the-battlefield look-back). The
 * activated ability is Blazing Hellhound's `SacrificeAnother` cost; it may target the creature
 * being sacrificed (ruling).
 */
val ErebosBleakHearted = card("Erebos, Bleak-Hearted") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 6
    oracleText = "Indestructible\n" +
        "As long as your devotion to black is less than five, Erebos isn't a creature.\n" +
        "Whenever another creature you control dies, you may pay 2 life. If you do, draw a card.\n" +
        "{1}{B}, Sacrifice another creature: Target creature gets -2/-1 until end of turn."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.BLACK), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.MayPay(Effects.PayLife(2), Effects.DrawCards(1))
        description = "Whenever another creature you control dies, you may pay 2 life. If you do, draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-2, -1, creature)
        description = "{1}{B}, Sacrifice another creature: Target creature gets -2/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "93"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e340df19-b89a-403f-9736-4bd30b65de74.jpg?1783931568"
        ruling("2020-01-24", "While resolving Erebos's triggered ability, you can't pay more than 2 life to draw more than one card.")
        ruling(
            "2020-01-24",
            "If one or more creatures you control die at the same time as Erebos, its triggered ability " +
                "triggers for each of those creatures."
        )
        ruling(
            "2020-01-24",
            "Erebos's last ability can target the creature you'll sacrifice to pay its cost. In this case, the " +
                "ability won't resolve, but Erebos's triggered ability will trigger."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
