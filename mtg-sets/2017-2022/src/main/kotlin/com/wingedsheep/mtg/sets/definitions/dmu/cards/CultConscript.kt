package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cult Conscript
 * {B}
 * Creature — Skeleton Warrior
 * 2/1
 * This creature enters tapped.
 * {1}{B}: Return this card from your graveyard to the battlefield. Activate only if a
 * non-Skeleton creature died under your control this turn.
 */
val CultConscript = card("Cult Conscript") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Warrior"
    oracleText = "This creature enters tapped.\n{1}{B}: Return this card from your graveyard to the battlefield. " +
        "Activate only if a non-Skeleton creature died under your control this turn."
    power = 2
    toughness = 1

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.NonSubtypeCreatureDiedThisTurn(Subtype.SKELETON, player = Player.You)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Joe Slucher"
        flavorText = "His eternal rest turned out to be more of a quick nap."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fed77513-94ab-44e2-b9a1-30ba927cd6ee.jpg?1783921335"
    }
}
