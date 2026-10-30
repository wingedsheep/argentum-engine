package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nessian Boar
 * {3}{G}{G}
 * Creature — Boar
 * 10/6
 * All creatures able to block this creature do so.
 * Whenever this creature becomes blocked by a creature, that creature's controller draws a card.
 *
 * The Lure clause is the static `MustBeBlocked(allCreatures = true)` (Nath's Elite), which only
 * binds creatures that can block it — a tapped creature or one whose block carries a cost stays
 * free (first ruling). The draw is Grasping Giant's filtered `becomesBlocked(by = Creature)`, which
 * fires once per blocker with that blocker as the triggering entity, so each blocker's controller
 * draws through [EffectTarget.ControllerOfTriggeringEntity].
 */
val NessianBoar = card("Nessian Boar") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Boar"
    power = 10
    toughness = 6
    oracleText = "All creatures able to block this creature do so.\n" +
        "Whenever this creature becomes blocked by a creature, that creature's controller draws a card."

    staticAbility {
        ability = MustBeBlocked(allCreatures = true)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.DrawCards(1, EffectTarget.ControllerOfTriggeringEntity)
        description = "Whenever this creature becomes blocked by a creature, that creature's controller " +
            "draws a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Zoltan Boros"
        flavorText = "Renata led the best hunters in all of Theros on a quest to bring down the terror of " +
            "the Setessan wilds."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaf91fc2-7317-47e4-9e6c-d7d74cdf0153.jpg?1783931536"
        ruling(
            "2020-01-24",
            "If a creature can't block Nessian Boar for any reason (such as being tapped), then it doesn't " +
                "block. If there's a cost associated with having it block, the player isn't forced to pay that " +
                "cost, so it doesn't have to block in that case either. If there's a cost associated with having " +
                "it block Nessian Boar, but not to block other attacking creatures, that creature is free to " +
                "block one of those other creatures or to not block at all."
        )
        ruling("2020-01-24", "If a creature is required to block two or more different creatures, its controller chooses which one that creature blocks.")
        ruling("2020-01-24", "Nessian Boar's second ability triggers for each creature blocking it.")
        ruling(
            "2020-01-24",
            "Nessian Boar's second ability resolves before combat damage is dealt, and players have priority " +
                "to cast spells (such as the cards the defending player just drew) after the ability has " +
                "resolved but before combat damage is dealt."
        )
    }
}
