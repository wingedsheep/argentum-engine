package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mechanical Mobster
 * {3}
 * Artifact Creature — Human Robot Villain
 * 2/1
 *
 * When this creature enters, exile up to one target card from a graveyard. Target creature you
 * control connives.
 *
 * Two independent targets on one ETB trigger: the graveyard exile is "up to one target" (optional,
 * so the ability resolves fine with no card chosen or if the chosen card leaves the graveyard),
 * while the connive recipient is a mandatory "target creature you control". Connive itself
 * (CR 701.50) is draw-a-card-then-discard, with a +1/+1 counter on the conniving creature if a
 * nonland was discarded.
 */
val MechanicalMobster = card("Mechanical Mobster") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Human Robot Villain"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, exile up to one target card from a graveyard. " +
        "Target creature you control connives. (Draw a card, then discard a card. If you discarded " +
        "a nonland card, put a +1/+1 counter on that creature.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val exiled = target("target card in a graveyard", TargetObject(optional = true, filter = TargetFilter.CardInGraveyard))
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.Move(exiled, Zone.EXILE),
            Effects.Connive(target = creature)
        )
        description = "When this creature enters, exile up to one target card from a graveyard. Target creature you control connives."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "David Szabo"
        flavorText = "\"A man-made made man? You always knew how to get ahead, Silvermane.\"\n—Spider-Man"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6d9ecc-2dd1-471a-8678-a2461b1084fa.jpg?1783905305"
    }
}
