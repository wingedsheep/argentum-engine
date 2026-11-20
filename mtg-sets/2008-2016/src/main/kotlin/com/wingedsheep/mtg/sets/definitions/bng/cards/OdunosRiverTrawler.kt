package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Odunos River Trawler
 * {2}{B}
 * Creature — Zombie
 * 2/2
 *
 * When this creature enters, return target enchantment creature card from your graveyard to your hand.
 * {W}, Sacrifice this creature: Return target enchantment creature card from your graveyard to your hand.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val OdunosRiverTrawler = card("Odunos River Trawler") {
    manaCost = "{2}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, return target enchantment creature card from your graveyard to your hand.\n" +
        "{W}, Sacrifice this creature: Return target enchantment creature card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(enchantmentCreatureCardInYourGraveyard())
        effect = Effects.Move(t, Zone.HAND)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.SacrificeSelf)
        val t = target(enchantmentCreatureCardInYourGraveyard())
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5d1eda4-ac34-4905-9022-59f91d38dbb3.jpg?1783939552"
    }
}

private fun enchantmentCreatureCardInYourGraveyard() =
    TargetFilter((GameObjectFilter.Creature and GameObjectFilter.Enchantment).ownedByYou(), zone = Zone.GRAVEYARD)
