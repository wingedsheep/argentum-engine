package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Fall of Gil-galad
 * {1}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Scry 2.
 * II — Put two +1/+1 counters on target creature you control.
 * III — Until end of turn, target creature you control gains "When this creature dies, draw two cards."
 *       Then that creature fights up to one other target creature.
 */
val FallOfGilGalad = card("Fall of Gil-galad") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Scry 2.\n" +
        "II — Put two +1/+1 counters on target creature you control.\n" +
        "III — Until end of turn, target creature you control gains \"When this creature dies, draw two cards.\" Then that creature fights up to one other target creature."

    sagaChapter(1) {
        effect = LibraryPatterns.scry(2)
    }

    sagaChapter(2) {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, creature)
    }

    sagaChapter(3) {
        val mine = target("creature you control", Targets.CreatureYouControl)
        val other = target("up to one other target creature", TargetCreature(optional = true))
        effect = GrantTriggeredAbilityEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.Dies.event,
                binding = Triggers.Dies.binding,
                effect = DrawCardsEffect(2)
            ),
            target = mine
        )
            .then(Effects.Fight(mine, other))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Craig Elliott"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbaab2c0-ea18-4b2f-b75b-506cbbea97e1.jpg?1688569166"
    }
}
