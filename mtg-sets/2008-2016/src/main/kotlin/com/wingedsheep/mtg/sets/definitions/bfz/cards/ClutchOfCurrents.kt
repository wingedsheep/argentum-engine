package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clutch of Currents
 * {U}
 * Sorcery
 * Return target creature to its owner's hand.
 * Awaken 3—{4}{U} (If you cast this spell for {4}{U}, also put three +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the creature target and runs the awaken ability after the bounce.
 */
val ClutchOfCurrents = card("Clutch of Currents") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target creature to its owner's hand.\n" +
        "Awaken 3—{4}{U} (If you cast this spell for {4}{U}, also put three +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(3, "{4}{U}"))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d20f482-e0b2-4e4a-be32-347ab112912c.jpg?1783938210"
    }
}
