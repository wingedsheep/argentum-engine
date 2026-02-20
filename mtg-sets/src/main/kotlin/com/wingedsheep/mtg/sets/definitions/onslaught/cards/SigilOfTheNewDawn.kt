package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Sigil of the New Dawn
 * {3}{W}
 * Enchantment
 * Whenever a creature is put into your graveyard from the battlefield,
 * you may pay {1}{W}. If you do, return that card to your hand.
 */
val SigilOfTheNewDawn = card("Sigil of the New Dawn") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature is put into your graveyard from the battlefield, you may pay {1}{W}. If you do, return that card to your hand."

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{W}"),
            effect = MoveToZoneEffect(EffectTarget.TriggeringEntity, Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca1babca-b285-4b00-8b46-ed946c9a027f.jpg?1587857620"
    }
}
