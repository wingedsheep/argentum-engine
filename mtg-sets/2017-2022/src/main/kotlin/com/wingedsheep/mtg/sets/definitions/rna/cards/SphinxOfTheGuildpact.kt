package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.SetColors
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sphinx of the Guildpact — Ravnica Allegiance #241
 * {7} · Artifact Creature — Sphinx · 5 / 5
 *
 * "Sphinx of the Guildpact is all colors" is a characteristic-defining self-[SetColors]: the SDK
 * folds it into the card's own colors (`CardDefinition.selfDefinedColors`), so it is all colors in
 * every zone — in hand, on the stack, and on the battlefield.
 */
val SphinxOfTheGuildpact = card("Sphinx of the Guildpact") {
    manaCost = "{7}"
    colorIdentity = "WUBRG"
    typeLine = "Artifact Creature — Sphinx"
    power = 5
    toughness = 5
    oracleText = "Sphinx of the Guildpact is all colors.\n" +
        "Flying\n" +
        "Hexproof from monocolored (This creature can't be the target of monocolored spells or abilities your opponents control.)"

    staticAbility {
        ability = SetColors(colors = Color.entries.toSet(), filter = GroupFilter.source())
    }
    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.Monocolored))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d30399e-2ecb-4de2-b830-673a3f059197.jpg?1783933620"
        ruling("2020-08-07", "The ability that makes Sphinx of the Guildpact all colors applies in all zones, not just the battlefield.")
        ruling("2020-08-07", "An object is monocolored if it has exactly one color. A colorless object isn't monocolored.")
    }
}
