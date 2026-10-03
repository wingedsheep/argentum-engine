package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thopter Spy Network
 * {2}{U}{U}
 * Enchantment
 * At the beginning of your upkeep, if you control an artifact, create a 1/1 colorless Thopter
 * artifact creature token with flying.
 * Whenever one or more artifact creatures you control deal combat damage to a player, draw a card.
 *
 * The upkeep trigger carries an intervening-if ([Conditions.YouControl] an artifact), checked on
 * trigger and again on resolution. The draw is a CR 603.2c batch trigger (Kutzil, Malamet Exemplar's
 * shape): one card no matter how many artifact creatures connect at once.
 */
val ThopterSpyNetwork = card("Thopter Spy Network") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, if you control an artifact, create a 1/1 colorless " +
        "Thopter artifact creature token with flying.\n" +
        "Whenever one or more artifact creatures you control deal combat damage to a player, draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControl(GameObjectFilter.Artifact)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
        )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.ArtifactCreature).dealCombatDamageToAPlayer()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5516d86e-539a-4ba8-8d25-1a3b27b1267a.jpg?1783938346"
    }
}
