package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Daydream
 * {W}
 * Sorcery
 *
 * Exile target creature you control, then return that card to the battlefield under its
 * owner's control with a +1/+1 counter on it.
 * Flashback {2}{W}
 *
 * The blink is the standard `Move(EXILE) then Move(BATTLEFIELD)` flicker (see Splash Portal);
 * the +1/+1 counter is a chained `AddCounters` once the card is back on the battlefield (the
 * target handle still resolves to the freshly-returned permanent). Flashback is a keyword
 * ability so the card can be re-cast from the graveyard for {2}{W}.
 */
val Daydream = card("Daydream") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target creature you control, then return that card to the battlefield " +
        "under its owner's control with a +1/+1 counter on it.\n" +
        "Flashback {2}{W} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.Move(creature, Zone.EXILE)
            .then(Effects.Move(creature, Zone.BATTLEFIELD))
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature))
    }

    keywordAbility(KeywordAbility.flashback("{2}{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Nia Kovalevski"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b16cb2-b8b2-45df-9695-3c16e9d89e28.jpg?1775936973"
    }
}
