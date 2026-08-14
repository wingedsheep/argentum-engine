package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tony Stark // The Invincible Iron Man — Marvel Super Heroes #80 (mythic)
 *
 * Front — Tony Stark · {1}{U} · Legendary Creature — Human Artificer Hero · 1/3
 *   {1}, {T}: Look at the top four cards of your library. You may reveal an artifact card from
 *     among them and put it into your hand. Put the rest on the bottom of your library in a
 *     random order.
 *   {4}{U}{R}: Transform Tony Stark. Activate only as a sorcery.
 *
 * Back — The Invincible Iron Man · Legendary Artifact Creature — Human Hero · 5/5
 *   Flying, haste
 *   At the beginning of combat on your turn, you may put an artifact card from your hand onto the
 *     battlefield. If it's an Equipment, attach it to The Invincible Iron Man.
 *
 * A **modal** double-faced creature ([CardDefinition.modalDoubleFacedPermanent]), the shape the
 * whole MSH hero cycle shares. CR 712.3 lets a modal DFC also transform, and this card uses both
 * routes to the same back face: cast it from hand for its own `{4}{U}{R}` (CR 712.11b/712.11c), or
 * transform into it with the front's sorcery-speed [TransformEffect] ability
 * ([TimingRule.SorcerySpeed]). So the back carries its printed mana cost and *no* color indicator —
 * its U/R comes from that cost — and per CR 712.8f (which, unlike CR 712.8e for nonmodal DFCs, has
 * no mana-value exception) the transformed permanent has the back face's mana value, not the
 * front's.
 *
 *  - The front's dig is the stock [Patterns.Library.lookAtTopRevealMatchingToHand] recipe (look at
 *    four, optionally reveal one artifact card to hand, rest to the bottom in a random order).
 *  - The back's combat trigger is an inline Gather → Select → Move pipeline (the Gilgamesh,
 *    Master-at-Arms idiom): the artifact is a resolution-time *choice*, not a target, and
 *    `chooseUpTo(1)` already expresses "you may". After the move, a choice-free `filter` keeps only
 *    the card that is actually an Equipment, and [Effects.AttachTargetEquipmentToCreature] force-
 *    attaches it to Iron Man — a graceful no-op when the artifact wasn't an Equipment, when the
 *    player declined, or when Iron Man is no longer on the battlefield.
 */

private val TonyStarkFront = card("Tony Stark") {
    manaCost = "{1}{U}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Artificer Hero"
    power = 1
    toughness = 3
    oracleText = "{1}, {T}: Look at the top four cards of your library. You may reveal an artifact " +
        "card from among them and put it into your hand. Put the rest on the bottom of your " +
        "library in a random order.\n" +
        "{4}{U}{R}: Transform Tony Stark. Activate only as a sorcery."

    // {1}, {T}: Look at the top four cards of your library. …
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(4),
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal an artifact card and put it into your hand",
            restOrder = CardOrder.Random,
        )
    }

    // {4}{U}{R}: Transform Tony Stark. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{4}{U}{R}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Tony Stark. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "80"
        artist = "Alexander Lozano"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cea42fd-035e-4b8f-8b1d-ff363b694f14.jpg?1783902955"
    }
}

private val TheInvincibleIronManBack = card("The Invincible Iron Man") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Artifact Creature — Human Hero"
    power = 5
    toughness = 5
    oracleText = "Flying, haste\n" +
        "At the beginning of combat on your turn, you may put an artifact card from your hand " +
        "onto the battlefield. If it's an Equipment, attach it to The Invincible Iron Man."

    keywords(Keyword.FLYING, Keyword.HASTE)

    // At the beginning of combat on your turn, you may put an artifact card from your hand onto
    // the battlefield. If it's an Equipment, attach it to The Invincible Iron Man.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = putArtifactFromHandAndAttach()
        description = "At the beginning of combat on your turn, you may put an artifact card from " +
            "your hand onto the battlefield. If it's an Equipment, attach it to The Invincible " +
            "Iron Man."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "80"
        artist = "Alexander Lozano"
        flavorText = "\"The future never stands still, and neither do I.\""
        imageUri = "https://cards.scryfall.io/normal/back/4/c/4cea42fd-035e-4b8f-8b1d-ff363b694f14.jpg?1783902955"
    }
}

private fun putArtifactFromHandAndAttach(): Effect = Effects.Pipeline {
    val artifactsInHand = gather(
        CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Artifact),
        name = "artifactsInHand",
    )
    val chosen = chooseUpTo(
        1,
        from = artifactsInHand,
        prompt = "You may put an artifact card from your hand onto the battlefield",
        name = "chosenArtifact",
    )
    val entered = moveTracked(
        chosen,
        CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
        name = "putOntoBattlefield",
    )
    // "If it's an Equipment, attach it" — a choice-free partition, not a second decision.
    val equipment = filter(
        entered,
        GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
        name = "enteredEquipment",
    )
    run(
        Effects.AttachTargetEquipmentToCreature(
            equipmentTarget = EffectTarget.PipelineTarget(equipment.key, 0),
            creatureTarget = EffectTarget.Self,
        )
    )
}

val TonyStark: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = TonyStarkFront,
    backFace = TheInvincibleIronManBack,
)
