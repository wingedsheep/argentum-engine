package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.DamageType

/**
 * Grisly Sigil
 * {B}
 * Sorcery
 * Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell and you may choose a new target for the copy.)
 * Choose target creature or planeswalker. If it was dealt noncombat damage this turn, Grisly Sigil deals 3 damage to it and you gain 3 life. Otherwise, Grisly Sigil deals 1 damage to it and you gain 1 life.
 *
 * The check reads the target's per-turn damage marker narrowed to noncombat damage, so a creature
 * hurt only in combat takes the 1-damage branch. With Casualty, the copy resolves first: its damage
 * is noncombat, so the original then takes the 3-damage branch on the same target.
 */
val GrislySigil = card("Grisly Sigil") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell and you may choose a new target for the copy.)\nChoose target creature or planeswalker. If it was dealt noncombat damage this turn, Grisly Sigil deals 3 damage to it and you gain 3 life. Otherwise, Grisly Sigil deals 1 damage to it and you gain 1 life."

    keywordAbility(KeywordAbility.casualty(1))

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                Filters.Permanent.wasDealtDamageThisTurn(DamageType.NonCombat), t
            ),
            then = Effects.DealDamage(3, t) then Effects.GainLife(3),
            otherwise = Effects.DealDamage(1, t) then Effects.GainLife(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6322e12d-b932-4ca1-a51d-e2a928140cc7.jpg?1783923130"
    }
}
