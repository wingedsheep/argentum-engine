package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Daring Archaeologist
 * {3}{W}
 * Creature — Human Artificer
 * 3/3
 * When this creature enters, you may return target artifact card from your graveyard to your hand.
 * Whenever you cast a historic spell, put a +1/+1 counter on this creature.
 */
val DaringArchaeologist = card("Daring Archaeologist") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may return target artifact card from your graveyard to your hand.\nWhenever you cast a historic spell, put a +1/+1 counter on this creature. (Artifacts, legendaries, and Sagas are historic.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Artifact.ownedByYou(),
                zone = Zone.GRAVEYARD
            ),
            optional = true
        ))
        effect = MoveToZoneEffect(
            target = t,
            destination = Zone.HAND
        )
    }

    triggeredAbility {
        trigger = Triggers.YouCastHistoric
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/306ebfe1-428d-4f38-950e-b72a44262c25.jpg?1562733552"
        ruling("2018-04-27", "A card, spell, or permanent is historic if it has the legendary supertype, the artifact card type, or the Saga subtype.")
        ruling("2018-04-27", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
