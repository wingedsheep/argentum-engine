package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Furious Assault
 * {2}{R}
 * Enchantment
 *
 * Whenever you cast a creature spell, this enchantment deals 1 damage to target player or
 * planeswalker.
 *
 * [Triggers.YouCastCreature] is the cast trigger (an ANY-bound `SpellCastEvent` filtered to
 * creature spells and to your own casts), so the ability goes on the stack *above* the creature
 * spell and resolves first. The damage target is the errata'd modern wording —
 * [Targets.PlayerOrPlaneswalker], not a bare player.
 */
val FuriousAssault = card("Furious Assault") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a creature spell, this enchantment deals 1 damage to target player or planeswalker."

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        val t = target("target", Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Greg Staples"
        flavorText = "\"Does it burn, Saprazzan? Do you wish you had never raised arms against us?\"\n" +
            "—Kyren overseer"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27a07fae-0f34-45e7-b22d-97eea9031022.jpg"
    }
}
