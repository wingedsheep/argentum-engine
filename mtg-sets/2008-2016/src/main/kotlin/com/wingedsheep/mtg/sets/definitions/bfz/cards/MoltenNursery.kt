package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Molten Nursery — Battle for Zendikar #130
 * {2}{R} · Enchantment
 * Devoid (This card has no color.)
 * Whenever you cast a colorless spell, this enchantment deals 1 damage to any target.
 *
 * The cast trigger is Kozilek's Sentinel's `CardPredicate.IsColorless` filter, so a devoid spell
 * or an artifact counts.
 */
val MoltenNursery = card("Molten Nursery") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Devoid (This card has no color.)\n" +
        "Whenever you cast a colorless spell, this enchantment deals 1 damage to any target."

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless))
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
        description = "Whenever you cast a colorless spell, this enchantment deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Raymond Swanland"
        flavorText = "\"Some detours just aren't worth it.\"\n—Raff Slugeater, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3db8aaf5-e2bf-40ea-bd5d-663017cfd4a6.jpg?1783938198"
    }
}
