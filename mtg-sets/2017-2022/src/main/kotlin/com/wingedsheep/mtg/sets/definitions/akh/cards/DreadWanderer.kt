package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dread Wanderer
 * {B}
 * Creature — Zombie Jackal
 * 2/1
 * This creature enters tapped.
 * {2}{B}: Return this card from your graveyard to the battlefield. Activate only as a sorcery and
 * only if you have one or fewer cards in hand.
 *
 * The recursion is a graveyard-zone activated ability (the Balustrade Wurm shape); its
 * self-replacement "enters tapped" also applies when it returns this way.
 */
val DreadWanderer = card("Dread Wanderer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Jackal"
    power = 2
    toughness = 1
    oracleText = "This creature enters tapped.\n" +
        "{2}{B}: Return this card from your graveyard to the battlefield. Activate only as a sorcery " +
        "and only if you have one or fewer cards in hand."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CardsInHandAtMost(1)))
        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "{2}{B}: Return this card from your graveyard to the battlefield. Activate only " +
            "as a sorcery and only if you have one or fewer cards in hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/343fc82d-7d10-4489-8f7e-9995322114e2.jpg?1783936506"
    }
}
