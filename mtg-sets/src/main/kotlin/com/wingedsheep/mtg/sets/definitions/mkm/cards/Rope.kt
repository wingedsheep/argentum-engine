package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Rope — Murders at Karlov Manor #173
 * {G} · Artifact — Clue Equipment
 *
 * Equipped creature gets +1/+2, has reach, and can't be blocked by more than one creature.
 * {2}, Sacrifice this Equipment: Draw a card.
 * Equip {3}
 *
 * One of the set's "murder weapon" Equipment — an Equipment that is also a Clue, so it carries the
 * standard Clue sacrifice-to-draw *and* the Clue subtype, which the set's "sacrifice a Clue"
 * payoffs read straight off the type line (Scryfall's ruling: "If an effect refers to a Clue, it
 * means any Clue artifact, not just a Clue artifact token").
 *
 * The blocking restriction is granted as the [AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE]
 * keyword rather than as a [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan] static scoped
 * to [Filters.EquippedCreature]. That matters: `BlockPhaseManager.validateMaxBlockersRequirements`
 * reads the printed `CantBeBlockedByMoreThan` form off the *attacker's own* card definition and
 * only when its filter scope is `Self`, so a copy of it living on the Equipment would never be
 * consulted. The flag form goes through the layer system onto the equipped creature and is picked
 * up by the same validator's projected-keyword branch — the identical path Glorfindel, Dauntless
 * Rescuer uses for its durational grant — and the client already has a display name for it.
 *
 * Rope stacks with menace rather than overriding it: "at least two" and "at most one" are both
 * enforced, so an equipped creature with menace simply can't be blocked at all (Scryfall ruling).
 */
val Rope = card("Rope") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Clue Equipment"
    oracleText = "Equipped creature gets +1/+2, has reach, and can't be blocked by more than one " +
        "creature.\n" +
        "{2}, Sacrifice this Equipment: Draw a card.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(+1, +2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE.name,
            Filters.EquippedCreature
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Matt Forsyth"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6881946c-5036-4d9f-926f-932c9a592aff.jpg?1783912861"

        ruling(
            "2024-02-02",
            "If the equipped creature also has menace, it can't be blocked at all."
        )
    }
}
