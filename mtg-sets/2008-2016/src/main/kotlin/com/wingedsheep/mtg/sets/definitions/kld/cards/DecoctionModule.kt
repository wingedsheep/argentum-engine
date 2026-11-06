package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decoction Module — Kaladesh #205
 * {2} · Artifact
 *
 * Whenever a creature you control enters, you get {E} (an energy counter).
 * {4}, {T}: Return target creature you control to its owner's hand.
 */
val DecoctionModule = card("Decoction Module") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Whenever a creature you control enters, you get {E} (an energy counter).\n" +
        "{4}, {T}: Return target creature you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GetEnergy(1)
        description = "Whenever a creature you control enters, you get {E}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ReturnToHand(creature)
        description = "{4}, {T}: Return target creature you control to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Aaron Miller"
        flavorText = "Progress leads to inspiration."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b08ae769-0070-47a6-aae0-1a3bcfd40d07.jpg?1783937159"
    }
}
