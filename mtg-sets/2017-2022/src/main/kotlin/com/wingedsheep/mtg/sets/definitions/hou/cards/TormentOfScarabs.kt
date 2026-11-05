package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Torment of Scarabs
 * {3}{B}
 * Enchantment — Aura Curse
 *
 * Enchant player
 * At the beginning of enchanted player's upkeep, that player loses 3 life unless they sacrifice a
 * nonland permanent of their choice or discard a card.
 *
 * The enchanted player is both the one asked and the one who loses the life, so both name
 * [Player.EnchantedPlayer] rather than the Curse's controller.
 */
val TormentOfScarabs = card("Torment of Scarabs") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "At the beginning of enchanted player's upkeep, that player loses 3 life unless they sacrifice " +
        "a nonland permanent of their choice or discard a card."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Choice(
                listOf(
                    Costs.pay.Sacrifice(filter = GameObjectFilter.NonlandPermanent),
                    Costs.pay.Discard()
                )
            ),
            suffer = Effects.LoseLife(3, target = EffectTarget.PlayerRef(Player.EnchantedPlayer)),
            player = EffectTarget.PlayerRef(Player.EnchantedPlayer),
            consequenceDescription = "lose 3 life"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Bastien L. Deharme"
        flavorText = "Like a carpet unrolling for their god, the scarabs poured out from the necropolis."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01651f67-cc25-4240-9698-58b7d906a160.jpg?1783936035"
    }
}
