package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cemetery Illuminator
 * {1}{U}{U}
 * Creature — Spirit
 * 2/3
 * Flying
 * Whenever this creature enters or attacks, exile a card from a graveyard.
 * You may look at the top card of your library any time.
 * Once each turn, you may cast a spell from the top of your library if it shares a card type with a
 * card exiled with this creature.
 *
 * The blue half of the Crimson Vow cemetery cycle, and the last of the five. Like [CemeteryProwler]
 * it keeps exiling — one card per enter *and* per attack — so its payoff reads the whole
 * linked-exile pile (CR 607) rather than a single "the exiled card": `sharingCardTypeWithLinkedExile`
 * is the filter form of the reading `CostReductionSource.SharedCardTypesWithLinkedExile` already
 * takes on the Prowler's cost side. An index-keyed `EffectTarget.LinkedExiledCard` would only
 * ever see the first exile and go stale the moment it left exile.
 *
 * "Enters or attacks" is two triggered abilities for [CemeteryGatekeeper]'s reason: the corpus
 * contracts the pair in *print* and writes it as two abilities in the model, which is the spelling
 * Argentum Assay reads and prints.
 *
 * The last line splits cleanly onto two existing statics: [LookAtTopOfLibrary] for "you may look at
 * the top card of your library any time", and [CastSpellTypesFromTopOfLibrary] with
 * `maxCastsPerTurn = 1` for "once each turn, you may cast a spell from the top of your library if …".
 * The allowance belongs to the *permanent*, so an Illuminator that leaves and returns brings a fresh
 * one — which is what the second static's own contract says, and what a second Illuminator relies on.
 *
 * Per its 2021-11-19 ruling the comparison is against the **exiled** cards, not against the top
 * card's other faces: a modal double-faced card whose back face shares a type may be cast even when
 * its front face doesn't, because the shared type comes off the exile pile either way.
 */
val CemeteryIlluminator = card("Cemetery Illuminator") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature enters or attacks, exile a card from a graveyard.\n" +
        "You may look at the top card of your library any time.\n" +
        "Once each turn, you may cast a spell from the top of your library if it shares a card " +
        "type with a card exiled with this creature."

    keywords(Keyword.FLYING)

    // Whenever this creature enters …
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = exileFromAGraveyard()
        description = "When Cemetery Illuminator enters, exile a card from a graveyard."
    }

    // … or attacks, exile a card from a graveyard.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = exileFromAGraveyard()
        description = "Whenever Cemetery Illuminator attacks, exile a card from a graveyard."
    }

    // You may look at the top card of your library any time.
    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    // Once each turn, you may cast a spell from the top of your library if it shares a card type
    // with a card exiled with this creature.
    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Any.sharingCardTypeWithLinkedExile(),
            maxCastsPerTurn = 1,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "50"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f619464-dc3b-4265-b4e4-2578034bf5bf.jpg?1783924900"
        ruling(
            "2021-11-19",
            "Card types that can be exiled from a graveyard include artifact, creature, " +
                "enchantment, land, planeswalker, instant, and sorcery. Legendary, basic, and snow " +
                "are supertypes, not card types. Human, Equipment, and Aura are subtypes, not card types."
        )
        ruling(
            "2021-11-19",
            "Cemetery Illuminator's ability allows you to cast a spell that shares a card type " +
                "with the exiled card, even if the card on top of the library doesn't. For " +
                "example, if you've exiled an artifact card, you could cast The Omenkeel from the " +
                "top of your library, even though it's the back face of the modal double-faced " +
                "card Cosima, God of the Voyage, which isn't an artifact."
        )
    }
}

/** "Exile a card from a graveyard.", linked to the Illuminator — the payload of both trigger halves. */
private fun exileFromAGraveyard(): Effect = Effects.Pipeline {
    val graveyards = gather(
        CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Any),
    )
    val exiled = chooseExactly(
        1,
        from = graveyards,
        useTargetingUI = true,
        prompt = "Exile a card from a graveyard",
        selectedLabel = "Exile",
    )
    exile(exiled, linkToSource = true)
}
