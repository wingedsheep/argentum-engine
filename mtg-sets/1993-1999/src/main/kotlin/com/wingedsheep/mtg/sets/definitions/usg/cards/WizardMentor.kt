package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wizard Mentor
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * {T}: Return this creature and target creature you control to their owner's hand.
 *
 * The target may be Wizard Mentor itself (it's a creature you control). If the target is illegal
 * on resolution the whole ability doesn't resolve, so Wizard Mentor stays put.
 */
val WizardMentor = card("Wizard Mentor") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "{T}: Return this creature and target creature you control to their owner's hand."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ReturnToHand(creature) then Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Jeff Miracola"
        flavorText = "Although some of the students quickly grasped the concept, the others could summon only blackboards."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49805401-9bd9-48a3-9b99-0120a8bb1fb5.jpg?1783946351"
    }
}
