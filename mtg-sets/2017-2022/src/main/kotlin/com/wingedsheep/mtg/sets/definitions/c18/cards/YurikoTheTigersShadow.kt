package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.commanderNinjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yuriko, the Tiger's Shadow — Commander 2018 #52
 * {1}{U}{B} · Legendary Creature — Human Ninja · 1/3
 *
 * Commander ninjutsu {U}{B}
 * Whenever a Ninja you control deals combat damage to a player, reveal the top card of your
 * library and put that card into your hand. Each opponent loses life equal to that card's mana
 * value.
 *
 * Commander ninjutsu (CR 702.49d) is ninjutsu that also works from the command zone. The trigger
 * is Dark Confidant's reveal-to-hand pipeline aimed at each opponent.
 */
val YurikoTheTigersShadow = card("Yuriko, the Tiger's Shadow") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Ninja"
    power = 1
    toughness = 3
    oracleText = "Commander ninjutsu {U}{B} ({U}{B}, Return an unblocked attacker you control to hand: " +
        "Put this card onto the battlefield from your hand or the command zone tapped and attacking.)\n" +
        "Whenever a Ninja you control deals combat damage to a player, reveal the top card of your library " +
        "and put that card into your hand. Each opponent loses life equal to that card's mana value."

    commanderNinjutsu("{U}{B}")

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.NINJA).youControl())
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
            toHand(revealed, revealed = true)
            run(Effects.LoseLife(
                DynamicAmounts.manaValueOf(revealed),
                EffectTarget.PlayerRef(Player.EachOpponent)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd81ae6-e628-447a-a36b-597e63ede295.jpg?1783934325"
    }
}
