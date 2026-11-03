package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wesley Crusher, Starfleet Cadet
 * {2}{U}{U}
 * Legendary Creature — Human Officer
 * 2/2
 *
 * Flash
 * When Wesley Crusher enters, choose two —
 * • Counter target noncreature spell unless its controller pays {2}.
 * • Return target creature with mana value 2 or less to its owner's hand.
 * • Target creature can't be blocked this turn.
 * • Target player draws two cards, then discards two cards.
 *
 * A "choose two" enters trigger ([ModalEffect.chooseTwo], as Craftwork Crusher); each targeting
 * mode declares its own requirement, so modes and targets are chosen as the trigger goes on the
 * stack. The counter mode is Ojutai's Command's shape with Stubborn Denial's noncreature filter.
 */
val WesleyCrusherStarfleetCadet = card("Wesley Crusher, Starfleet Cadet") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Officer"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When Wesley Crusher enters, choose two —\n" +
        "• Counter target noncreature spell unless its controller pays {2}.\n" +
        "• Return target creature with mana value 2 or less to its owner's hand.\n" +
        "• Target creature can't be blocked this turn.\n" +
        "• Target player draws two cards, then discards two cards."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseTwo(
            mode("Counter target noncreature spell unless its controller pays {2}.") {
                target(TargetFilter.NoncreatureSpellOnStack)
                effect = Effects.CounterUnlessPays("{2}")
            },
            mode("Return target creature with mana value 2 or less to its owner's hand.") {
                val creature = target(TargetFilter.Creature.manaValueAtMost(2))
                effect = Effects.ReturnToHand(creature)
            },
            mode("Target creature can't be blocked this turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
            },
            mode("Target player draws two cards, then discards two cards.") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(2, player) then Patterns.Hand.discardCards(2, player)
            },
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Nia Kovalevski"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/956eeb71-fd5f-4a26-aff6-77127c0ad4e7.jpg?1793274247"
    }
}
