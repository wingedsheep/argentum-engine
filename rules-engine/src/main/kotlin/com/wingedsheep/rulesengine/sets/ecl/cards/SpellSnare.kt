package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CounterSpellEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.SpellTargetFilter
import com.wingedsheep.rulesengine.targeting.TargetSpell

/**
 * Spell Snare
 *
 * {U} Instant
 * Counter target spell with mana value 2.
 */
object SpellSnare {
    val definition = CardDefinition.instant(
        name = "Spell Snare",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "Counter target spell with mana value 2.",
        metadata = ScryfallMetadata(
            collectorNumber = "71",
            rarity = Rarity.UNCOMMON,
            artist = "Iris Compiet",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb2b2b2b-2b2b-2b2b-2b2b-2b2b2b2b2b2b.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Spell Snare") {
        targets(TargetSpell(filter = SpellTargetFilter.WithManaValue(2)))

        spell(CounterSpellEffect)
    }
}
