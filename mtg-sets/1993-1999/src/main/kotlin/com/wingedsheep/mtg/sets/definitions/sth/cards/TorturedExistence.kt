package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tortured Existence — Stronghold #74 (canonical printing)
 * {B} · Enchantment
 *
 * {B}, Discard a creature card: Return target creature card from your graveyard to your hand.
 *
 * The target is chosen before the discard cost is paid, so the discarded card can never be the
 * returned one (2004-10-04 ruling).
 */
val TorturedExistence = card("Tortured Existence") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "{B}, Discard a creature card: Return target creature card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Discard(GameObjectFilter.Creature))
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Keith Parkinson"
        flavorText = "\"There are terrors lurking in the unseen corners of us all.\"\n—Crovax"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1754b92b-d6f9-4503-af01-dee03f72a048.jpg?1783946558"
        ruling("2004-10-04", "You can't return the same card you are discarding. This is because you pick the target before paying the costs.")
    }
}
