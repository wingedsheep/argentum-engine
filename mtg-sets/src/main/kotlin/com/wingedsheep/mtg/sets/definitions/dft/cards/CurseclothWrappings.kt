package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cursecloth Wrappings
 * {2}{B}{B}
 * Artifact
 *
 * Zombies you control get +1/+1.
 * {T}: Target creature card in your graveyard gains embalm until end of turn. The embalm cost is
 * equal to its mana cost. (Exile that card and pay its embalm cost: Create a token that's a copy
 * of it, except it's a white Zombie in addition to its other types and has no mana cost. Embalm
 * only as a sorcery.)
 *
 * The lord is an ordinary [ModifyStats] over Zombies you control — Cursecloth Wrappings is an
 * artifact, not a creature, so it never pumps itself and needs no `excludeSelf`.
 *
 * The tap ability is the runtime grant of the **Embalm** keyword (CR 702.128), modelled exactly like
 * Songcrafter Mage's harmonize grant and Archmage's Newt's flashback grant: [Effects.GrantEmbalm]
 * with a null cost means "the embalm cost is equal to its mana cost", read off the targeted card
 * when the ability resolves. Embalm differs from those two in *kind* — it is an ordinary
 * graveyard-activated ability rather than an alternative way to cast — so the grant rides the plain
 * granted-activated-ability channel and the engine's zone-activated-ability enumerator surfaces it
 * on the card in the graveyard. The granted ability is the same object
 * [com.wingedsheep.sdk.dsl.embalmAbility] builds for a printed-embalm card.
 *
 * Per the printed reminder text and CR 702.128a the token is white *instead of* its other colors, a
 * Zombie *in addition to* its other types, and has no mana cost (so mana value 0) — the three
 * exceptions the embalm ability's copy effect carries.
 */
val CurseclothWrappings = card("Cursecloth Wrappings") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "Zombies you control get +1/+1.\n" +
        "{T}: Target creature card in your graveyard gains embalm until end of turn. The embalm " +
        "cost is equal to its mana cost. (Exile that card and pay its embalm cost: Create a token " +
        "that's a copy of it, except it's a white Zombie in addition to its other types and has " +
        "no mana cost. Embalm only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl())
        )
    }

    activatedAbility {
        cost = Costs.Tap
        val card = target(
            "target creature card in your graveyard",
            TargetObject(filter = TargetFilter.CreatureInGraveyard.ownedByYou())
        )
        effect = Effects.GrantEmbalm(card)
        description = "{T}: Target creature card in your graveyard gains embalm until end of " +
            "turn. The embalm cost is equal to its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5803b32-4a81-46c2-9b10-3198a709611d.jpg?1783907896"

        ruling("2025-02-07", "Except for the listed exceptions, the token copies exactly what was " +
            "printed on the original card and nothing else. It doesn't copy any information about " +
            "the object the card was before it was put into your graveyard.")
        ruling("2025-02-07", "The token is a Zombie in addition to its other types and is white " +
            "instead of its other colors. It has no mana cost, and thus its mana value is 0. " +
            "These are copiable values of the token that other effects may copy.")
        ruling("2025-02-07", "If the card copied by the token had any \"when [this permanent] " +
            "enters\" abilities, then the token also has those abilities and they'll trigger when " +
            "it enters. Similarly, any \"as [this permanent] enters\" or \"[this permanent] " +
            "enters with\" abilities that the token has copied will also work.")
        ruling("2025-02-07", "Once you've activated an embalm ability, the card is immediately " +
            "exiled. Opponents can't try to stop the ability by exiling the card.")
    }
}
