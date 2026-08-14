package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Snowslope Hunter — The Hobbit #112
 * {2}{R} · Creature — Goblin Ranger · Uncommon
 * 2/3
 *
 * Sacrifice another creature or artifact: Exile the top card of your library. You may play it until
 * the end of your next turn. Activate only during your turn and only once each turn.
 *
 * Modeling notes:
 *  - The cost is [Costs.SacrificeAnother], so the Hunter can never eat itself.
 *  - The impulse window is [MayPlayExpiry.UntilEndOfNextTurn] — turn-keyed, so the permission
 *    survives the end of the turn it was granted on and is revoked at the cleanup of your next turn.
 *    It also survives the Hunter leaving the battlefield, which is the printed behaviour.
 *  - Both printed restrictions are activation restrictions rather than a timing rule: the ability is
 *    still instant-speed *within* your turn (you may sacrifice in response during your own combat),
 *    it just can't be activated on someone else's turn, and only once each turn.
 */
val SnowslopeHunter = card("Snowslope Hunter") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Ranger"
    power = 2
    toughness = 3
    oracleText = "Sacrifice another creature or artifact: Exile the top card of your library. " +
        "You may play it until the end of your next turn. Activate only during your turn and " +
        "only once each turn."

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilEndOfNextTurn)
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.OncePerTurn
        )
        description = "Sacrifice another creature or artifact: Exile the top card of your library. " +
            "You may play it until the end of your next turn. Activate only during your turn and " +
            "only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Adrián Rodríguez Pérez"
        flavorText = "Goblins did not usually venture very far from their mountains except to go " +
            "on raids or hunt for food."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47666099-ffb2-4d07-a801-70524dba0837.jpg?1785497147"
    }
}
