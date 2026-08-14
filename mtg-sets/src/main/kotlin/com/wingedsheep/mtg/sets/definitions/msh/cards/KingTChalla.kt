package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * King T'Challa // Black Panther, Hope Enduring — Marvel Super Heroes #219 (mythic)
 *
 * Front — King T'Challa · {1}{W}{U} · Legendary Creature — Human Noble Hero · 3/2
 *   Flash
 *   Whenever a player draws their second card each turn, you draw a card.
 *   {4}{W}{U}: Transform King T'Challa. Activate only as a sorcery.
 *
 * Back — Black Panther, Hope Enduring · Legendary Creature — Human Warrior Hero · 3/3
 *   Flash
 *   Double strike
 *   Prevent all damage that would be dealt to Black Panther.
 *   Whenever Black Panther deals combat damage to a player, draw a card.
 *
 * A **modal** double-faced creature ([CardDefinition.modalDoubleFacedPermanent]), the shape the
 * whole MSH hero cycle shares. CR 712.3 lets a modal DFC also transform, and this card uses both
 * routes to the same back face: cast it from hand for its own `{4}{W}{U}` (CR 712.11b/712.11c), or
 * transform into it with the front's sorcery-speed [TransformEffect] ability
 * ([TimingRule.SorcerySpeed]). So the back carries its printed mana cost and *no* color indicator —
 * its W/U comes from that cost — and per CR 712.8f (which, unlike CR 712.8e for nonmodal DFCs, has
 * no mana-value exception) the transformed permanent has the back face's mana value, not the
 * front's.
 *
 *  - **"Whenever a player draws their second card each turn"** is [Triggers.NthCardDrawn] with
 *    [Player.Each], not [Player.You] — it watches *every* player's per-turn draw count
 *    (`CardsDrawnThisTurnComponent`), and fires once per player per turn when their count crosses
 *    two, including when a single multi-card draw crosses the threshold (CR 121.2). The payoff is
 *    always "you draw", i.e. T'Challa's controller, so an opponent's second draw still profits the
 *    controller. The reflexive draw it grants can itself be the controller's second card, but the
 *    trigger only ever fires on the *second* card, so there is no loop.
 *  - **"Prevent all damage that would be dealt to Black Panther"** is a *static* prevention
 *    replacement effect, not a triggered or one-shot shield: [PreventDamage] with `amount = null`
 *    ("prevent all") over an unrestricted [EventPattern.DamageEvent] keyed to
 *    [RecipientFilter.Self]. Leaving `damageType` at its default `Any` is what separates it from
 *    Fog Bank's combat-only twin — noncombat damage (burn, pingers, sagas) is prevented too.
 *    Being a prevention *shield* rather than damage immunity, it does not stop "damage can't be
 *    prevented" sources, and it never turns off state-based lethality for other creatures.
 *  - The back's draw is a plain [Triggers.DealsCombatDamageToPlayer]. Double strike makes it fire
 *    twice in a combat where both strikes connect (two separate combat-damage steps, two
 *    triggers), which is the card's intended payoff.
 */

private val KingTChallaFront = card("King T'Challa") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Noble Hero"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "Whenever a player draws their second card each turn, you draw a card.\n" +
        "{4}{W}{U}: Transform King T'Challa. Activate only as a sorcery."

    keywords(Keyword.FLASH)

    // Whenever a player draws their second card each turn, you draw a card.
    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2, Player.Each)
        effect = Effects.DrawCards(1)
        description = "Whenever a player draws their second card each turn, you draw a card."
    }

    // {4}{W}{U}: Transform King T'Challa. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{4}{W}{U}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform King T'Challa. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/add7d3ce-aa58-4da0-8c2a-cfd01c3a8975.jpg?1783902911"
    }
}

private val BlackPantherHopeEnduringBack = card("Black Panther, Hope Enduring") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Warrior Hero"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "Double strike\n" +
        "Prevent all damage that would be dealt to Black Panther.\n" +
        "Whenever Black Panther deals combat damage to a player, draw a card."

    keywords(Keyword.FLASH, Keyword.DOUBLE_STRIKE)

    // Prevent all damage that would be dealt to Black Panther.
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.Self),
        )
    )

    // Whenever Black Panther deals combat damage to a player, draw a card.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DrawCards(1)
        description = "Whenever Black Panther deals combat damage to a player, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Eric Wilkerson"
        imageUri = "https://cards.scryfall.io/normal/back/a/d/add7d3ce-aa58-4da0-8c2a-cfd01c3a8975.jpg?1783902911"
    }
}

val KingTChalla: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = KingTChallaFront,
    backFace = BlackPantherHopeEnduringBack,
)
