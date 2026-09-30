package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Annihilating Glare
 * {B}
 * Sorcery
 * As an additional cost to cast this spell, pay {4} or sacrifice an artifact or creature.
 * Destroy target creature or planeswalker.
 *
 * The binary additional-cost fork is the Deadly Precision / Lash of the Balrog shape: a non-modal
 * [ModalEffect.chooseOne] whose two modes share the target and effect but carry different costs.
 * `countsAsModalSpell = false` — there is no "Choose one —" on the card.
 */
val AnnihilatingGlare = card("Annihilating Glare") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, pay {4} or sacrifice an artifact or creature.\n" +
        "Destroy target creature or planeswalker."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Pay {4} — destroy target creature or planeswalker") {
                val victim = target(Targets.CreatureOrPlaneswalker)
                additionalManaCost = "{4}"
                effect = Effects.Destroy(victim)
            },
            mode("Sacrifice an artifact or creature — destroy target creature or planeswalker") {
                val victim = target(Targets.CreatureOrPlaneswalker)
                additionalCosts = listOf(
                    Costs.additional.SacrificePermanent(
                        filter = GameObjectFilter.Artifact.or(GameObjectFilter.Creature)
                    )
                )
                effect = Effects.Destroy(victim)
            },
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Konstantin Porubov"
        flavorText = "By gaze, sting, and claw, Vraska will see the will of Phyrexia done."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be5d0b95-ec12-4e8e-99a0-7aca457f9107.jpg?1783918052"
    }
}
