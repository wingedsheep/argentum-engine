package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hypnotic Specter
 * {1}{B}{B}
 * Creature — Specter
 * 2/2
 * Flying
 * Whenever this creature deals damage to an opponent, that player discards a card at random.
 */
val HypnoticSpecter = card("Hypnotic Specter") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    oracleText = "Flying\nWhenever this creature deals damage to an opponent, that player discards a card at random."
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = TriggerSpec(
            event = DealsDamageEvent(
                damageType = DamageType.Any,
                recipient = RecipientFilter.Opponent
            ),
            binding = TriggerBinding.SELF
        )
        effect = Patterns.Hand.discardRandom(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Douglas Shuler"
        flavorText = "\"...There was no trace/ Of aught on that illumined face...\"\n—Samuel Coleridge, \"Phantom\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b43b900f-2d9b-442b-9699-058483604ec9.jpg?1783948694"
        ruling("2008-08-01", "The ability triggers even if the Specter's damage is being redirected to an opponent. It does not trigger if damage that would have been dealt to the opponent is redirected to a nonopponent player or a creature, or if the damage is prevented.")
    }
}
