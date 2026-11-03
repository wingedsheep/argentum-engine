package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Those Old Scientists — Star Trek #86
 * {2}{U} · Enchantment — Saga
 *
 * I — Draw two cards, then discard two cards.
 * II — Return up to one target artifact, instant, or sorcery card from your graveyard to your hand.
 * III — Choose up to two target cards in graveyards. The owners of those cards shuffle them into
 * their libraries.
 *
 * - Chapter I is Frantic Search's draw-then-discard sequence.
 * - Chapter III targets cards in any graveyard; each one is shuffled into its *owner's* library
 *   (Witness the Future's `ForEachTarget` over `ShuffleIntoLibrary`), so targets from two different
 *   graveyards each land in the right library. Zero targets is legal and does nothing.
 */
val ThoseOldScientists = card("Those Old Scientists") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Draw two cards, then discard two cards.\n" +
        "II — Return up to one target artifact, instant, or sorcery card from your graveyard to your hand.\n" +
        "III — Choose up to two target cards in graveyards. The owners of those cards shuffle them into their libraries."

    sagaChapter(1) {
        effect = Effects.DrawCards(2) then Patterns.Hand.discardCards(2)
    }

    sagaChapter(2) {
        val spellOrArtifact = target(
            TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery, zone = Zone.GRAVEYARD)
                .ownedByYou(),
            optional = true,
        )
        effect = Effects.ReturnToHand(spellOrArtifact)
    }

    sagaChapter(3) {
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.ShuffleIntoLibrary(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Gregg Schigiel"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44280b43-b07e-43a4-9ae3-2eac0baeb918.jpg?1792753712"
    }
}
