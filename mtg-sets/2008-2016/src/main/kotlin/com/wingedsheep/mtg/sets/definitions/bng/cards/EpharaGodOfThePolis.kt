package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ephara, God of the Polis — Born of the Gods #145
 * {2}{W}{U} · Legendary Enchantment Creature — God · 6/5
 * Indestructible
 * As long as your devotion to white and blue is less than seven, Ephara isn't a creature.
 * At the beginning of each upkeep, if you had another creature enter the battlefield under your
 * control last turn, draw a card.
 *
 * The devotion clause is the two-colour God template (Mogis, Athreos). The upkeep trigger fires on
 * every player's upkeep and reads the previous turn's entry log
 * ([DynamicAmounts.cardTypeEnteredUnderControlLastTurn]): one draw however many creatures entered,
 * whatever has happened to them since, and only permanents that were creatures as they entered
 * (rulings). `excludeSource` keeps Ephara's own entry as a creature from counting ("another").
 */
val EpharaGodOfThePolis = card("Ephara, God of the Polis") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to white and blue is less than seven, Ephara isn't a creature.\n" +
        "At the beginning of each upkeep, if you had another creature enter the battlefield under " +
        "your control last turn, draw a card."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.WHITE, Color.BLUE),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.cardTypeEnteredUnderControlLastTurn(CardType.CREATURE, excludeSource = true),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.DrawCards(1)
        description = "At the beginning of each upkeep, if you had another creature enter the " +
            "battlefield under your control last turn, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6832e495-7ee9-43e0-94ea-03c88344080e.jpg?1783939528"
        ruling(
            "2014-02-01",
            "Ephara's last ability checks at the beginning of each upkeep whether another creature entered the " +
                "battlefield under your control last turn. If one did, it will trigger; otherwise, it won't. The " +
                "ability will trigger only once no matter how many creatures entered the battlefield under your " +
                "control that turn, as long as at least one did."
        )
        ruling(
            "2014-02-01",
            "The last ability will trigger regardless of what has happened to the creature that entered the " +
                "battlefield on the previous turn. It doesn't matter whether it's still under your control or " +
                "whether it's still on the battlefield."
        )
        ruling(
            "2014-02-01",
            "If a noncreature permanent you control (such as an Aura with bestow) becomes a creature, it will " +
                "not cause Ephara's last ability to trigger the following turn. This is true even if that " +
                "noncreature permanent became a creature the same turn it entered the battlefield."
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
