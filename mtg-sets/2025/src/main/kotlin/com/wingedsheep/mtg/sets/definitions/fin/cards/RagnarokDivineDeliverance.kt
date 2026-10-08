package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ragnarok, Divine Deliverance
 * Legendary Creature — Beast Avatar
 * 7/6
 * Vigilance, menace, trample, reach, haste
 * When Ragnarok dies, destroy target permanent and return target nonlegendary permanent card
 * from your graveyard to the battlefield.
 *
 * This is a meld result (Fang, Fearless l'Cie + Vanille, Cheerful l'Cie); Vanille's first-main-phase
 * trigger melds the pair into it (CR 701.42). It has no mana cost, matching the printed card.
 * `meldOf` declares the pair the meld effect checks and keeps Ragnarok out of booster, draft and
 * deckbuilding pools — it's only ever created by melding.
 *
 * The dies trigger has two independent targets chosen when it's put on the stack (CR 603.3d):
 * a permanent to destroy and a nonlegendary permanent card in your graveyard to reanimate. Each
 * is a separate [target] declaration, so if one becomes illegal before resolution the other
 * still resolves (CR 608.2c). [Effects.PutOntoBattlefield] returns the reanimated card as the
 * same object under the resolving controller.
 */
val RagnarokDivineDeliverance = card("Ragnarok, Divine Deliverance") {
    manaCost = ""
    meldOf("Fang, Fearless l'Cie", "Vanille, Cheerful l'Cie")
    colorIdentity = "BG"
    colorIndicator = "BG"
    typeLine = "Legendary Creature — Beast Avatar"
    power = 7
    toughness = 6
    oracleText = "Vigilance, menace, trample, reach, haste\n" +
        "When Ragnarok dies, destroy target permanent and return target nonlegendary permanent " +
        "card from your graveyard to the battlefield."

    keywords(Keyword.VIGILANCE, Keyword.MENACE, Keyword.TRAMPLE, Keyword.REACH, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val permanent = target(TargetFilter.Permanent)
        val reanimate = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou().nonlegendary(), zone = Zone.GRAVEYARD))
        effect = Effects.Destroy(permanent) then Effects.PutOntoBattlefield(reanimate)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99b"
        artist = "Simon Dominic"
        flavorText = "When prayers turn to promises, not even fate can stand in their way."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01c5bafe-c995-4cef-90fb-7ccb95858511.jpg?1782686523"
    }
}
