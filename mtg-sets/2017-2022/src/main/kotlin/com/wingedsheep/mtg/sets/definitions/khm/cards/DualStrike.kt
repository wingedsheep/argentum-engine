package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Dual Strike
 * {R}{R}
 * Instant
 * When you next cast an instant or sorcery spell with mana value 4 or less this turn, copy that spell.
 * You may choose new targets for the copy.
 * Foretell {R} (During your turn, you may pay {2} and exile this card from your hand face down.
 * Cast it on a later turn for its foretell cost.)
 */
val DualStrike = card("Dual Strike") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "When you next cast an instant or sorcery spell with mana value 4 or less this turn, " +
        "copy that spell. You may choose new targets for the copy.\nForetell {R} (During your turn, you " +
        "may pay {2} and exile this card from your hand face down. Cast it on a later turn for its " +
        "foretell cost.)"

    spell {
        effect = Effects.CopyNextSpellCast(
            copies = 1,
            spellFilter = GameObjectFilter.InstantOrSorcery.manaValueAtMost(4)
        )
    }

    keywordAbility(KeywordAbility.foretell("{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ef580f8-efec-4d31-8dc3-2018dd48b6f4.jpg?1783928231"
    }
}
