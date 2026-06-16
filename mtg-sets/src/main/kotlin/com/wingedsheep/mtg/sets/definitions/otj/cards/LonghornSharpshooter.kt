package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Longhorn Sharpshooter
 * {2}{R}
 * Creature — Minotaur Rogue
 * 3/3
 * Reach
 * When this card becomes plotted, it deals 2 damage to any target.
 * Plot {3}{R}
 */
val LonghornSharpshooter = card("Longhorn Sharpshooter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Rogue"
    power = 3
    toughness = 3
    oracleText = "Reach\n" +
        "When this card becomes plotted, it deals 2 damage to any target.\n" +
        "Plot {3}{R} (You may pay {3}{R} and exile this card from your hand. Cast it as a sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.plot("{3}{R}"))

    triggeredAbility {
        trigger = Triggers.BecomesPlotted
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/398d9a16-d72c-42e2-a0ea-d9da642ee046.jpg"

        ruling("2024-04-12", "Plot abilities are written \"Plot [cost],\" which means \"Any time you have priority during your main phase while the stack is empty, you may pay [cost] and exile this card from your hand. It becomes plotted.\"")
        ruling("2024-04-12", "Longhorn Sharpshooter's triggered ability triggers when it becomes plotted, not when you cast it from exile. You choose the target as the ability is put onto the stack.")
        ruling("2024-04-12", "You can't cast a plotted card on the same turn it became plotted. On any future turn, you may cast that card from exile without paying its mana cost during your main phase while the stack is empty.")
    }
}
