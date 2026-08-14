package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Delver of Secrets // Insectile Aberration (Innistrad #51)
 * {U}
 * Creature — Human Wizard 1/1 // Creature — Human Insect 3/2
 *
 * Front — "At the beginning of your upkeep, look at the top card of your library. You may reveal
 *          that card. If an instant or sorcery card is revealed this way, transform this creature."
 * Back  — Flying.
 *
 * Nothing moves zones: the card is looked at, optionally revealed, and stays on top of the library
 * either way (printed ruling). The pipeline is therefore gather-only —
 * `GatherCards(TopOfLibrary(1))` puts the card in a collection without touching the library, and no
 * `MoveCollection` follows.
 *
 * The "look at … you may reveal" pair is a single [SelectFromCollectionEffect] with
 * `ChooseUpTo(1)` and `showAllCards = true`: the overlay *shows* the controller the top card (the
 * look, private to them) and selecting it *is* the choice to reveal, while declining selects nothing.
 * [RevealCollectionEffect] then publishes only what was actually selected, so an unrevealed card is
 * never leaked to opponents.
 *
 * The transform is gated on the **revealed** collection, not the looked-at one — per the ruling you
 * may reveal a card that isn't an instant or sorcery (no transform), and declining to reveal an
 * instant or sorcery likewise doesn't transform.
 *
 * The back face has no mana cost, so its blue comes from a color indicator (CR 204).
 */

private val DelverOfSecretsFront = card("Delver of Secrets") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your upkeep, look at the top card of your library. You may " +
        "reveal that card. If an instant or sorcery card is revealed this way, transform this creature."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(
            // Look at the top card of your library — gather only; it stays on top either way.
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "delverLooked",
            ),
            // You may reveal that card. Selecting it is the reveal; declining selects nothing.
            SelectFromCollectionEffect(
                from = "delverLooked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "delverRevealed",
                showAllCards = true,
                prompt = "You may reveal the top card of your library",
                selectedLabel = "Reveal",
            ),
            RevealCollectionEffect(from = "delverRevealed", revealToSelf = false),
            // If an instant or sorcery card is revealed this way, transform this creature.
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch(
                    "delverRevealed",
                    GameObjectFilter.InstantOrSorcery,
                ),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "At the beginning of your upkeep, look at the top card of your library. You " +
            "may reveal that card. If an instant or sorcery card is revealed this way, transform " +
            "this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11bf83bb-c95b-4b4f-9a56-ce7a1816307a.jpg?1783940984"
        ruling(
            "2011-09-22",
            "You may reveal the card even if it's not an instant or sorcery. Whether or not you " +
                "reveal it, the card stays on top of your library."
        )
    }
}

private val InsectileAberration = card("Insectile Aberration") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Insect"
    power = 3
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Nils Hamm"
        flavorText = "\"Unfortunately, all my test animals have died or escaped, so I shall be the " +
            "final subject. I feel no fear. This is a momentous night.\"\n—Laboratory notes, final entry"
        imageUri = "https://cards.scryfall.io/normal/back/1/1/11bf83bb-c95b-4b4f-9a56-ce7a1816307a.jpg?1783940984"
    }
}

val DelverOfSecrets: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DelverOfSecretsFront,
    backFace = InsectileAberration,
)
