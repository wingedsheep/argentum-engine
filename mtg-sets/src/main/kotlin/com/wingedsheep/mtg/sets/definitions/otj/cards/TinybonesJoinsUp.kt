package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Tinybones Joins Up
 * {B}
 * Legendary Enchantment
 *
 * When Tinybones Joins Up enters, any number of target players each discard a card.
 * Whenever a legendary creature you control enters, any number of target players each
 * mill a card and lose 1 life.
 *
 * Part of the OTJ "Joins Up" cycle of Legendary Enchantments. Both abilities use
 * "any number of target players" — a [TargetPlayer] with `unlimited = true` — and iterate
 * the chosen players with [ForEachTargetEffect], applying a per-player effect via
 * `Player.ContextPlayer(0)`. The discarding/milling player is the iterated target, so the
 * discard's selection prompt goes to that player (CR: each affected player chooses their own
 * card simultaneously).
 */
val TinybonesJoinsUp = card("Tinybones Joins Up") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment"
    oracleText = "When Tinybones Joins Up enters, any number of target players each discard a card.\n" +
        "Whenever a legendary creature you control enters, any number of target players each mill a card and lose 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("any number of target players", TargetPlayer(unlimited = true))
        effect = ForEachTargetEffect(
            listOf(
                Effects.Discard(1, EffectTarget.PlayerRef(Player.ContextPlayer(0)))
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.legendary().youControl(),
            binding = TriggerBinding.ANY
        )
        target("any number of target players", TargetPlayer(unlimited = true))
        effect = ForEachTargetEffect(
            listOf(
                Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.ContextPlayer(0))),
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.ContextPlayer(0)))
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Wylie Beckert"
        flavorText = "It was going to be the greatest heist of his unlife."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5724a15f-0ba0-421a-9cd4-a2b701e6141f.jpg?1712355685"
    }
}
