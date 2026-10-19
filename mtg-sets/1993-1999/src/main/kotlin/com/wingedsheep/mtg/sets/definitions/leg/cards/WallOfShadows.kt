package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeTargetedBy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.UntargetableBy
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Wall of Shadows
 * {1}{B}{B}
 * Creature — Wall
 * 0/1
 *
 * Defender (This creature can't attack.)
 * Prevent all damage that would be dealt to this creature by creatures it's blocking.
 * This creature can't be the target of spells that can target only Walls or of abilities that
 * can target only Walls.
 *
 * The prevention is a static [PreventDamage] whose source filter is `blockedBySource()` — live
 * combat state, so only the attackers this Wall is blocking right now; damage from other
 * creatures, or from a creature it blocked earlier but that has since left combat, still lands.
 */
val WallOfShadows = card("Wall of Shadows") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 1
    oracleText = "Defender (This creature can't attack.)\n" +
        "Prevent all damage that would be dealt to this creature by creatures it's blocking.\n" +
        "This creature can't be the target of spells that can target only Walls or of abilities that can " +
        "target only Walls."

    keywords(Keyword.DEFENDER)

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature.blockedBySource(),
            )
        )
    )

    staticAbility {
        ability = CantBeTargetedBy(UntargetableBy.TargetingOnly(Subtype.WALL))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb351900-cffd-4d23-b82f-5fb12a4874d9.jpg?1783948060"
    }
}
