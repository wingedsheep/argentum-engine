package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Search Party Captain — Innistrad: Midnight Hunt #32
 * {3}{W} · Creature — Human Soldier · 2/2
 *
 * This spell costs {1} less to cast for each creature you attacked with this turn.
 * When this creature enters, draw a card.
 *
 * The reduction is [CostReductionSource.CreaturesThatAttackedThisTurn] with `yoursOnly = true`:
 * turn history of the caster's own attack declarations, so an attacker that died in combat still
 * counts and an opponent's attackers never do. It only eats generic mana, so the floor is {W}.
 */
val SearchPartyCaptain = card("Search Party Captain") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "This spell costs {1} less to cast for each creature you attacked with this turn.\n" +
        "When this creature enters, draw a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CreaturesThatAttackedThisTurn(yoursOnly = true)
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Mike Bierek"
        flavorText = "\"Werewolves leave messy trails. Easy to follow. Hard to forget.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb9006c1-2e6f-4bca-a1c4-3cf2a8b6e964.jpg?1783925652"
    }
}
