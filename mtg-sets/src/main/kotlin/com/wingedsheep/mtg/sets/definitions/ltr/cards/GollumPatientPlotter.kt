package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gollum, Patient Plotter
 * {1}{B}
 * Legendary Creature — Halfling Horror
 * 3/1
 *
 * When Gollum leaves the battlefield, the Ring tempts you.
 * {B}, Sacrifice a creature: Return this card from your graveyard to your hand. Activate only as a
 * sorcery.
 *
 * Gap 11 (graveyard-functional activated ability) is engine-landed (`activateFromZone =
 * Zone.GRAVEYARD` + `GraveyardAbilityEnumerator`, template Undead Gladiator). The leave-battlefield
 * Ring tempt and the sorcery-speed return-from-graveyard both compose from existing primitives.
 */
val GollumPatientPlotter = card("Gollum, Patient Plotter") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Halfling Horror"
    power = 3
    toughness = 1
    oracleText = "When Gollum leaves the battlefield, the Ring tempts you.\n" +
        "{B}, Sacrifice a creature: Return this card from your graveyard to your hand. Activate only " +
        "as a sorcery."

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.TheRingTemptsYou()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Sacrifice(GameObjectFilter.Creature))
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"They'll take it, steal my Precious. Thieves. We hates them.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4ddda7d4-0226-404f-8418-f1f5720dcef8.jpg?1686968450"
    }
}
