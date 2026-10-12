package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Mechanized Warfare
 * {1}{R}{R}
 * Enchantment
 * If a red or artifact source you control would deal damage to an opponent or a permanent an
 * opponent controls, it deals that much damage plus 1 instead.
 *
 * A plain additive [ModifyDamageAmount] (Aether Revolt / Fated Firepower's shape, every damage
 * type) whose source filter is "red or artifact, you control". The extra damage is dealt by the
 * original source; the source filter is read against projected state, and a spell on the stack
 * or a source that has left the battlefield falls back to its own characteristics.
 */
val MechanizedWarfare = card("Mechanized Warfare") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "If a red or artifact source you control would deal damage to an opponent or a " +
        "permanent an opponent controls, it deals that much damage plus 1 instead."

    replacementEffect(
        ModifyDamageAmount(
            modifier = 1,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.withColor(Color.RED)
                    .or(GameObjectFilter.Artifact)
                    .youControl(),
                recipient = Recipient.OpponentOrPermanentTheyControl,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Robin Olausson"
        flavorText = "First the automatons reinforced depleted squads. Then they replaced them entirely."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba2d026f-bdca-4376-9d70-c81c707598e8.jpg?1783920064"
        ruling("2022-10-14", "The additional 1 damage is dealt by the original source of damage. The additional damage isn't dealt by Mechanized Warfare.")
        ruling("2022-10-14", "If damage dealt by a red or artifact source you control is being divided or assigned among multiple opponents and/or permanents opponents' control, divide the original amount before adding 1. For example, if you cast a red spell that deals 4 damage divided among any number of targets and you choose to have it deal 2 damage to a creature and 2 damage to that creature's controller, that spell will deal 3 damage to each of them instead.")
    }
}
