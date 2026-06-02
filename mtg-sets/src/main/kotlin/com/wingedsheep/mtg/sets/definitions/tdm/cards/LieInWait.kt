package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Lie in Wait — Tarkir: Dragonstorm #203
 * {B}{G}{U} · Sorcery · Uncommon
 *
 * Return target creature card from your graveyard to your hand. Lie in Wait deals damage equal
 * to that card's power to target creature.
 *
 * Two targets are chosen at cast (CR 601.2c): target #0 the creature card in your graveyard,
 * target #1 the creature to damage. On resolution the graveyard card is returned to hand first,
 * then we deal damage equal to that card's power — read via [DynamicAmount.EntityProperty] off
 * [EntityReference.Target] 0. The card has moved to hand by then, but power off the battlefield
 * resolves to its printed power, which is exactly "that card's power".
 */
val LieInWait = card("Lie in Wait") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to your hand. Lie in Wait deals " +
        "damage equal to that card's power to target creature."

    spell {
        target("target creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
        target("target creature", Targets.Creature)
        effect = Effects.Composite(
            listOf(
                Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                Effects.DealDamage(
                    DynamicAmount.EntityProperty(
                        entity = EntityReference.Target(0),
                        numericProperty = EntityNumericProperty.Power
                    ),
                    EffectTarget.ContextTarget(1),
                    damageSource = EffectTarget.Self
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Diana Franco"
        flavorText = "Some paths through Gurmag Swamp are deserted for good reason."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96fff22c-282b-4849-82ce-890013b53262.jpg?1743204797"
    }
}
