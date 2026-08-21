package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionFromLinkedExiledCardTypes

/**
 * Mirror Golem — Mirrodin #208
 * {6} · Artifact Creature — Golem · Uncommon · 3/4
 *
 * Imprint — When this creature enters, you may exile target card from a graveyard.
 * This creature has protection from each of the exiled card's card types.
 *
 * Modelling notes:
 * - The two halves are a *linked* pair (CR 607): the static ability can only read what this
 *   creature's own ETB trigger exiled. `Effects.ExileLinkedToSource` writes the pile
 *   (`linkToSource = true`) and [GrantProtectionFromLinkedExiledCardTypes] reads it — never a
 *   "cards in exile" scan, which would scoop up every other exiled card in the game.
 * - The protection is *dynamic*, not printed: it recomputes every projection from the pile, so it
 *   appears the moment the card is exiled and disappears if that card ever leaves exile. Declining
 *   the imprint leaves Mirror Golem with no protection at all — which is why the static can't be
 *   modelled as printed `Keyword.Protection` scopes.
 * - "Each of the exiled card's card types" is plural on purpose: an artifact creature card exiled
 *   this way grants protection from artifacts *and* from creatures.
 * - The trigger targets, so it is put on the stack with a chosen graveyard card and the "you may"
 *   is decided at resolution (`optional = true` lowers to that gate). With no card in any graveyard
 *   the trigger is simply removed for want of a legal target.
 */
val MirrorGolem = card("Mirror Golem") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 4
    oracleText = "Imprint — When this creature enters, you may exile target card from a graveyard.\n" +
        "This creature has protection from each of the exiled card's card types. (Artifact, " +
        "battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery are " +
        "card types.)"

    // "Imprint — When this creature enters, you may exile target card from a graveyard."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val exiled = target("target card from a graveyard", Targets.CardInGraveyard)
        effect = Effects.ExileLinkedToSource(exiled)
        description = "Imprint — When this creature enters, you may exile target card from a graveyard."
    }

    // "This creature has protection from each of the exiled card's card types."
    staticAbility {
        ability = GrantProtectionFromLinkedExiledCardTypes()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9467839-b2a2-4cfe-a60e-12766e2ba983.jpg?1783944513"
    }
}
