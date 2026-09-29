package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Kami of Whispered Hopes
 * {2}{G}
 * Creature — Spirit
 * 1/1
 * If one or more +1/+1 counters would be put on a permanent you control, that many plus one
 * +1/+1 counters are put on that permanent instead.
 * {T}: Add X mana of any one color, where X is this creature's power.
 *
 * Hardened Scales' replacement widened to "a permanent you control" (any permanent, not just a
 * creature), gated on the recipient only — who places the counters doesn't matter.
 */
val KamiOfWhisperedHopes = card("Kami of Whispered Hopes") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "If one or more +1/+1 counters would be put on a permanent you control, that many plus one +1/+1 counters are put on that permanent instead.\n" +
        "{T}: Add X mana of any one color, where X is this creature's power."

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                recipient = Recipient.PermanentYouControl,
            ),
        )
    )

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(DynamicAmounts.sourcePower())
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Filipe Pagliuso"
        flavorText = "Nashi knelt reverently before the kami and envisioned his family reunited."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c644650-3861-4a78-9e39-a413b073ddac.jpg?1783916966"
    }
}
