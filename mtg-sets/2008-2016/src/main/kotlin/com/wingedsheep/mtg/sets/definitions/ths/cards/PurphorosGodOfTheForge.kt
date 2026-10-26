package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Purphoros, God of the Forge
 * {3}{R}
 * Legendary Enchantment Creature — God
 * 6/5
 *
 * Indestructible
 * As long as your devotion to red is less than five, Purphoros isn't a creature.
 * Whenever another creature you control enters, Purphoros deals 2 damage to each opponent.
 * {2}{R}: Creatures you control get +1/+0 until end of turn.
 *
 * The god clause is Nylea's Layer 4 [RemoveCardType] of CREATURE gated by `DevotionTo(RED) < 5`.
 * The damage trigger works whether or not Purphoros is a creature (its abilities function on the
 * battlefield either way); Purphoros is the damage source. The pump snapshots the creatures you
 * control as it resolves.
 */
val PurphorosGodOfTheForge = card("Purphoros, God of the Forge") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to red is less than five, Purphoros isn't a creature.\n" +
        "Whenever another creature you control enters, Purphoros deals 2 damage to each opponent.\n" +
        "{2}{R}: Creatures you control get +1/+0 until end of turn."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(DynamicAmounts.devotionTo(Color.RED), ComparisonOperator.LT, 5)
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.DealDamage(
            2,
            EffectTarget.PlayerRef(Player.EachOpponent),
            damageSource = EffectTarget.Self
        )
        description = "Whenever another creature you control enters, Purphoros deals 2 damage to each opponent."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl)
        description = "{2}{R}: Creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "135"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf6baf2-d20b-467d-8929-abefcf7dfa99.jpg?1783939755"
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "If a God is attacking or blocking and it stops being a creature, it will be removed from combat. It " +
                "won't rejoin combat if it resumes being a creature later during that combat."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
        ruling(
            "2020-01-24",
            "When a God enters the battlefield, your devotion to its color (including the mana symbols in the mana " +
                "cost of the God itself) will determine if a creature entered the battlefield or not for abilities " +
                "that trigger whenever a creature enters the battlefield."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count toward " +
                "your devotion to their color(s)."
        )
    }
}
