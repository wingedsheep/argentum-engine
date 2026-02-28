package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Forgotten Ancient
 * {3}{G}
 * Creature — Elemental
 * 0/3
 * Whenever a player casts a spell, you may put a +1/+1 counter on Forgotten Ancient.
 * At the beginning of your upkeep, you may move any number of +1/+1 counters from
 * Forgotten Ancient onto other creatures.
 */
val ForgottenAncient = card("Forgotten Ancient") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 3
    oracleText = "Whenever a player casts a spell, you may put a +1/+1 counter on Forgotten Ancient.\nAt the beginning of your upkeep, you may move any number of +1/+1 counters from Forgotten Ancient onto other creatures."

    triggeredAbility {
        trigger = TriggerSpec(
            event = SpellCastEvent(player = Player.Each),
            binding = TriggerBinding.ANY
        )
        effect = MayEffect(
            AddCountersEffect(
                counterType = "+1/+1",
                count = 1,
                target = EffectTarget.Self
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = MayEffect(Effects.DistributeCountersFromSelf("+1/+1"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Mark Tedin"
        flavorText = "Its blood is life. Its body is growth."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49d3b91d-2e4f-4574-89f8-7b804f1d21bf.jpg?1562528527"
        ruling("2022-12-08", "Forgotten Ancient's first ability will resolve before the spell that caused it to trigger. Putting a +1/+1 counter on Forgotten Ancient is optional.")
        ruling("2022-12-08", "Forgotten Ancient's last ability doesn't target any creatures. You choose how many +1/+1 counters will be moved (and onto which creatures) as the ability resolves.")
    }
}
