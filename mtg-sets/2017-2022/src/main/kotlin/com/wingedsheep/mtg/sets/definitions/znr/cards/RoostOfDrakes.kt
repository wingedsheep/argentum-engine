package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Roost of Drakes — Zendikar Rising #74
 * {U} · Enchantment
 *
 * Kicker {2}{U}
 * When this enchantment enters, if it was kicked, create a 2/2 blue Drake creature token with flying.
 * Whenever you cast a kicked spell, create a 2/2 blue Drake creature token with flying.
 *
 * Roost itself isn't on the battlefield when it is cast, so a kicked Roost makes exactly one Drake
 * (from its enters trigger), not two.
 */
val RoostOfDrakes = card("Roost of Drakes") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Kicker {2}{U} (You may pay an additional {2}{U} as you cast this spell.)\n" +
        "When this enchantment enters, if it was kicked, create a 2/2 blue Drake creature token with flying.\n" +
        "Whenever you cast a kicked spell, create a 2/2 blue Drake creature token with flying."

    keywordAbility(KeywordAbility.kicker("{2}{U}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Drake"),
            keywords = setOf(Keyword.FLYING),
        )
        description = "When this enchantment enters, if it was kicked, create a 2/2 blue Drake creature " +
            "token with flying."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Drake"),
            keywords = setOf(Keyword.FLYING),
        )
        description = "Whenever you cast a kicked spell, create a 2/2 blue Drake creature token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Bayard Wu"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba1240d4-dbd0-483b-934a-5f7c505fad42.jpg?1783929393"
    }
}
