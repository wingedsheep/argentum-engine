package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ichneumon Druid — Legends #191
 * {1}{G}{G} · Creature — Human Druid · 1/1 · Uncommon
 *
 * Whenever an opponent casts an instant spell other than the first instant spell that player
 * casts each turn, this creature deals 4 damage to that player.
 *
 * The trigger is "an opponent casts an instant spell" with a `triggerRestriction` reading the
 * *caster's* own instant history ([Player.TriggeringPlayer]) — the 2004 ruling: the count is per
 * player. The triggering spell is already recorded when the event is examined, so "at least 2" is
 * exactly "not the first". The history counts casts, not resolutions, so an instant that player
 * cast before the Druid arrived (or one that was countered) still uses up the free one.
 */
val IchneumonDruid = card("Ichneumon Druid") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 1
    toughness = 1
    oracleText = "Whenever an opponent casts an instant spell other than the first instant spell " +
        "that player casts each turn, this creature deals 4 damage to that player."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Instant)
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastThisTurn(player = Player.TriggeringPlayer, filter = GameObjectFilter.Instant),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.DealDamage(4, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent casts an instant spell other than the first instant spell " +
            "that player casts each turn, this creature deals 4 damage to that player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2313bb-6f9b-49d6-b069-9f3b77b6e107.jpg?1783948048"
        ruling("2004-10-04", "It tracks the instant count for each player separately.")
    }
}
