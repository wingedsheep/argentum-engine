package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Goblin Cratermaker
 * {1}{R}
 * Creature — Goblin Warrior
 * 2/2
 *
 * {1}, Sacrifice this creature: Choose one —
 * • This creature deals 2 damage to target creature.
 * • Destroy target colorless nonland permanent.
 *
 * A modal *ability*, so `countsAsModalSpell = false`. The damage source is the sacrificed
 * creature, read through last-known information.
 */
val GoblinCratermaker = card("Goblin Cratermaker") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "{1}, Sacrifice this creature: Choose one —\n" +
        "• This creature deals 2 damage to target creature.\n" +
        "• Destroy target colorless nonland permanent."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = ModalEffect.chooseOne(
            mode("Goblin Cratermaker deals 2 damage to target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(2, creature)
            },
            mode("Destroy target colorless nonland permanent") {
                val permanent = target(
                    TargetFilter(GameObjectFilter.NonlandPermanent.withCardPredicate(CardPredicate.IsColorless))
                )
                effect = Effects.Destroy(permanent)
            },
            countsAsModalSpell = false
        )
        description = "{1}, Sacrifice Goblin Cratermaker: Choose one — it deals 2 damage to target " +
            "creature; or destroy target colorless nonland permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86ecaedc-08f1-4de7-aae8-056df57940e0.jpg?1783934162"
    }
}
