package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Encroaching Dragonstorm — Tarkir: Dragonstorm #142
 * {3}{G} · Enchantment · Uncommon
 *
 * When this enchantment enters, search your library for up to two basic land cards, put them
 * onto the battlefield tapped, then shuffle.
 * When a Dragon you control enters, return this enchantment to its owner's hand.
 *
 * ETB is a standard "up to two" basic-land ramp ([LibraryPatterns.searchLibrary] with
 * [GameObjectFilter.BasicLand], `count = 2` → `SelectionMode.ChooseUpTo(2)`,
 * [SearchDestination.BATTLEFIELD] `entersTapped = true`, shuffle after). Shares the
 * Dragonstorm-cycle Dragon-bounce trigger.
 */
val EncroachingDragonstorm = card("Encroaching Dragonstorm") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, search your library for up to two basic land " +
        "cards, put them onto the battlefield tapped, then shuffle.\n" +
        "When a Dragon you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
        description = "When this enchantment enters, search your library for up to two basic " +
            "land cards, put them onto the battlefield tapped, then shuffle."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "When a Dragon you control enters, return this enchantment to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Marco Gorlei"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4ddd4477-f8c9-4d05-9177-f8344ba8f40b.jpg?1743204535"
    }
}
