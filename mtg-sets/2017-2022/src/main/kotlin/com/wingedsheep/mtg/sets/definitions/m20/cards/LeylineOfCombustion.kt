package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leyline of Combustion — Core Set 2020 #148
 * {2}{R}{R} · Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * Whenever you and/or at least one permanent you control becomes the target of a spell or ability
 * an opponent controls, this enchantment deals 2 damage to that player.
 *
 * "You and/or at least one permanent you control" is the batch template: one firing per targeting
 * spell or ability, however many of its targets are you or your permanents (`batch = true`; the
 * 2019-07-12 ruling). "That player" is the targeting object's controller —
 * [Player.ControllerOfCause], read with last-known information if the spell was countered first.
 */
val LeylineOfCombustion = card("Leyline of Combustion") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "Whenever you and/or at least one permanent you control becomes the target of a spell or ability " +
        "an opponent controls, this enchantment deals 2 damage to that player."

    mayBeginGameOnBattlefield()

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl())
            .becomesTarget(byOpponent = true, players = Player.You, batch = true)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.ControllerOfCause))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a93c8e2-fb27-43af-83a7-2bd4d40e0eff.jpg?1783932974"
        ruling("2019-07-12", "Leyline of Combustion's triggered ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-07-12", "If a spell targets you and/or a permanent you control multiple times, or if a spell targets you and one or more permanents you control, Leyline of Combustion's triggered ability triggers once.")
    }
}
