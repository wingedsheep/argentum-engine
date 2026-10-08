package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Aang, Airbending Master
 * {4}{W}
 * Legendary Creature — Human Avatar Ally
 * 4/4
 *
 * When Aang enters, airbend another target creature. (Exile it. While it's exiled, its owner may
 * cast it for {2} rather than its mana cost.)
 * Whenever one or more creatures you control leave the battlefield without dying, you get an
 * experience counter.
 * At the beginning of your upkeep, create a 1/1 white Ally creature token for each experience
 * counter you have.
 *
 * - The ETB is the set's target-agnostic [Effects.Airbend] (CR 701.65a) over "another target
 *   creature" ([TargetOther] — any controller, not optional).
 * - "One or more … leave the battlefield without dying" is the Dour Port-Mage batch trigger
 *   (`leaveWithoutDying()`), here *not* "other" — Aang leaving alongside them still counts.
 * - Experience counters live on the player ([CounterType.EXPERIENCE], CR 122.1), so they outlast
 *   Aang and are shared with every other source (the rulings).
 * - The upkeep payoff counts them at resolution.
 */
val AangAirbendingMaster = card("Aang, Airbending Master") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Avatar Ally"
    power = 4
    toughness = 4
    oracleText = "When Aang enters, airbend another target creature. (Exile it. While it's exiled, its owner may cast it for {2} rather than its mana cost.)\n" +
        "Whenever one or more creatures you control leave the battlefield without dying, you get an experience counter.\n" +
        "At the beginning of your upkeep, create a 1/1 white Ally creature token for each experience counter you have."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetOther(baseRequirement = TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.Airbend()
        description = "When Aang enters, airbend another target creature."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.youControl()).leaveWithoutDying()
        effect = Effects.AddCounters(CounterType.EXPERIENCE, 1, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            count = DynamicAmounts.playerCounterCount(CounterType.EXPERIENCE),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf(Subtype.ALLY.value),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "74"
        artist = "Tomoyo Asatani"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de7a150b-1b0d-4928-a2cc-80a4b7412350.jpg?1783904837"
        ruling("2025-10-02", "All experience counters are identical, no matter how you got them. For example, the last ability will count experience counters that you got from the second ability, from another ability, from another copy of Aang, Airbending Master, and so on.")
        ruling("2025-10-02", "The experience counter goes on you, the player, not on Aang. You will keep that counter even if Aang, Airbending Master dies.")
        ruling("2025-10-02", "Tokens exiled this way will cease to exist and cannot be cast.")
        ruling("2025-10-02", "Lands exiled this way cannot be played from exile. (Lands that were animated by earthbend will be returned to the battlefield tapped when they're exiled.)")
    }
}
