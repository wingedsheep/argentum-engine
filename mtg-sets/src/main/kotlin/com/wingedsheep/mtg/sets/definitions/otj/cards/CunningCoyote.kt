package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Cunning Coyote
 * {1}{R}
 * Creature — Coyote
 * 2/2
 * Haste
 * When this creature enters, another target creature you control gets +1/+1 and gains haste until end of turn.
 * Plot {1}{R} (You may pay {1}{R} and exile this card from your hand. Cast it as a sorcery on a later turn
 * without paying its mana cost. Plot only as a sorcery.)
 */
val CunningCoyote = card("Cunning Coyote") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Coyote"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "When this creature enters, another target creature you control gets +1/+1 and gains haste until end of turn.\n" +
        "Plot {1}{R} (You may pay {1}{R} and exile this card from your hand. Cast it as a sorcery on a later turn " +
        "without paying its mana cost. Plot only as a sorcery.)"

    keywords(Keyword.HASTE)
    keywordAbility(KeywordAbility.plot("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.Composite(
            Effects.ModifyStats(power = 1, toughness = 1, target = t),
            Effects.GrantKeyword(Keyword.HASTE, target = t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b4ac6ea-c67b-4f90-be4f-aa25882f5794.jpg?1712355730"

        ruling("2024-04-12", "Plot abilities are written \"Plot [cost],\" which means \"Any time you have priority during your main phase while the stack is empty, you may pay [cost] and exile this card from your hand. It becomes plotted.\"")
        ruling("2024-04-12", "Cunning Coyote's triggered ability targets another creature you control, not Cunning Coyote itself. If you control no other creatures, the ability is removed from the stack and has no effect.")
    }
}
