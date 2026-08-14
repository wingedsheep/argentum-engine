package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Lagorin, Soul of Alacria — Aetherdrift #211
 * {G}{W} · Legendary Creature — Beast Mount · 1/1
 *
 * Flying
 * Whenever Lagorin attacks while saddled, put a +1/+1 counter on each of up to two target
 * Mounts and/or Vehicles.
 * Saddle 1
 *
 * "Attacks while saddled" is [Triggers.Attacks] gated by [Conditions.SourceIsSaddled] as an
 * intervening-if (CR 603.4) — per the printed rulings the ability triggers only if Lagorin was
 * saddled *when it was declared as an attacker*, which is exactly when the attack trigger is
 * checked. Same shape as Brightfield Glider and the rest of the DFT saddled-attacker cycle.
 *
 * The reward targets *permanents*, not creatures: an uncrewed Vehicle is a legal target even
 * though it isn't a creature, so the filter is [GameObjectFilter.Permanent] over the Mount /
 * Vehicle subtype union rather than `GameObjectFilter.Creature`. There is no "you control"
 * clause — an opponent's Mount is a legal (if unlikely) choice.
 *
 * `optional = true` on a `count = 2` requirement is the SDK's "up to two" shape, so the
 * controller may pick zero, one, or two; [ForEachTargetEffect] then applies one counter per
 * chosen target, each rebound as `ContextTarget(0)`.
 */
private val MountsAndVehicles = GameObjectFilter.Permanent
    .withAnyOfSubtypes(listOf(Subtype("Mount"), Subtype.VEHICLE))

val LagorinSoulOfAlacria = card("Lagorin, Soul of Alacria") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Beast Mount"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever Lagorin attacks while saddled, put a +1/+1 counter on each of up to two " +
        "target Mounts and/or Vehicles.\n" +
        "Saddle 1 (Tap any number of other creatures you control with total power 1 or more: " +
        "This Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        target = TargetPermanent(
            count = 2,
            optional = true,
            filter = TargetFilter(MountsAndVehicles),
            id = "two target Mounts and/or Vehicles"
        )
        effect = ForEachTargetEffect(
            listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
        )
        description = "Whenever Lagorin attacks while saddled, put a +1/+1 counter on each of " +
            "up to two target Mounts and/or Vehicles."
    }

    keywordAbility(KeywordAbility.saddle(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b04e3f4-103b-4845-b3f0-5417971c7666.jpg?1783907856"
        ruling(
            "2025-02-07",
            "An ability that triggers when a creature \"attacks while saddled\" will trigger only " +
                "if that creature was saddled when it was declared as an attacker."
        )
    }
}
