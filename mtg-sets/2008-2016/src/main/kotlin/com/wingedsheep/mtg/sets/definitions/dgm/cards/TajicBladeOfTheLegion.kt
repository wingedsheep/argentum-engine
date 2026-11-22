package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tajic, Blade of the Legion
 * {2}{R}{W}
 * Legendary Creature — Human Soldier
 * 2/2
 * Indestructible
 * Battalion — Whenever Tajic and at least two other creatures attack, Tajic gets +5/+5 until end of turn.
 *
 * Battalion is the self attack trigger with `AttackerCountAtLeast(3)` (Boros Mastiff).
 */
val TajicBladeOfTheLegion = card("Tajic, Blade of the Legion") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Indestructible\n" +
        "Battalion — Whenever Tajic and at least two other creatures attack, Tajic gets +5/+5 until end of turn."

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.ModifyStats(5, 5, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "James Ryman"
        flavorText = "\"I run the maze alone, but I fight with the full might of the Legion.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be5717c1-338e-446c-aa7e-93e79e4abb72.jpg?1783940021"
        ruling(
            "2024-01-12",
            "The three attacking creatures don't have to be attacking the same player, planeswalker, or battle.",
        )
        ruling(
            "2013-04-15",
            "Once a battalion ability has triggered, it doesn't matter how many creatures are still attacking " +
                "when that ability resolves.",
        )
    }
}
