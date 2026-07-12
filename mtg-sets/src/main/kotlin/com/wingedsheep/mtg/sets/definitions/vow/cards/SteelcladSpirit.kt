package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Steelclad Spirit
 * {1}{U}
 * Creature — Spirit
 * 3/3
 *
 * Defender
 * Whenever an enchantment you control enters, this creature can attack this turn as though it
 * didn't have defender.
 *
 * Models the enchantment-enters toggle like Stalked Researcher: a triggered ability filtered to
 * enchantments you control that grants [Effects.CanAttackDespiteDefenderThisTurn].
 */
val SteelcladSpirit = card("Steelclad Spirit") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "Defender\n" +
        "Whenever an enchantment you control enters, this creature can attack this turn as though " +
        "it didn't have defender."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.CanAttackDespiteDefenderThisTurn()
        description = "Whenever an enchantment you control enters, this creature can attack this " +
            "turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Artur Treffner"
        flavorText = "Some geists imitate the lives they left behind. Others follow the dreams they " +
            "never realized."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55fb1426-5a6f-48dd-938b-c64b1a28ee59.jpg?1782703135"
    }
}
