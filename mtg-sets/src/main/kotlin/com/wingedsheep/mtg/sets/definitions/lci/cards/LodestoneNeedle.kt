package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Lodestone Needle // Guidestone Compass (CR 702.167, The Lost Caverns of Ixalan #62)
 * {1}{U}
 * Artifact // Artifact
 *
 * Front face — Lodestone Needle ({1}{U}, Artifact)
 *   Flash
 *   When this artifact enters, tap up to one target artifact or creature and put
 *   two stun counters on it.
 *   Craft with artifact {2}{U} ({2}{U}, Exile this artifact, Exile another artifact
 *   you control or an artifact card from your graveyard: Return this card transformed
 *   under its owner's control. Craft only as a sorcery.)
 *
 * Back face — Guidestone Compass (Artifact)
 *   {1}, {T}: Target creature you control explores. Activate only as a sorcery.
 *
 * Implementation:
 *  - Front ETB: "up to one target artifact or creature" is a [TargetPermanent] with
 *    `optional = true` (Queen's Bay Paladin idiom); the effect is a [Effects.Composite]
 *    of [Effects.Tap] plus [AddCountersEffect] with two [Counters.STUN] counters — the
 *    tap + stun-counter pairing proven by Waylaying Pirates. If the target is declined,
 *    both steps are no-ops.
 *  - Craft: the `craft(...)` helper wires [com.wingedsheep.sdk.scripting.AbilityCost.Craft]
 *    with exactly one artifact material (`minCount = 1, maxCount = 1`) plus the {2}{U}
 *    mana cost at sorcery speed; resolution returns the source from exile transformed via
 *    [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect].
 *  - Back activated ability: the predefined Map token's idiom minus the sacrifice —
 *    declared target [Targets.CreatureYouControl], cost `{1}, {T}`
 *    ([Costs.Composite] of [Costs.Mana] + [Costs.Tap]), [Effects.Explore] on the target,
 *    restricted by [TimingRule.SorcerySpeed].
 */

private val LodestoneNeedleFront = card("Lodestone Needle") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "When this artifact enters, tap up to one target artifact or creature and put two stun counters on it.\n" +
        "Craft with artifact {2}{U} ({2}{U}, Exile this artifact, Exile another artifact you control or an artifact card from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    keywords(Keyword.FLASH)

    // When this artifact enters, tap up to one target artifact or creature
    // and put two stun counters on it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "up to one target artifact or creature",
            TargetPermanent(
                optional = true,
                filter = TargetFilter(GameObjectFilter.CreatureOrArtifact)
            )
        )
        effect = Effects.Composite(
            Effects.Tap(permanent),
            AddCountersEffect(counterType = Counters.STUN, count = 2, target = permanent)
        )
    }

    // Craft with artifact {2}{U} — exactly one artifact material.
    craft(
        filter = GameObjectFilter.Artifact,
        cost = "{2}{U}",
        materialDescription = "artifact",
        minCount = 1,
        maxCount = 1
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dedd7a22-92e2-41fd-aa80-944c69653a5e.jpg?1782694560"
    }
}

private val GuidestoneCompass = card("Guidestone Compass") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Target creature you control explores. Activate only as a sorcery. " +
        "(Reveal the top card of your library. Put that card into your hand if it's a land. " +
        "Otherwise, put a +1/+1 counter on that creature, then put the card back or put it into your graveyard.)"

    // {1}, {T}: Target creature you control explores. Activate only as a sorcery.
    // Same shape as the predefined Map token's ability, without the sacrifice.
    activatedAbility {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap
        )
        effect = Effects.Explore(creature)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/back/d/e/dedd7a22-92e2-41fd-aa80-944c69653a5e.jpg?1782694560"
    }
}

val LodestoneNeedle: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LodestoneNeedleFront,
    backFace = GuidestoneCompass
)
