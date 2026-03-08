package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakshasa Vizier
 * {2}{B}{G}{U}
 * Creature — Demon
 * 4/4
 * Whenever one or more cards are put into exile from your graveyard,
 * put that many +1/+1 counters on Rakshasa Vizier.
 *
 * Note: Each individual card exile triggers separately in the engine,
 * adding 1 counter per card. The net result is identical to the batched
 * "that many" wording.
 */
val RakshasaVizier = card("Rakshasa Vizier") {
    manaCost = "{2}{B}{G}{U}"
    typeLine = "Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Whenever one or more cards are put into exile from your graveyard, put that many +1/+1 counters on Rakshasa Vizier."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(from = Zone.GRAVEYARD, to = Zone.EXILE),
            binding = TriggerBinding.ANY
        ).youControl()
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Nils Hamm"
        flavorText = "Rakshasa offer deals that sound advantageous to those who forget who they are dealing with."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb39e674-919d-4db6-9ac1-cfa1cca02207.jpg?1699601238"
    }
}
