package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Arid Archway
 * Land — Desert
 *
 * This land enters tapped.
 * When this land enters, return a land you control to its owner's hand. If another Desert was
 * returned this way, surveil 1.
 * {T}: Add {C}{C}.
 *
 * "Return a land you control" is the controller's choice, modeled as a target constrained to their
 * own lands (a self-bounce that cannot fizzle), like the Karoo bounce lands. "If another Desert
 * was returned this way" is evaluated against the chosen land *before* the bounce (it leaves the
 * battlefield, so its subtype can't be read afterward): surveil 1 fires only when the returned land
 * is a Desert AND is not Arid Archway itself ([Conditions.Not] of [Conditions.TargetIsSource]).
 */
val AridArchway = card("Arid Archway") {
    typeLine = "Land — Desert"
    colorIdentity = ""
    oracleText = "This land enters tapped.\n" +
        "When this land enters, return a land you control to its owner's hand. If another Desert " +
        "was returned this way, surveil 1. (Look at the top card of your library. You may put it " +
        "into your graveyard.)\n" +
        "{T}: Add {C}{C}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target(
            "a land you control",
            TargetPermanent(filter = TargetFilter.Land.youControl()),
        )
        // Evaluate the "another Desert" check while the land is still on the battlefield, then
        // bounce + surveil (true branch) or just bounce (false branch).
        effect = ConditionalEffect(
            condition = Conditions.All(
                Conditions.TargetMatchesFilter(GameObjectFilter.Land.withSubtype(Subtype.DESERT)),
                Conditions.Not(Conditions.TargetIsSource()),
            ),
            effect = Effects.ReturnToHand(land).then(Patterns.Library.surveil(1)),
            elseEffect = Effects.ReturnToHand(land),
        )
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Raymond Bonilla"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f8c8fa2-12ab-4f6a-9f7a-2bc69e9ba024.jpg?1713223021"
    }
}
