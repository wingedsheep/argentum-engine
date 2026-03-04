package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Celestial Gatekeeper
 * {3}{W}{W}
 * Creature — Bird Cleric
 * 2/2
 * Flying
 * When Celestial Gatekeeper dies, exile it, then return up to two target Bird and/or Cleric
 * permanent cards from your graveyard to the battlefield.
 */
val CelestialGatekeeper = card("Celestial Gatekeeper") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Bird Cleric"
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsPermanent,
                        CardPredicate.Or(listOf(
                            CardPredicate.HasSubtype(Subtype.BIRD),
                            CardPredicate.HasSubtype(Subtype.CLERIC)
                        ))
                    )
                ).ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.EXILE) then
            ForEachTargetEffect(
                effects = listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD))
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b4dc1d3-53a1-411b-abf9-f5e4e80edc63.jpg?1562897265"
        ruling("2004-10-04", "You can choose to target this card as one of the two Bird and/or Cleric cards. If you do, it gets exiled and does not get returned to the battlefield.")
    }
}
