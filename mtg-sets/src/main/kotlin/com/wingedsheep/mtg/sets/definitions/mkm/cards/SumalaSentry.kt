package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sumala Sentry — Murders at Karlov Manor #233
 * {G}{W} · Creature — Elf Archer · 1/3
 *
 * Reach
 * Whenever a face-down permanent you control is turned face up, put a +1/+1 counter on it and a
 * +1/+1 counter on this creature.
 *
 * The GW disguise payoff: every flip is two counters, one on the flipped permanent and one here.
 * Both halves are unconditional and neither is a target, so nothing fizzles if the flipped
 * permanent leaves in response — the trigger just does as much as it can.
 *
 * Oracle says "face-down *permanent*", modelled as [Triggers.CreatureTurnedFaceUp]. That is not a
 * narrowing: a face-down permanent on the battlefield is always a 2/2 colorless creature with no
 * name, types, or abilities (CR 708.2), so every face-down permanent you control *is* a face-down
 * creature you control at the moment the flip happens. The binding is ANY, so Sumala Sentry
 * triggers on itself being turned face up too (it has no disguise, but manifest and cloak can put
 * it onto the battlefield face down) — again matching the oracle wording, which doesn't say
 * "another".
 *
 * "Put a +1/+1 counter on it" uses [EffectTarget.TriggeringEntity]: the permanent named by the
 * event, not a chosen target. Order in the [Effects.Composite] is printed order, which matters
 * only for watchers counting counter placements.
 */
val SumalaSentry = card("Sumala Sentry") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elf Archer"
    oracleText = "Reach\n" +
        "Whenever a face-down permanent you control is turned face up, put a +1/+1 counter on it " +
        "and a +1/+1 counter on this creature."
    power = 1
    toughness = 3

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.CreatureTurnedFaceUp()
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "Put a +1/+1 counter on that permanent and a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Nicholas Elias"
        flavorText = "The great meditation garden of Sumala was no place for Ravnica's rising " +
            "crime wave, and he would keep it that way."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b9d4691-59d1-4e97-9b5d-8017788fbcb3.jpg?1783912836"

        ruling(
            "2024-02-02",
            "If the permanent that was turned face up is no longer on the battlefield when Sumala " +
                "Sentry's triggered ability resolves, you'll still put a +1/+1 counter on Sumala " +
                "Sentry. Similarly, if Sumala Sentry is no longer on the battlefield, you'll " +
                "still put a +1/+1 counter on the permanent that turned face up."
        )
    }
}
