package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wintermoor Commander
 * {W}{B}
 * Creature — Human Knight
 * 2 / *
 *
 * Deathtouch
 * Wintermoor Commander's toughness is equal to the number of Knights you control.
 * Whenever this creature attacks, another target Knight you control gains indestructible until
 * end of turn.
 *
 * Only the toughness is characteristic-defining, so power stays a printed literal and the count
 * goes through [dynamicToughness]. The count includes the Commander itself (it is a Knight).
 */
val WintermoorCommander = card("Wintermoor Commander") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Knight"
    oracleText = "Deathtouch\n" +
        "Wintermoor Commander's toughness is equal to the number of Knights you control.\n" +
        "Whenever this creature attacks, another target Knight you control gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"

    power = 2
    dynamicToughness(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT)).count()
    )

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val knight = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT).youControl(), excludeSelf = true)
        )
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, knight)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06e8d715-025f-4578-b427-e401318a9c58.jpg"
    }
}
