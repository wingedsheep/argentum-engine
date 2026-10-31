package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn

/**
 * Deafening Silence
 * {W}
 * Enchantment
 * Each player can't cast more than one noncreature spell each turn.
 *
 * The global, filtered form of [RestrictSpellsCastPerTurn] (as on Phyrexian Censor): only
 * noncreature spells cast this turn count toward the cap, and only a noncreature spell is blocked
 * once it's reached, so creature spells stay castable. The count reads the whole turn's cast
 * records, so a noncreature spell cast before this entered — or Deafening Silence itself — counts.
 */
val DeafeningSilence = card("Deafening Silence") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Each player can't cast more than one noncreature spell each turn."

    staticAbility {
        ability = RestrictSpellsCastPerTurn(
            maxPerTurn = 1,
            eachPlayer = true,
            spellFilter = GameObjectFilter.Noncreature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Igor Kieryluk"
        flavorText = "\"They have forsaken their vows. The edict of the Circle demands their voices be silenced.\"\n" +
            "—Syr Tasdale, knight of Ardenvale"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/6072d9b0-d3c7-46f4-bd24-095bb13c4dea.jpg?1783932677"
        ruling(
            "2019-10-04",
            "Players may cast any number of creature spells plus one noncreature spell each turn."
        )
        ruling(
            "2019-10-04",
            "Deafening Silence will look at the entire turn to see if a player has cast a noncreature spell " +
                "yet that turn, even if Deafening Silence wasn't on the battlefield when that spell was cast. " +
                "Notably, you can't cast Deafening Silence and then cast another noncreature spell that turn."
        )
        ruling(
            "2019-10-04",
            "If you cast a noncreature spell that was countered, you can't cast another noncreature spell that turn."
        )
    }
}
