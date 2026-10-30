package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Karametra's Blessing
 * {W}
 * Instant
 * Target creature gets +2/+2 until end of turn. If it's an enchanted creature or enchantment
 * creature, it also gains hexproof and indestructible until end of turn.
 *
 * The enchanted/enchantment check is read once, as the spell resolves (the ruling), so it is an
 * [Effects.If] over the target rather than a conditional static.
 */
val KarametrasBlessing = card("Karametra's Blessing") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's an enchanted creature or " +
        "enchantment creature, it also gains hexproof and indestructible until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control. Damage and effects " +
        "that say \"destroy\" don't destroy it.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then Effects.If(
            Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.enchanted() or
                    GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsEnchantment)
            ),
            Effects.GrantKeyword(Keyword.HEXPROOF, t) then Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88c8e4dc-5378-48d6-85b2-f5ea9ec7cf36.jpg?1783931592"
        ruling(
            "2020-01-24",
            "Whether the target creature is enchanted or an enchantment is checked only while Karametra's " +
                "Blessing is resolving. If it becomes enchanted or stops being enchanted later (or, in some " +
                "unusual cases, becomes or ceases to be an enchantment later) it won't gain or lose hexproof " +
                "and indestructible."
        )
    }
}
