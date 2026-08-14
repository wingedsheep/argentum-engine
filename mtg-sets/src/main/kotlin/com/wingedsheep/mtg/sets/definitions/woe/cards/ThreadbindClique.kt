package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Threadbind Clique // Rip the Seams
 * {3}{U}
 * Creature — Faerie
 * 3/3
 *
 * Flying
 *
 * Adventure: Rip the Seams — {2}{W}, Instant — Adventure
 * Destroy target tapped creature.
 *
 * "Tapped" is a state predicate on the target requirement ([TargetFilter.TappedCreature]), not a
 * check inside the effect, so the restriction is enforced twice: once when targets are chosen and
 * again on resolution (CR 608.2b). A creature that untaps in response — Hylda's Crown of Winter
 * pointed the other way, a flash blink — stops being a legal target and the Adventure is countered
 * for having no legal targets, which is the whole reason this is a two-mana removal spell.
 *
 * The two faces are in different colors, so the card's color identity spans both (CR 903.4 — every
 * mana symbol on the card counts): the creature is mono-blue but the Adventure's {W} makes the
 * *card* {U}{W}, which is what `colorIdentity` records.
 */
val ThreadbindClique = card("Threadbind Clique") {
    manaCost = "{3}{U}"
    colorIdentity = "UW"
    typeLine = "Creature — Faerie"
    power = 3
    toughness = 3
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Rip the Seams") {
        manaCost = "{2}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Destroy target tapped creature. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val victim = target(
                "target tapped creature",
                TargetCreature(filter = TargetFilter.TappedCreature),
            )
            effect = Effects.Destroy(victim)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Michal Ivan"
        flavorText = "Giggles of delight may be the last thing you hear."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd6ed252-c262-4062-97ba-75c50d6b5579.jpg?1783915061"
    }
}
