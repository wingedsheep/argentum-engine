package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stolen Identity
 * {4}{U}{U}
 * Sorcery
 * Create a token that's a copy of target artifact or creature.
 * Cipher
 *
 * Cipher is the keyword alone — the engine runs the encoding step after the token is created, so the
 * fresh token itself is a legal creature to encode onto. Each copy cast off the encoded creature's
 * combat-damage trigger picks a fresh target artifact or creature.
 */
val StolenIdentity = card("Stolen Identity") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create a token that's a copy of target artifact or creature.\n" +
        "Cipher (Then you may exile this spell card encoded on a creature you control. Whenever " +
        "that creature deals combat damage to a player, its controller may cast a copy of the " +
        "encoded card without paying its mana cost.)"

    keywords(Keyword.CIPHER)

    spell {
        val permanent = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.CreateTokenCopyOfTarget(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e058447-7cee-4670-b86b-c95bd6b68144.jpg?1783940133"
        ruling("2013-01-24", "The token copies exactly what was printed on the original artifact or creature and nothing else (unless that permanent is copying something else or is a token). It doesn't copy whether it is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on.")
        ruling("2013-01-24", "If the copied permanent has {X} in its mana cost, X is considered to be zero.")
        ruling("2013-01-24", "If the copied permanent is copying something else (for example, if the copied creature is a Clone), then the token enters as whatever that creature copied.")
        ruling("2013-01-24", "If the copied permanent is a token, the token that's created copies the original characteristics of that token as stated by the effect that put the token onto the battlefield.")
        ruling("2013-01-24", "Any \"enters\" abilities of the copied permanent will trigger when the token enters the battlefield. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the chosen permanent will also work.")
        ruling("2013-01-24", "If the token you create is a copy of a creature, you can exile Stolen Identity encoded on that token.")
        ruling("2013-04-15", "The spell with cipher is encoded on the creature as part of that spell's resolution, just after the spell's other effects. That card goes directly from the stack to exile. It never goes to the graveyard.")
        ruling("2013-04-15", "If the spell with cipher doesn't resolve, none of its effects will happen, including cipher. The card will go to its owner's graveyard and won't be encoded on a creature.")
        ruling("2013-04-15", "If the creature leaves the battlefield, the exiled card will no longer be encoded on any creature. It will stay exiled.")
        ruling("2013-04-15", "If you choose not to cast the copy, or you can't cast it (perhaps because there are no legal targets available), the copy will cease to exist the next time state-based actions are performed. You won't get a chance to cast the copy at a later time.")
    }
}
