package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ruinous Path
 * {1}{B}{B}
 * Sorcery
 * Destroy target creature or planeswalker.
 * Awaken 4—{5}{B}{B} (If you cast this spell for {5}{B}{B}, also put four +1/+1 counters on
 * target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the creature/planeswalker target and runs the awaken ability after the destroy.
 */
val RuinousPath = card("Ruinous Path") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or planeswalker.\n" +
        "Awaken 4—{5}{B}{B} (If you cast this spell for {5}{B}{B}, also put four +1/+1 counters on " +
        "target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{5}{B}{B}"))

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/709ab9cf-eed8-4d73-b10d-c7f6d8750328.jpg?1783938199"
    }
}
