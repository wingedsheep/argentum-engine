package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blood Fountain
 * {B}
 * Artifact
 *
 * When this artifact enters, create a Blood token.
 * {3}{B}, {T}, Sacrifice this artifact: Return up to two target creature cards from your graveyard
 * to your hand.
 */
val BloodFountain = card("Blood Fountain") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, create a Blood token. (It's an artifact with \"{1}, {T}, " +
        "Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "{3}{B}, {T}, Sacrifice this artifact: Return up to two target creature cards from your " +
        "graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateBlood(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.Tap, Costs.SacrificeSelf)
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd03651e-ada0-41dc-8722-0eba476943e3.jpg?1782703122"
    }
}
