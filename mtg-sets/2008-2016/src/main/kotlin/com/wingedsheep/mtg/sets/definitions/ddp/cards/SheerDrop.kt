package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sheer Drop
 * {2}{W}
 * Sorcery
 * Destroy target tapped creature.
 * Awaken 3—{5}{W} (If you cast this spell for {5}{W}, also put three +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The canonical printing is Duel Decks: Zendikar vs. Eldrazi (2015-08-28), five weeks ahead of
 * Battle for Zendikar, which gets a [com.wingedsheep.sdk.model.Printing] row instead.
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the tapped-creature target and runs the awaken ability after the destroy.
 */
val SheerDrop = card("Sheer Drop") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy target tapped creature.\n" +
        "Awaken 3—{5}{W} (If you cast this spell for {5}{W}, also put three +1/+1 counters on target land " +
        "you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(3, "{5}{W}"))

    spell {
        val t = target(TargetFilter.TappedCreature)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca6e9658-684e-44fd-9c72-c5c3faa9fb1f.jpg?1783938258"
    }
}
