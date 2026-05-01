package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spinerock Tyrant
 * {3}{R}{R}
 * Creature — Dragon  6/6
 *
 * Flying
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 * Whenever you cast an instant or sorcery spell with a single target, you may copy it.
 * If you do, those spells gain wither. You may choose new targets for the copy.
 */
val SpinerockTyrant = card("Spinerock Tyrant") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nWither (This deals damage to creatures in the form of -1/-1 counters.)\nWhenever you cast an instant or sorcery spell with a single target, you may copy it. If you do, those spells gain wither. You may choose new targets for the copy."
    power = 6
    toughness = 6

    keywords(Keyword.FLYING, Keyword.WITHER)

    // "Whenever you cast an instant or sorcery spell with a single target, you may copy it.
    //  If you do, those spells gain wither. You may choose new targets for the copy."
    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        triggerCondition = Conditions.TriggeringSpellHasSingleTarget
        optional = true
        effect = Effects.CopyTargetSpell(
            target = EffectTarget.TriggeringEntity,
            keywordsForCopy = listOf(Keyword.WITHER)
        ).then(Effects.GrantKeywordToSpell(Keyword.WITHER, EffectTarget.TriggeringEntity))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Cory Godbey"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/478bbb7a-4b96-4e04-921e-bdf23185de25.jpg?1767862554"
        ruling("2025-11-17", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2025-11-17", "The copy will have the same target unless you choose new ones. If you change the target, the new target must be legal.")
        ruling("2025-11-17", "Any damage dealt to creatures by a source with wither, whether it's combat damage or noncombat damage, is dealt in the form of -1/-1 counters.")
    }
}
