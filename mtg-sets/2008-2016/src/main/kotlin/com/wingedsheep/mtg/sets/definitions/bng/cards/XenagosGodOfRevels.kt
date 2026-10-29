package com.wingedsheep.mtg.sets.definitions.bng.cards

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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Xenagos, God of Revels — Born of the Gods #156
 * {3}{R}{G} · Legendary Enchantment Creature — God · 6/5
 * Indestructible
 * As long as your devotion to red and green is less than seven, Xenagos isn't a creature.
 * At the beginning of combat on your turn, another target creature you control gains haste and
 * gets +X/+X until end of turn, where X is that creature's power.
 *
 * The devotion clause is the two-colour God template (Athreos, Iroas). The pump is Berserk's
 * `ModifyStats(powerOf(target))`, read once as the ability resolves (ruling).
 */
val XenagosGodOfRevels = card("Xenagos, God of Revels") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 5
    oracleText = "Indestructible\n" +
        "As long as your devotion to red and green is less than seven, Xenagos isn't a creature.\n" +
        "At the beginning of combat on your turn, another target creature you control gains haste " +
        "and gets +X/+X until end of turn, where X is that creature's power."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.RED, Color.GREEN),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.ModifyStats(DynamicAmounts.powerOf(creature), DynamicAmounts.powerOf(creature), creature)
        description = "At the beginning of combat on your turn, another target creature you control " +
            "gains haste and gets +X/+X until end of turn, where X is that creature's power."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "156"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3184b138-1109-4195-9d96-4f190164e98b.jpg?1783939521"
        ruling("2014-02-01", "The value of X is calculated only once, as the ability resolves.")
        ruling(
            "2020-11-10",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-11-10",
            "If Xenagos stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-11-10",
            "The type-changing ability that can make Xenagos not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to red and green. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
