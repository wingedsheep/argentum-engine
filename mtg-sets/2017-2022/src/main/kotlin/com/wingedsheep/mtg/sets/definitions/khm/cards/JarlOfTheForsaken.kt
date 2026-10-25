package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jarl of the Forsaken
 * {3}{B}
 * Creature — Zombie Cleric
 * 3/2
 * Flash
 * When this creature enters, destroy target creature or planeswalker an opponent controls that
 * was dealt damage this turn.
 * Foretell {1}{B}
 *
 * The bare `Keyword.FORETELL` is display-only; [KeywordAbility.foretell] is what the engine's
 * ForetellEnumerator reads to offer the pay-{2}-and-exile action and the later {1}{B} cast.
 */
val JarlOfTheForsaken = card("Jarl of the Forsaken") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Cleric"
    oracleText = "Flash\nWhen this creature enters, destroy target creature or planeswalker an opponent " +
        "controls that was dealt damage this turn.\nForetell {1}{B} (During your turn, you may pay {2} " +
        "and exile this card from your hand face down. Cast it on a later turn for its foretell cost.)"
    power = 3
    toughness = 2

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker).opponentControls().wasDealtDamageThisTurn()
        )
        effect = Effects.Destroy(t)
    }

    keywordAbility(KeywordAbility.foretell("{1}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5b12c6c-c3d6-4b8a-bed4-d61f67f565b9.jpg?1783928245"
    }
}
