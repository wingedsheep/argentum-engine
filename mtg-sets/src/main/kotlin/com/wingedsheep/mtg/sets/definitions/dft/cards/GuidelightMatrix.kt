package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Guidelight Matrix — Aetherdrift #233
 * {2} · Artifact
 *
 * When this artifact enters, draw a card.
 * {2}, {T}: Target Mount you control becomes saddled until end of turn. Activate only as a sorcery.
 * {2}, {T}: Target Vehicle you control becomes an artifact creature until end of turn.
 *
 * Two sides of the same crew/saddle enabler, each a `{2}, {T}` ability:
 *  - The Mount half resolves to [Effects.BecomeSaddled], the marker half of a Saddle ability
 *    (CR 702.171b) — no P/T or type change, just the until-end-of-turn saddled status that
 *    "whenever this Mount becomes saddled" / saddled-gated abilities read. It carries the printed
 *    "Activate only as a sorcery" ([TimingRule.SorcerySpeed]), matching real Saddle timing.
 *  - The Vehicle half adds the Creature card type for the turn ([Effects.AddCardType] with
 *    [Duration.EndOfTurn]). A Vehicle is already an artifact (CR 301.7) and its printed P/T and
 *    keywords apply automatically once it's a creature, so the type grant alone is the whole
 *    animation — the same Layer-4 change crew makes. This half has *no* timing restriction, so it
 *    can animate a Vehicle as a combat trick or to soak up damage.
 */
val GuidelightMatrix = card("Guidelight Matrix") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{2}, {T}: Target Mount you control becomes saddled until end of turn. Activate only as a sorcery.\n" +
        "{2}, {T}: Target Vehicle you control becomes an artifact creature until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val mount = target(
            "target Mount you control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.withSubtype(Subtype("Mount")).youControl()
                )
            )
        )
        effect = Effects.BecomeSaddled(mount)
        timing = TimingRule.SorcerySpeed
        description = "{2}, {T}: Target Mount you control becomes saddled until end of turn. " +
            "Activate only as a sorcery."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val vehicle = target(
            "target Vehicle you control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl()
                )
            )
        )
        effect = Effects.AddCardType("Creature", vehicle, Duration.EndOfTurn)
        description = "{2}, {T}: Target Vehicle you control becomes an artifact creature until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "233"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cccf7fb5-c043-4a1f-ad2f-edb280cb5037.jpg?1783907849"
    }
}
