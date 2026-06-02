package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Saproling Infestation
 * {1}{G}
 * Enchantment
 * Whenever a player kicks a spell, you create a 1/1 green Saproling creature token.
 *
 * "A player" includes every player, so the trigger watches `SpellCastEvent(player = Player.Each)`
 * gated by the [SpellCastPredicate.WasKicked] cast-time fact — the same predicate the kicker
 * cost-payment marks on the stack. The token is created by the enchantment's controller
 * ("you"), so the default controller (null) is correct.
 */
val SaprolingInfestation = card("Saproling Infestation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a player kicks a spell, you create a 1/1 green Saproling creature token."

    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(player = Player.Each, requires = setOf(SpellCastPredicate.WasKicked)),
            TriggerBinding.ANY
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "/images/tokens/inv-saproling.jpeg"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Heather Hudson"
        flavorText = "\"My army took centuries to gather,\" remarked Urza. " +
            "\"Yavimaya seems to conjure hers out of thin air.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8642e530-914c-4149-944a-c4966ee27299.jpg?1562922159"
    }
}
