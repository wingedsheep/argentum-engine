package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.CostZone

/**
 * Necrotic Fumes — Strixhaven: School of Mages #78 (canonical printing)
 * {1}{B}{B} · Sorcery — Lesson
 *
 * As an additional cost to cast this spell, exile a creature you control.
 * Exile target creature or planeswalker.
 *
 * The additional cost is Soul Exchange's — [Costs.additional.ExileCards] over a creature you
 * control on the battlefield — and the spell is Feed the Serpent's plain exile.
 */
val NecroticFumes = card("Necrotic Fumes") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery — Lesson"
    oracleText = "As an additional cost to cast this spell, exile a creature you control.\n" +
        "Exile target creature or planeswalker."

    additionalCost(
        Costs.additional.ExileCards(
            count = 1,
            filter = GameObjectFilter.Creature.youControl(),
            fromZone = CostZone.BATTLEFIELD,
        )
    )

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Exile(victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "David Rapoza"
        flavorText = "When live specimens aren't readily available, Witherbloom mages make creative use of ragged spirits clinging to long-dead bodies."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e1b68a4-fb8d-4b59-b049-73505296f775.jpg?1783927365"
    }
}
