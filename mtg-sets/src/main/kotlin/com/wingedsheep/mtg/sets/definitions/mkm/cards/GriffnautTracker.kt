package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Griffnaut Tracker — Murders at Karlov Manor #17
 * {3}{W} · Creature — Human Detective · 3/2
 *
 * Flying
 * When this creature enters, exile up to two target cards from a single graveyard.
 *
 * "From a single graveyard" is the [TargetObject.sameOwner] flag, the same modeling as Arashin
 * Sunshield: both chosen cards must be owned by the same player, so the two targets can't be split
 * across two opponents' graveyards. It is a *targeting* restriction, checked when targets are chosen
 * and re-checked on resolution — not a resolution-time filter.
 *
 * "Up to two" makes the whole ability optional in the targeting sense (`optional = true`): it still
 * goes on the stack with zero targets when every graveyard is empty, and it resolves doing nothing
 * rather than being removed for lack of targets (CR 608.2b).
 */
val GriffnautTracker = card("Griffnaut Tracker") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Detective"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, exile up to two target cards from a single graveyard."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "up to two target cards from a single graveyard",
            TargetObject(
                count = 2,
                optional = true,
                filter = TargetFilter.CardInGraveyard,
                sameOwner = true,
            )
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE))
        )
        description = "When this creature enters, exile up to two target cards from a single graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Svetlin Velinov"
        flavorText = "\"The desperate will tread unexpected paths. Be prepared to follow.\"\n" +
            "—Tam Sennic, Ezrim's second-in-command"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95f5d048-226f-49a4-a2ce-a6fa99aa9e8a.jpg?1783912926"
    }
}
