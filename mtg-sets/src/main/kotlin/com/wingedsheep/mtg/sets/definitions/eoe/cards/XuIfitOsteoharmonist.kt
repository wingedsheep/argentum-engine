package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
/**
 * Xu-Ifit, Osteoharmonist
 * {1}{B}{B}
 * Legendary Creature — Human Wizard
 * 2/3
 * {T}: Return target creature card from your graveyard to the battlefield. It's a Skeleton
 *      in addition to its other types and has no abilities. Activate only as a sorcery.
 *
 * The rider is two `Duration.Permanent` floating effects keyed to the reanimated entity
 * (layer-4 add-subtype + layer-6 strip-abilities), appended after `PutOntoBattlefield`.
 * They expire when the entity leaves the battlefield, so a later return is a new object
 * without the rider (CR 400.7). The engine separately suppresses the entity's own
 * dies / leaves-battlefield triggers while the strip is in effect (CR 603.10a).
 */
val XuIfitOsteoharmonist = card("Xu-Ifit, Osteoharmonist") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "{T}: Return target creature card from your graveyard to the battlefield. " +
        "It's a Skeleton in addition to its other types and has no abilities. " +
        "Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        val creature = target("target creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Composite(
            listOf(
                Effects.PutOntoBattlefield(creature),
                Effects.AddCreatureType(Subtype.SKELETON.value, creature, Duration.Permanent),
                Effects.RemoveAllAbilities(creature, Duration.Permanent),
            )
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Michal Ivan"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0838f25-2193-4305-b73a-bf0c0bb4981a.jpg?1752947068"
    }
}
