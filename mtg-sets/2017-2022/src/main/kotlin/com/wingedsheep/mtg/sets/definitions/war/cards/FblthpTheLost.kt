package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fblthp, the Lost
 * {1}{U}
 * Legendary Creature — Homunculus
 * 1/1
 *
 * When Fblthp enters, draw a card. If it entered from your library or was cast from your library,
 * draw two cards instead.
 * When Fblthp becomes the target of a spell, shuffle Fblthp into its owner's library.
 *
 * "Your library": a card is only ever in its owner's library, so it came from *your* library
 * exactly when it came from a library and you (the ability's controller) own it.
 */
val FblthpTheLost = card("Fblthp, the Lost") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Homunculus"
    power = 1
    toughness = 1
    oracleText = "When Fblthp enters, draw a card. If it entered from your library or was cast from " +
        "your library, draw two cards instead.\n" +
        "When Fblthp becomes the target of a spell, shuffle Fblthp into its owner's library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.TriggeringEntityEnteredOrWasCastFrom(Zone.LIBRARY),
                Conditions.SourceMatches(GameObjectFilter.Any.ownedByYou()),
            ),
            then = Effects.DrawCards(2),
            otherwise = Effects.DrawCards(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(spellsOnly = true)
        effect = Effects.ShuffleIntoLibrary(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52558748-6893-4c72-a9e2-e87d31796b59.jpg?1783933465"
        ruling("2019-05-03", "There's normally no way to cast Fblthp from your library or to have it enter the battlefield from your library. You'll have to use other effects to find Fblthp for the bonus card draw.")
        ruling("2019-05-03", "If an effect exiles Fblthp from your library and then lets you cast that card, it's cast from exile, not from your library.")
        ruling("2019-05-03", "Fblthp's last ability triggers only if it's on the battlefield when it becomes the target of a spell.")
        ruling("2019-05-03", "Fblthp's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-05-03", "If the spell that targets Fblthp has no other targets, it won't resolve (because it no longer has a legal target after Fblthp has gotten totally lost in your library).")
    }
}
