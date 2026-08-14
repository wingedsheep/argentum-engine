package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Wrench — Murders at Karlov Manor #37
 * {W} · Artifact — Clue Equipment
 *
 * Equipped creature gets +1/+1 and has vigilance and "{3}, {T}: Tap target creature."
 * {2}, Sacrifice this Equipment: Draw a card.
 * Equip {2}
 *
 * One of the set's "murder weapon" Equipment — an Equipment that is also a Clue, so it carries the
 * standard Clue sacrifice-to-draw *and* the Clue subtype, which the set's "sacrifice a Clue"
 * payoffs read straight off the type line (Scryfall's ruling: "If an effect refers to a Clue, it
 * means any Clue artifact, not just a Clue artifact token").
 *
 * The tapper is a [GrantActivatedAbility] scoped to [Filters.EquippedCreature] rather than an
 * ability on the Equipment: the granted ability lives on the *creature*, so its {T} taps the
 * creature (not the Wrench), it needs the creature to be untapped, and it's subject to the
 * creature's own summoning sickness. Vigilance in the same grant is what makes the pair work —
 * an equipped attacker stays untapped and can still tap for the ability afterwards.
 */
val Wrench = card("Wrench") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Clue Equipment"
    oracleText = "Equipped creature gets +1/+1 and has vigilance and \"{3}, {T}: Tap target " +
        "creature.\"\n" +
        "{2}, Sacrifice this Equipment: Draw a card.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap),
                effect = Effects.Tap(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(TargetCreature()),
                descriptionOverride = "{3}, {T}: Tap target creature."
            ),
            filter = Filters.EquippedCreature
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c36cc39b-8f3b-4297-b118-86fa624308b4.jpg?1783912917"
    }
}
