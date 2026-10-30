package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hero of the Winds
 * {3}{W}
 * Creature — Human Soldier
 * 1/4
 * Flying
 * Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn.
 *
 * The Hero of the Pride trigger on a flier.
 */
val HeroOfTheWinds = card("Hero of the Winds") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 4
    oracleText = "Flying\nWhenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
        description = "Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Greg Staples"
        flavorText = "To tame a griffin is to ride into legend."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7da95097-dbe7-416a-8571-6ab6d88a531a.jpg?1783931595"
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "If you cast a spell that targets this creature multiple times, this ability triggers just once.")
        ruling("2020-01-24", "This ability doesn't trigger if you copy a spell that targets it, or if a spell's targets are changed to target it.")
        ruling("2020-01-24", "This effect affects only creatures you control at the time the ability resolves. Creatures you begin to control later in the turn won't get +1/+0.")
    }
}
