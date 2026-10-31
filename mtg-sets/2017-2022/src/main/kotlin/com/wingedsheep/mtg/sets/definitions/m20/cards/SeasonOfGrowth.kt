package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Season of Growth
 * {1}{G}
 * Enchantment
 *
 * Whenever a creature you control enters, scry 1.
 * Whenever you cast a spell that targets a creature you control, draw a card.
 *
 * The cast trigger gates on [SpellCastPredicate.TargetsMatching] — it fires once per spell as long as
 * at least one chosen target is a creature you control, however many such targets the spell has.
 */
val SeasonOfGrowth = card("Season of Growth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control enters, scry 1. (Look at the top card of your library. " +
        "You may put that card on the bottom.)\n" +
        "Whenever you cast a spell that targets a creature you control, draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.Scry(1)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl()))
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Randy Vargas"
        flavorText = "\"Awake, and lift your faces to the sun.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b9a718-01b9-46b2-85eb-55f6206ee5e4.jpg?1783932959"

        ruling(
            "2019-07-12",
            "Season of Growth's last ability resolves before the spell that caused it to trigger. " +
                "It resolves even if that spell is countered."
        )
        ruling(
            "2019-07-12",
            "If multiple creatures enter the battlefield under your control simultaneously, you'll " +
                "scry 1 for each of those creatures. You won't look at more than one card from your " +
                "library at once."
        )
        ruling(
            "2019-07-12",
            "Season of Growth's last ability triggers when you cast a spell that has multiple targets, " +
                "as long as at least one of those targets is a creature you control. It doesn't trigger " +
                "multiple times if you cast a spell that targets a creature you control multiple times " +
                "or that targets multiple creatures you control."
        )
    }
}
