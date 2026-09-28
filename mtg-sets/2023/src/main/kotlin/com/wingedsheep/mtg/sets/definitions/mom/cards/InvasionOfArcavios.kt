package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Arcavios // Invocation of the Founders — March of the Machine #61.
 * {3}{U}{U} · Battle — Siege · defense 7 // Enchantment
 *
 * When this Siege enters, search your library, graveyard, and/or outside the game for an instant
 * or sorcery card you own, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 * // Whenever you cast an instant or sorcery spell from your hand, you may copy that spell. You
 * may choose new targets for the copy.
 *
 * "Outside the game" is the per-player sideboard pseudo-zone, searched alongside library and
 * graveyard in one pipeline.
 */
private val InvasionOfArcaviosFront = card("Invasion of Arcavios") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Battle — Siege"
    startingDefense = 7
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, search your library, graveyard, and/or outside the game for an " +
        "instant or sorcery card you own, reveal it, and put it into your hand. If you search " +
        "your library this way, shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchMultipleZones(
            zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD, Zone.SIDEBOARD),
            filter = GameObjectFilter.InstantOrSorcery,
            destination = SearchDestination.HAND,
            reveal = true,
        )
        description = "When this Siege enters, search your library, graveyard, and/or outside the " +
            "game for an instant or sorcery card you own, reveal it, and put it into your hand. " +
            "If you search your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c3f661d-b9f7-48e7-afe3-3eaea1022bc1.jpg?1783917042"
    }
}

private val InvocationOfTheFounders = card("Invocation of the Founders") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an instant or sorcery spell from your hand, you may copy that " +
        "spell. You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        effect = Effects.May(Effects.CopyTargetSpell(EffectTarget.TriggeringEntity))
        description = "Whenever you cast an instant or sorcery spell from your hand, you may copy " +
            "that spell. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Dmitry Burmak"
        flavorText = "No professors, no grades, no room for mistakes."
        imageUri = "https://cards.scryfall.io/normal/back/1/c/1c3f661d-b9f7-48e7-afe3-3eaea1022bc1.jpg?1783917042"
    }
}

val InvasionOfArcavios: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfArcaviosFront,
    backFace = InvocationOfTheFounders,
)
