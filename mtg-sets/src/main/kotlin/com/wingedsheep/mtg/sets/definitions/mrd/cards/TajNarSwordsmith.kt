package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPayXForEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Taj-Nar Swordsmith — Mirrodin #27
 * {3}{W} · Creature — Cat Soldier · 2/3
 *
 * When this creature enters, you may pay {X}. If you do, search your library for an Equipment
 * card with mana value X or less, put that card onto the battlefield, then shuffle.
 *
 * The {X} is chosen *on resolution of the trigger*, not when the Swordsmith is cast — the number
 * chooser [MayPayXForEffect] raises runs while the trigger resolves, so the opponent has already
 * had the chance to respond to the creature and the payment is made with whatever mana is
 * untapped at that moment. The chosen value is bound into the resolution context, which is how
 * [GameObjectFilter.manaValueAtMostX] reads it while filtering the library.
 *
 * The search is a normal `searchLibrary`, so it is a "you may fail to find" search
 * (`ChooseUpTo(1)`, CR 701.19c) — a library with no cheap enough Equipment is not a loss, and
 * the shuffle happens either way.
 *
 * Fidelity note: the engine's X chooser treats X = 0 as declining the payment, so the strictly
 * legal (and near-useless) "pay {0}, search for a mana value 0 Equipment" line is not offered.
 */
val TajNarSwordsmith = card("Taj-Nar Swordsmith") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you may pay {X}. If you do, search your library for an " +
        "Equipment card with mana value X or less, put that card onto the battlefield, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayPayXForEffect(
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Artifact.withSubtype("Equipment").manaValueAtMostX(),
                count = 1,
                destination = SearchDestination.BATTLEFIELD
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89c24cb4-4d7d-41df-b5c2-bd967a6a5d7e.jpg?1783944557"
    }
}
