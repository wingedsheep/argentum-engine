package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Barrage Tyrant — Battle for Zendikar #127
 * {4}{R} · Creature — Eldrazi · 5/3
 * Devoid (This card has no color.)
 * {2}{R}, Sacrifice another colorless creature: This creature deals damage equal to the sacrificed
 * creature's power to any target.
 *
 * Brion Stoutarm's shape: [Costs.SacrificeAnother] (excludes the Tyrant itself) narrowed to colorless
 * creatures with the `IsColorless` predicate Skitterskin uses, and [DynamicAmounts.sacrificedPower]
 * reading the sacrificed creature's last-known power off the paid cost. The Tyrant is the damage
 * source, so the sacrificed creature's lifelink or deathtouch doesn't apply (2015-08-25 ruling).
 */
val BarrageTyrant = card("Barrage Tyrant") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{2}{R}, Sacrifice another colorless creature: This creature deals damage equal to the " +
        "sacrificed creature's power to any target."

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{R}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless)),
        )
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sacrificedPower(), victim)
        description = "Barrage Tyrant deals damage equal to the sacrificed colorless creature's power to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Chris Rallis"
        flavorText = "Terrible fury. Excellent aim."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08a83120-c0c5-4277-acae-6ecb6f6bf3e1.jpg?1783938199"
        ruling(
            "2015-08-25",
            "Barrage Tyrant deals the damage, not the sacrificed creature. For example, if the sacrificed " +
                "creature had lifelink, you won't gain life."
        )
    }
}
