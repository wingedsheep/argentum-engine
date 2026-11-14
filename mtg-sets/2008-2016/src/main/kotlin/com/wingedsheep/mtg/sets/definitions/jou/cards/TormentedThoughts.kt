package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tormented Thoughts
 * {2}{B}
 * Sorcery
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Target player discards a number of cards equal to the sacrificed creature's power.
 *
 * The sacrificed creature's power is read from its cost-payment snapshot (last-known
 * information), and the discarding player chooses which cards to discard.
 */
val TormentedThoughts = card("Tormented Thoughts") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nTarget player discards a number of cards equal to the sacrificed creature's power."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val player = target(Targets.Player)
        effect = Effects.Discard(DynamicAmounts.sacrificedPower(), player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Allen Williams"
        flavorText = "\"Not all nightmares can be escaped by waking.\"\n—Ashiok, Nightmare Weaver"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/130655e0-af82-4d91-acc2-270eaa33e3d1.jpg?1783939428"
        ruling("2014-04-26", "The targeted player will discard a number of cards equal to the power of the sacrificed creature as it last existed on the battlefield.")
        ruling("2014-04-26", "You must sacrifice exactly one creature to cast this spell; you cannot cast it without sacrificing a creature, and you cannot sacrifice additional creatures.")
        ruling("2014-04-26", "Players can only respond once this spell has been cast and all its costs have been paid. No one can try to destroy the creature you sacrificed to prevent you from casting this spell.")
    }
}
