package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thunder of Unity — Tarkir: Dragonstorm #231
 * {R}{W}{B} · Enchantment — Saga · Rare
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — You draw two cards and you lose 2 life.
 * II, III — Whenever a creature you control enters this turn, each opponent loses 1 life and
 *           you gain 1 life.
 *
 * Chapters II and III each install a turn-bounded, filter-scoped event delayed triggered ability:
 * `CreateDelayedTriggerEffect(trigger = entersBattlefield(Creature.youControl(), binding = ANY))`
 * with `fireOnce = false` (it fires for *every* creature you control that enters, not just the
 * first) and `expiry = EndOfTurn`. Because the trigger has no single watched entity, the delayed
 * trigger detector scopes it by the GameObjectFilter (`creature you control`) rather than a
 * watched entity — see [TriggerDetector.matchesEventForWatchedEntity]. The two chapters create two
 * independent instances of the ability, matching the printed card.
 */
val ThunderOfUnity = card("Thunder of Unity") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — You draw two cards and you lose 2 life.\n" +
        "II, III — Whenever a creature you control enters this turn, each opponent loses 1 life and you gain 1 life."

    // I — You draw two cards and you lose 2 life.
    sagaChapter(1) {
        effect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    // II, III — Whenever a creature you control enters this turn, each opponent loses 1 life and
    // you gain 1 life.
    sagaChapter(2) {
        effect = thunderOfUnityDelayedTrigger()
    }
    sagaChapter(3) {
        effect = thunderOfUnityDelayedTrigger()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "Clint Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c953b36-f5e4-4258-91cb-f07e799321f7.jpg?1744578016"
    }
}

/**
 * "Whenever a creature you control enters this turn, each opponent loses 1 life and you gain 1
 * life." A fresh instance is installed by each of chapters II and III.
 */
private fun thunderOfUnityDelayedTrigger(): CreateDelayedTriggerEffect =
    CreateDelayedTriggerEffect(
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY
        ),
        fireOnce = false,
        expiry = DelayedTriggerExpiry.EndOfTurn,
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1)
    )
