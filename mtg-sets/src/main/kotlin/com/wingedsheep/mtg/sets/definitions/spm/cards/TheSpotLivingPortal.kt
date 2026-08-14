package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Spot, Living Portal
 * {3}{W}{B}
 * Legendary Creature — Human Scientist Villain
 * 4/4
 *
 * When The Spot enters, exile up to one target nonland permanent and up to one target nonland
 * permanent card from a graveyard.
 * When The Spot dies, put him on the bottom of his owner's library. If you do, return the exiled
 * cards to their owners' hands.
 *
 * Linked-exile shape (Banisher Priest / Fiend Hunter family, but with two exiled cards and a
 * bottom-of-library dies clause):
 *  - The ETB has two **independent optional** targets (CR 115.1) — a nonland permanent on the
 *    battlefield and a nonland permanent card in any graveyard — each modeled as an `optional`
 *    [TargetObject]. [Effects.ExileUntilLeaves] exiles each and records it in The Spot's
 *    `LinkedExileComponent`; the executor exiles a graveyard card just as well as a battlefield
 *    permanent (same primitive Savior of Ollenbock uses). Both exiled cards accumulate on the one
 *    source pile. There is deliberately **no** LeavesBattlefield return trigger: if The Spot leaves
 *    the battlefield any way other than dying-and-bottoming (exiled, bounced), the cards stay
 *    exiled, matching the oracle.
 *  - The dies clause is a self-move plus a linked return, gated with [Effects.IfYouDo]: the action
 *    [Effects.PutOnBottomOfLibrary]`(EffectTarget.Self)` moves The Spot from the graveyard to the
 *    bottom of its owner's library (Chaos, the Endless's precedent), and only "if you do" does
 *    [Effects.ReturnLinkedExileToHand] return the cards this source exiled to their owners' hands.
 *    The link survives the source's own graveyard→library move (`LinkedExileComponent` is stripped
 *    only on battlefield re-entry, CR 400.7), so the return still finds both exiled cards.
 */
val TheSpotLivingPortal = card("The Spot, Living Portal") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Scientist Villain"
    power = 4
    toughness = 4
    oracleText = "When The Spot enters, exile up to one target nonland permanent and up to one " +
        "target nonland permanent card from a graveyard.\n" +
        "When The Spot dies, put him on the bottom of his owner's library. If you do, return the " +
        "exiled cards to their owners' hands."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "up to one target nonland permanent",
            TargetObject(optional = true, filter = TargetFilter.NonlandPermanent),
        )
        val graveyardCard = target(
            "up to one target nonland permanent card from a graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.NonlandPermanent, zone = Zone.GRAVEYARD),
            ),
        )
        effect = Effects.Composite(
            Effects.ExileUntilLeaves(permanent),
            Effects.ExileUntilLeaves(graveyardCard),
        )
        description = "When The Spot enters, exile up to one target nonland permanent and up to " +
            "one target nonland permanent card from a graveyard."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.IfYouDo(
            action = Effects.PutOnBottomOfLibrary(EffectTarget.Self),
            ifYouDo = Effects.ReturnLinkedExileToHand(),
        )
        description = "When The Spot dies, put him on the bottom of his owner's library. If you " +
            "do, return the exiled cards to their owners' hands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Bastien Grivet"
        flavorText = "Dr. Johnathon Ohnn had no problem breaking laws, physical or otherwise."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09081740-1180-48ea-b50b-e016d9c3828a.jpg?1783905309"
    }
}
