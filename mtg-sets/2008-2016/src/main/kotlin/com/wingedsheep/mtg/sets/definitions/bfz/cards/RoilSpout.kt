package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Roil Spout
 * {1}{W}{U}
 * Sorcery
 * Put target creature on top of its owner's library.
 * Awaken 4—{4}{W}{U} (If you cast this spell for {4}{W}{U}, also put four +1/+1 counters on target
 * land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the creature target and runs the awaken ability after the creature is moved.
 */
val RoilSpout = card("Roil Spout") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Sorcery"
    oracleText = "Put target creature on top of its owner's library.\n" +
        "Awaken 4—{4}{W}{U} (If you cast this spell for {4}{W}{U}, also put four +1/+1 counters on " +
        "target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{4}{W}{U}"))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Move(t, Zone.LIBRARY, ZonePlacement.Top)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee9fdecc-ba16-4aa2-9c20-7c2942ec143e.jpg?1783938178"
    }
}
