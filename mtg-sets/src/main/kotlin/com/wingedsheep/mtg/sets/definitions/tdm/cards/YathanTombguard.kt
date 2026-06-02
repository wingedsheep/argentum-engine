package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yathan Tombguard
 * {2}{B}
 * Creature — Human Warrior
 * 2/3
 *
 * Menace
 * Whenever a creature you control with a counter on it deals combat damage to a player,
 * you draw a card and you lose 1 life.
 *
 * The trigger is a generic outgoing combat-damage trigger ([DealsDamageEvent] /
 * [TriggerBinding.ANY]) whose [sourceFilter] is "a creature you control with a counter
 * on it" via [GameObjectFilter.Creature.youControl().withAnyCounter]. "You" is Yathan's
 * controller, so the draw + life loss both target [Player.You] (the ability controller),
 * not the triggering player.
 */
val YathanTombguard = card("Yathan Tombguard") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "Menace\n" +
        "Whenever a creature you control with a counter on it deals combat damage to a player, " +
        "you draw a card and you lose 1 life."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = TriggerSpec(
            DealsDamageEvent(
                damageType = DamageType.Combat,
                recipient = RecipientFilter.AnyPlayer,
                sourceFilter = GameObjectFilter.Creature.youControl().withAnyCounter(),
            ),
            TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Xavier Ribeiro"
        flavorText = "\"Our khan would grant you clemency. She is naive. " +
            "And unfortunately for you, she is also not here.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e65d487-705a-4c3b-9bb6-69351e5dae81.jpg?1743204362"
    }
}
