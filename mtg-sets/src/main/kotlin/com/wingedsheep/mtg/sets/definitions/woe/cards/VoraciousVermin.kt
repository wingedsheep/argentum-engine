package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voracious Vermin
 * {2}{B}
 * Creature — Rat
 * 2/1
 *
 * When this creature enters, create a 1/1 black Rat creature token with "This token can't block."
 * Whenever another creature you control dies, put a +1/+1 counter on this creature.
 *
 * The death trigger is a per-creature `OTHER`-binding leaves-battlefield → graveyard trigger, so
 * it fires once for each other creature you control that dies (multiple simultaneous deaths add
 * multiple counters).
 */
val VoraciousVermin = card("Voracious Vermin") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, create a 1/1 black Rat creature token with " +
        "\"This token can't block.\"\n" +
        "Whenever another creature you control dies, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = woeRatToken()
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Milivoj Ćeran"
        flavorText = "Lord Skitter promised the starving rats of Edgewall that they would never go hungry again."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8059be65-3c73-49bb-a3b6-c346ce2f9fa4.jpg?1783915099"
    }
}
