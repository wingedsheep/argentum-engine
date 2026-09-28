package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirror-Shield Hoplite
 * {R}{W}
 * Creature — Human Soldier
 * 2/2
 * Vigilance
 * Whenever a creature you control becomes the target of a backup ability, copy that ability. You
 * may choose new targets for the copy. This ability triggers only once each turn.
 *
 * "That ability" is the targeting object on the stack (`EffectTarget.TargetingSource`). The copy
 * keeps the original's source, so a copy that ends up on the backup creature itself gives it only
 * the counter (CR 707.10; the card's rulings).
 */
val MirrorShieldHoplite = card("Mirror-Shield Hoplite") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    oracleText = "Vigilance\nWhenever a creature you control becomes the target of a backup ability, " +
        "copy that ability. You may choose new targets for the copy. This ability triggers only " +
        "once each turn."
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesTarget(ofBackupAbility = true)
        effect = Effects.CopyTargetTriggeredAbility(EffectTarget.TargetingSource)
        oncePerTurn = true
        description = "Whenever a creature you control becomes the target of a backup ability, copy " +
            "that ability. You may choose new targets for the copy. This ability triggers only once " +
            "each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Alex Brock"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49a5e7de-43cc-488b-99a4-7d173b42d5dc.jpg?1783916941"
    }
}
