package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Elite Headhunter
 * {B/R}{B/R}{B/R}{B/R}
 * Creature — Human Knight
 * 2/3
 * Menace
 * {B/R}{B/R}{B/R}, Sacrifice another creature or an artifact: This creature deals 2 damage to
 * target creature or planeswalker.
 */
val EliteHeadhunter = card("Elite Headhunter") {
    manaCost = "{B/R}{B/R}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "{B/R}{B/R}{B/R}, Sacrifice another creature or an artifact: This creature deals 2 damage to target creature or planeswalker."

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B/R}{B/R}{B/R}"),
            // "another creature or an artifact": only the creature branch excludes this permanent,
            // so it can sacrifice itself if it has somehow become an artifact (2019-10-04 ruling).
            Costs.Sacrifice(
                GameObjectFilter.Creature.copy(statePredicates = listOf(StatePredicate.Not(StatePredicate.IsSource))) or
                    GameObjectFilter.Artifact
            ),
        )
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(2, victim)
        description = "{B/R}{B/R}{B/R}, Sacrifice another creature or an artifact: This creature deals 2 damage to target creature or planeswalker."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Daarken"
        flavorText = "Nothing remains of his foes but the gripping story of their downfall."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa9d0279-4fe0-4228-a82f-bc3cd91500df.jpg?1783932590"
        ruling(
            "2019-10-04",
            "If Elite Headhunter somehow becomes an artifact, you can sacrifice it to pay the cost of its activated ability."
        )
    }
}
