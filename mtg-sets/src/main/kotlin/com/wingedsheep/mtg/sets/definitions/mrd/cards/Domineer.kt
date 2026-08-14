package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Domineer — Mirrodin #33
 * {1}{U}{U} · Enchantment — Aura
 *
 * Enchant artifact creature
 * You control enchanted artifact creature.
 *
 * Mirrodin's artifact-only Control Magic — three mana instead of Confiscate's five, paid for by
 * the narrower enchant restriction.
 *
 * The enchant restriction is checked continuously, not just on resolution (CR 704.5m): if the
 * stolen permanent stops being an artifact creature — an Unnatural Selection-style type change,
 * or an artifact creature animated only temporarily — Domineer is put into its owner's graveyard
 * as a state-based action and control snaps back. That's why the restriction is the aura target
 * filter rather than a one-shot condition.
 *
 * There is no `Targets.ArtifactCreature` facade; this is the first card here to need one, so the
 * filter is inlined rather than promoted.
 */
val Domineer = card("Domineer") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact creature\nYou control enchanted artifact creature."

    auraTarget = TargetCreature(filter = TargetFilter(GameObjectFilter.ArtifactCreature))

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Jon Foster"
        flavorText = "Since they haven't seen their original master for millennia, golems are eager to take orders from anyone."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0010b89-afc4-4dee-bda3-2f34e552cba5.jpg?1783944556"
    }
}
