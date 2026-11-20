package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Phenax, God of Deception — Born of the Gods #152
 * {3}{U}{B} · Legendary Enchantment Creature — God · 4/7
 *
 * Indestructible
 * As long as your devotion to blue and black is less than seven, Phenax isn't a creature.
 * Creatures you control have "{T}: Target player mills X cards, where X is this creature's toughness."
 *
 * The devotion clause is the two-colour God template (Karametra, Xenagos, Athreos). The quoted ability
 * is a real granted activated ability over "creatures you control" (Resplendent Mentor's shape), so
 * Phenax grants it to itself while it's a creature (ruling). "This creature" is the granted ability's
 * source — the creature that was tapped — so X is `DynamicAmounts.sourceToughness()`, read as the
 * ability resolves; a creature that left the battlefield is read by last-known information, and a
 * source that isn't a creature then (Phenax tapped, then devotion drops) counts 0 (rulings).
 */
val PhenaxGodOfDeception = card("Phenax, God of Deception") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Enchantment Creature — God"
    power = 4
    toughness = 7
    oracleText = "Indestructible\n" +
        "As long as your devotion to blue and black is less than seven, Phenax isn't a creature.\n" +
        "Creatures you control have \"{T}: Target player mills X cards, where X is this creature's toughness.\""

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.BLUE, Color.BLACK),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val player = target(Targets.Player)
                effect = Patterns.Library.mill(DynamicAmounts.sourceToughness(), player)
                description = "{T}: Target player mills X cards, where X is this creature's toughness."
            },
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "152"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dfcb129-4665-40e4-b5cb-a79f3f40ae5c.jpg?1783939522"
        ruling(
            "2014-02-01",
            "If you activate the ability Phenax grants to creatures you control, the toughness of the creature " +
                "is calculated as the ability resolves. If the creature is no longer on the battlefield at that " +
                "time, use its toughness when it was last on the battlefield."
        )
        ruling("2014-02-01", "If Phenax is a creature, it will grant itself the activated ability.")
        ruling(
            "2014-02-01",
            "If you tap Phenax to activate the ability it grants itself, and Phenax is no longer a creature but " +
                "still on the battlefield as that ability resolves, no cards will be put into the target player's " +
                "graveyard. Similarly, if Phenax isn't on the battlefield as the ability resolves and wasn't a " +
                "creature when it left the battlefield, no cards will be put into the graveyard."
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
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
