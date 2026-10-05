package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Protector
 * {3}{W}
 * Creature — Angel
 * 2/2
 * Flying
 * Whenever this creature becomes the target of a spell or ability, this creature gets +0/+3 until end of turn.
 */
val AngelicProtector = card("Angelic Protector") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever this creature becomes the target of a spell or ability, this creature gets +0/+3 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesTarget()
        effect = Effects.ModifyStats(0, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "DiTerlizzi"
        flavorText = "\"My family sheltered in her light, the dark was content to wait.\"\n—Crovax"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44faefbe-d5e7-48f3-ba88-833da0b19707.jpg?1783946671"

        ruling(
            "2009-10-01",
            "The ability triggers when Angelic Protector is chosen as a target for a spell or ability. " +
                "The Protectors ability will be put on the stack above that spell or ability, and so resolve first."
        )
    }
}
