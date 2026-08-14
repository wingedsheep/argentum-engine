package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Bofur, Reliable Guardian // Concerted Care — The Hobbit #6
 * {W}
 * Legendary Creature — Dwarf Scout
 * 1/1
 *
 * Lifelink
 *
 * Adventure: Concerted Care — {1}{W}, Instant — Adventure
 * Target artifact or creature you control gains hexproof and indestructible until end of turn.
 *
 * Both grants ride the same target, so a single [TargetPermanent] slot feeds two
 * [Effects.GrantKeyword] applications — if the permanent leaves before resolution the spell fizzles
 * as a whole rather than half-applying.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val BofurReliableGuardian = card("Bofur, Reliable Guardian") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Scout"
    power = 1
    toughness = 1
    oracleText = "Lifelink"

    keywords(Keyword.LIFELINK)

    adventure("Concerted Care") {
        manaCost = "{1}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Target artifact or creature you control gains hexproof and indestructible " +
            "until end of turn. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            val permanent = target(
                "target artifact or creature you control",
                TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()))
            )
            effect = Effects.GrantKeyword(Keyword.HEXPROOF, permanent, Duration.EndOfTurn)
                .then(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, permanent, Duration.EndOfTurn))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Kieran Yanner"
        flavorText = "Bofur and Bombur were left behind to guard the ponies and such stores as they had."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b8e6435-7de4-41d5-bc7d-8e24c11897d0.jpg?1785496981"
    }
}
