package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Urban Retreat
 * Land
 *
 * This land enters tapped.
 * {T}: Add {G}, {W}, or {U}.
 * {2}, Return a tapped creature you control to its owner's hand: Put this card from your
 *   hand onto the battlefield. Activate only as a sorcery.
 *
 * The last ability functions from the hand: it's activated from the hand zone
 * ([activateFromZone] = [Zone.HAND]), its additional cost
 * bounces a tapped creature you control ([Costs.ReturnToHand] over `Creature.tapped()` — the
 * cost is limited to permanents you control), and its effect puts this card (the source, in
 * hand) onto the battlefield. `fromZone = HAND` is the resolution guard: nothing re-checks
 * [activateFromZone] once the ability is on the stack, so without it a card that left the hand in
 * response would still be put onto the battlefield — from wherever it now is.
 *
 * "This land enters tapped" is the [EntersTapped] self-replacement, and it is the *only* place
 * this card says tapped: the from-hand put spells no placement, because the effect-based entry
 * path applies the entering card's own clause too. Sorcery-speed via [TimingRule.SorcerySpeed].
 */
val UrbanRetreat = card("Urban Retreat") {
    manaCost = ""
    colorIdentity = "GWU"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G}, {W}, or {U}.\n" +
        "{2}, Return a tapped creature you control to its owner's hand: Put this card from " +
        "your hand onto the battlefield. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    // {T}: Add {G}, {W}, or {U}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Specific(setOf(Color.GREEN, Color.WHITE, Color.BLUE))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {2}, Return a tapped creature you control to its owner's hand:
    //   Put this card from your hand onto the battlefield. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.ReturnToHand(GameObjectFilter.Creature.tapped())
        )
        activateFromZone = Zone.HAND
        effect = Effects.Move(
            EffectTarget.Self,
            Zone.BATTLEFIELD,
            fromZone = Zone.HAND
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Jonas De Ro"
        flavorText = "For Peter, apartment hunting was about location, location, cost, and location."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2581f320-8238-413d-ab04-d5535da55630.jpg?1783905297"
    }
}
