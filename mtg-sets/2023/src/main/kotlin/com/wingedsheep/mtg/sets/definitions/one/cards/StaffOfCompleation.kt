package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Staff of Compleation — Phyrexia: All Will Be One #242
 * {3} · Artifact
 *
 * {T}, Pay 1 life: Destroy target permanent you own.
 * {T}, Pay 2 life: Add one mana of any color.
 * {T}, Pay 3 life: Proliferate.
 * {T}, Pay 4 life: Draw a card.
 * {5}: Untap this artifact.
 *
 * "Permanent you **own**" — [GameObjectFilter.ownedByYou], so a permanent an opponent stole
 * from you is a legal target while one you merely control is not. The mana ability is Myr
 * Convert's shape (CR 605.1a: no target, no stack).
 */
val StaffOfCompleation = card("Staff of Compleation") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Pay 1 life: Destroy target permanent you own.\n" +
        "{T}, Pay 2 life: Add one mana of any color.\n" +
        "{T}, Pay 3 life: Proliferate.\n" +
        "{T}, Pay 4 life: Draw a card.\n" +
        "{5}: Untap this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        val permanent = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou()))
        effect = Effects.Destroy(permanent)
        description = "{T}, Pay 1 life: Destroy target permanent you own."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(2))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(3))
        effect = Effects.Proliferate()
        description = "{T}, Pay 3 life: Proliferate."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(4))
        effect = Effects.DrawCards(1)
        description = "{T}, Pay 4 life: Draw a card."
    }

    activatedAbility {
        cost = Costs.Mana("{5}")
        effect = Effects.Untap(EffectTarget.Self)
        description = "{5}: Untap this artifact."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2934af5-aa4f-4c88-adbd-dcd847ef43a2.jpg?1783917986"
    }
}
