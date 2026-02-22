package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daru Spiritualist
 * {1}{W}
 * Creature — Human Cleric
 * 1/1
 * Whenever a Cleric creature you control becomes the target of a spell or ability,
 * it gets +0/+2 until end of turn.
 */
val DaruSpiritualist = card("Daru Spiritualist") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Whenever a Cleric creature you control becomes the target of a spell or ability, it gets +0/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.BecomesTarget(
            GameObjectFilter.Creature.withSubtype(Subtype.CLERIC).youControl()
        )
        effect = Effects.ModifyStats(0, 2, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Dave Dorman"
        flavorText = "He lifts the spirits of his people so they may descend in wrath upon their foes."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18f26b88-cffc-47ed-a70a-7d704a32c8bb.jpg?1562526069"
    }
}
