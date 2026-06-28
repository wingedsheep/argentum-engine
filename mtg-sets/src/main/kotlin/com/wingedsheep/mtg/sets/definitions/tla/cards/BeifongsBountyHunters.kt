package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Beifong's Bounty Hunters
 * {2}{B}{G}
 * Creature — Human Mercenary
 * 4/4
 *
 * Whenever a nonland creature you control dies, earthbend X, where X is that
 * creature's power. (Target land you control becomes a 0/0 creature with haste
 * that's still a land. Put X +1/+1 counters on it. When it dies or is exiled,
 * return it to the battlefield tapped.)
 *
 * The death trigger fires on any nonland creature you control hitting the
 * graveyard from the battlefield (ANY binding). The "nonland" restriction keeps
 * earthbent lands — which are creatures but still lands — from re-triggering when
 * they die. X reads the dying creature's power via last-known information
 * (`EntityReference.Triggering`). Earthbend is a keyword *action* composed from
 * existing primitives via [Effects.Earthbend] (animate land + haste + counters +
 * return-tapped self-triggers), targeting a land you control.
 */
val BeifongsBountyHunters = card("Beifong's Bounty Hunters") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Mercenary"
    power = 4
    toughness = 4
    oracleText = "Whenever a nonland creature you control dies, earthbend X, where X is that creature's power. " +
        "(Target land you control becomes a 0/0 creature with haste that's still a land. Put X +1/+1 counters " +
        "on it. When it dies or is exiled, return it to the battlefield tapped.)"

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = (GameObjectFilter.Creature and GameObjectFilter.Nonland).youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        val land = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(
            DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power),
            land,
        )
        description = "Whenever a nonland creature you control dies, earthbend X, where X is that creature's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Alexandr Leskinen"
        flavorText = "\"Let's take them now.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f88ec3a-44a7-4ae5-a68c-24294dabaeed.jpg?1764121481"
    }
}
