package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hero of the Pride
 * {1}{W}
 * Creature — Cat Soldier
 * 2/2
 * Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn.
 *
 * Gnarlback Rhino's cast-time "spell that targets this creature" trigger
 * ([SpellCastPredicate.TargetsSource]) feeding Inner-Flame Igniter's team pump. The group is
 * snapshotted at resolution, so creatures you gain control of later don't get the bonus.
 */
val HeroOfThePride = card("Hero of the Pride") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
        description = "Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Wisnu Tan"
        flavorText = "Leonin fight for neither god nor polis; their loyalty is to their pride."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81b56863-a52a-4559-a662-f6d9145d5804.jpg?1783931596"
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "If you cast a spell that targets this creature multiple times, this ability triggers just once.")
        ruling("2020-01-24", "This ability doesn't trigger if you copy a spell that targets it, or if a spell's targets are changed to target it.")
        ruling("2020-01-24", "This effect affects only creatures you control at the time the ability resolves. Creatures you begin to control later in the turn won't get +1/+0.")
    }
}
