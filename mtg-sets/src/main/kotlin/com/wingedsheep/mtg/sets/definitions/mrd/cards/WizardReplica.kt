package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wizard Replica — Mirrodin #275
 * {3} · Artifact Creature — Wizard · 1/3
 *
 * Flying
 * {U}, Sacrifice this creature: Counter target spell unless its controller pays {2}.
 *
 * The blue member of the Replica cycle ([ElfReplica], [GoblinReplica]): a colourless artifact body
 * whose one-shot ability is gated behind a coloured pip plus sacrificing itself.
 *
 * The sacrifice is part of the *cost*, so it happens on activation — the Replica is already in the
 * graveyard while the counter sits on the stack, and countering the ability's own target does not
 * bring it back.
 */
val WizardReplica = card("Wizard Replica") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Wizard"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "{U}, Sacrifice this creature: Counter target spell unless its controller pays {2}."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        target = Targets.Spell
        effect = Effects.CounterUnlessPays("{2}")
        description = "{U}, Sacrifice this creature: Counter target spell unless its controller pays {2}."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "275"
        artist = "Carl Critchlow"
        flavorText = "It responds with unnatural precision."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5ab68b3-864e-4fe3-a5c5-faa33b45da0f.jpg?1783944496"
    }
}
